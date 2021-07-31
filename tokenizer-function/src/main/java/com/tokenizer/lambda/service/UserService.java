package com.tokenizer.lambda.service;

import com.tokenizer.lambda.dao.UserRepository;
import com.tokenizer.lambda.model.users.User;
import com.tokenizer.lambda.util.QueueUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class UserService {
    private static final Logger LOGGER = LoggerFactory.getLogger(UserService.class);
    private UserRepository repository;

    public UserService(UserRepository repository) {
        this.repository = repository;
    }

    /**
     * Method to fetch all records of the user.
     * The resulting list of User gives all the queues the user owns or is subscribed to.
     * @param userId The user's user_id.
     * @return List of User objects, each pointing to a separate Queue. Null if no items were found for the user.
     * */
    public List<User> describeUser(String userId) {
        List<User> queuesForUser = repository.load(userId);

        return queuesForUser != null ? queuesForUser: new ArrayList<>();
    }

    /**
     * Method to create a new queue that will be owned by the user.
     * This does not imply a subscription to the queue since the owner cannot subscribe to their own queue.
     * Hence, The created link will not have a token number.
     * @param userId The user that is creating the new queue.
     * @return The ID of the newly created queue, null if failed to create queue.
     * */
    public String createNewQueueForUser(String userId) {
        String queueId;

        try {
            queueId = QueueUtil.generateQueueId();
            User userQueueRef = new User(userId, queueId);
            userQueueRef.setOwner(true);
            repository.save(userQueueRef);
        } catch (Exception e) {
            LOGGER.error("Exception occurred while creating new queue for user {}: {}", userId, e);
            queueId = null;
        }

        return queueId;
    }

    /**
     * Method to un-subscribe a user from a queue, by deleting the entry linking the user and the queue.
     * @param userId The user_id of the user.
     * @param queueId The queue_id to unsubscribe from.
     * @return true if successfully unsubscribed else returns false.
     * */
    public boolean unsubscribeUserFromQueue(String userId, String queueId) {
        boolean deleted = false;

        try {
            deleteUserQueueLink(userId, queueId, true);
            deleted = true;

        } catch (Exception e) {
            LOGGER.error("Error occurred while un-subscribing user {} from queue {}: {}", userId, queueId, e);
        }

        return deleted;
    }

    /**
     * Method to check if a user is the owner of a queue.
     * @param userId The user's ID.
     * @param queueId The queue's ID.
     * @return true if the user is the queue's owner.
     * */
    public boolean isQueueOwner(String userId, String queueId) {
        User userQueueDetails = repository.load(userId, queueId);
        // if we don't find a record of the user owning the queue return false
        return userQueueDetails != null && userQueueDetails.isOwner();
    }

    /**
     * Method to list all the subscribers of a queue.
     * @param queueId The ID of the queue.
     * @return List of Users who are subscribed to the queue.
     * */
    public List<User> getSubscribersToQueue(String queueId) {
        return repository.query(queueId);
    }

    /**
     * Method to delete item linking a user to a particular queue.
     * @param userId The ID of the user.
     * @param queueId The ID of the queue.
     * @param unsubscribeOnly Flag to indicate if only want to un-subscribe
     *                        or delete the queue altogether.
     */
    public void deleteUserQueueLink(String userId, String queueId, boolean unsubscribeOnly) {
        repository.delete(new User(userId, queueId), unsubscribeOnly);
    }
}
