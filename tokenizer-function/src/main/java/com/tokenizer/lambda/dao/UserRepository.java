package com.tokenizer.lambda.dao;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBDeleteExpression;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBQueryExpression;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.ConditionalCheckFailedException;
import com.tokenizer.lambda.model.users.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class UserRepository {
    private static final String WARN_MESSAGE = "Entity is null or no user_id/queue_id provided.";
    private static final Logger LOGGER = LoggerFactory.getLogger(UserRepository.class);
    private DynamoDBMapper mapper;

    public UserRepository(DynamoDBMapper mapper) {
        this.mapper = mapper;
    }

    public void save(User user) {
        if (isValid(user)) {
            LOGGER.info("Saving user {}", user);
            mapper.save(user);
        } else {
            LOGGER.warn("Save - {}", WARN_MESSAGE);
        }
    }

    public User load(User lookup) {
        User result = null;

        if (isValid(lookup)) {
            LOGGER.info("Loading user {}", lookup);
            result = mapper.load(lookup);
        } else {
            LOGGER.warn("Load - {}", WARN_MESSAGE);
        }

        return result;
    }

    public User load(String userId, String queueId) {
        return load(new User(userId, queueId));
    }

    /**
     * Method to query all entries for a particular user.
     * @param userId The userId to query.
     * @param ownedByUser Flag to indicate if query should be filter based on
     *                    whether user owns the queue or not.
     * @return List of entries for the user. If no items are found, returns null.
     * */
    public List<User> load(String userId, Boolean ownedByUser) throws ConditionalCheckFailedException {
        List<User> result = null;

        if (userId != null) {
            User lookup = new User(userId);
            DynamoDBQueryExpression<User> queryExpression = new DynamoDBQueryExpression<User>()
                    .withHashKeyValues(lookup);

            queryExpression = ownedByUser == null ?
                    queryExpression :
                    queryExpression.withFilterExpression("#owner = :owner")
                    .withExpressionAttributeNames(new HashMap<String, String>() {{
                        put("#owner", User.COL_QUEUE_OWNER);
                    }})
                    .withExpressionAttributeValues(new HashMap<String, AttributeValue>() {{
                        put(":owner", new AttributeValue().withBOOL(ownedByUser));
                    }});

            result = mapper.query(User.class, queryExpression);
        } else {
            LOGGER.warn("Query - {}", WARN_MESSAGE);
        }

        return result;
    }

    /**
     * Method to lookup users that are subscribed to a queue.
     * @param queueId The ID of the queue to check.
     * @return List of users subscribed to the queue. Null if queue has no subscribers.
     * */
    public List<User> query(String queueId) {
        List<User> result = null;

        if (queueId != null) {
            User lookup = new User(null, queueId);
            DynamoDBQueryExpression<User> queryExpression = new DynamoDBQueryExpression<User>()
                    .withIndexName(User.QUEUE_GSI)
                    .withConsistentRead(false)
                    .withHashKeyValues(lookup);
            result = mapper.query(User.class, queryExpression);
        }

        return result;
    }

    /**
     * Method to delete link between a user and queue.
     * Can be used for un-subscribing a user from queue and
     * also to delete a user's ownership of a queue - depending
     * on the value passed for the unsubscribeOnly parameter.
     *
     * @param userToDelete The user to un-subscribing from a queue.
     *
     * @param unsubscribeOnly Flag indicating whether to delete the link
     *                        if user is the owner of the queue.
     *                        If set to True, delete will be successful only if the user
     *                        does not own the queue. In other words, only un-subscribe
     *                        action will take place.
     *                        If set to False, delete will be successful only if the user
     *                        owns the queue.
     * */
    public void delete(User userToDelete, boolean unsubscribeOnly) {
        if (isValid(userToDelete)) {
            Map<String, String> ean = new HashMap<String, String>() {{
                put("#owner", User.COL_QUEUE_OWNER);
            }};
            Map<String, AttributeValue> eav = new HashMap<String, AttributeValue>() {{
                put(":owner", new AttributeValue().withBOOL(!unsubscribeOnly));
            }};
            DynamoDBDeleteExpression deleteExpression = new DynamoDBDeleteExpression()
                    .withConditionExpression("#owner = :owner")
                    .withExpressionAttributeNames(ean)
                    .withExpressionAttributeValues(eav);

            mapper.delete(userToDelete, deleteExpression);
        } else {
            LOGGER.warn("Delete - {}", WARN_MESSAGE);
        }
    }

    private boolean isValid(User user) {
        return user != null
                && user.getUserId() != null
                && user.getQueueId() != null;
    }
}
