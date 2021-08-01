package com.tokenizer.lambda.service;

import com.amazonaws.services.dynamodbv2.model.ConditionalCheckFailedException;
import com.tokenizer.lambda.dao.UserRepository;
import com.tokenizer.lambda.model.users.User;
import com.tokenizer.lambda.model.users.UserState;
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
     *
     * @param userId The user's user_id.
     *
     * @param ownedByUser Flag to filter queues owned by the user or not.
     *                    If null, all queues linked to the user are returned.
     *
     * @return List of User objects, each pointing to a separate Queue.
     * An empty List if no items were found for the user.
     * */
    public List<User> describeUser(String userId, Boolean ownedByUser) {
        List<User> queuesForUser = repository.load(userId, ownedByUser);

        return queuesForUser != null ? queuesForUser: new ArrayList<>();
    }

    /**
     * Method to create a new queue that will be owned by the user.
     * This does not imply a subscription to the queue since the
     * owner cannot subscribe to their own queue.
     * Hence, The created link will not have a token number.
     *
     * @param userId The user that is creating the new queue.
     *
     * @param queueId An ID that is unique across Lambda invocations.
     *                The caller needs to ensure the uniqueness -
     *                best way is to parse the aws request ID from
     *                the input API Gateway event.
     *
     * @return The ID of the newly created queue, null if failed to create queue.
     * */
    public String createNewQueueForUser(String userId, String queueId) {

        try {
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
     * */
    public void unsubscribeUserFromQueue(String userId, String queueId) throws ConditionalCheckFailedException{
        deleteUserQueueLink(userId, queueId, true);
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
    public void deleteUserQueueLink(String userId, String queueId, boolean unsubscribeOnly) throws ConditionalCheckFailedException {
        repository.delete(new User(userId, queueId), unsubscribeOnly);
    }

    /**
     * Method to mark a subscriber of a queue with the
     * mentioned token number as 'DONE'.
     * @param queueId The queue to which the user is subscribed to.
     * @param tokenNumber The token number of the subscribed user.
     * */
    public boolean markSubscriberAsProcessed(String queueId, String tokenNumber) {
        boolean success = false;
        User subscriber = getSubscriberAtPosition(queueId, tokenNumber);

        if (subscriber != null) {
            subscriber.setState(UserState.DONE);

            repository.save(subscriber);
            success = true;
        }

        return success;
    }

    /**
     * Method to lookup a subscriber at a given position
     * of a queue.
     * @param queueId The queue to which the user is subscribed.
     * @param tokenNumber The position of the user in the queue.
     * @return User object if there is a user is found with the mentioned
     * token number, Else returns null.
     * */
    private User getSubscriberAtPosition(String queueId, String tokenNumber) {
        User subscriberAtPosition = null;

        try {
            int token = Integer.parseInt(tokenNumber);
            subscriberAtPosition = repository.lookupByTokenNumber(queueId, token);
        } catch (NumberFormatException e) {
            LOGGER.error("Error parsing token number: {}", tokenNumber);
        }

        return subscriberAtPosition;
    }
}
