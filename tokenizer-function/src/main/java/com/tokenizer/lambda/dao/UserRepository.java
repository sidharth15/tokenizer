package com.tokenizer.lambda.dao;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBQueryExpression;
import com.tokenizer.lambda.model.users.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

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
     * Method to query all entries for a particulr user.
     * @param userId The userId to query.
     * @return List of entries for the user. If no items are found, returns null.
     * */
    public List<User> load(String userId) {
        List<User> result = null;

        if (userId != null) {
            User lookup = new User(userId);
            DynamoDBQueryExpression<User> queryExpression = new DynamoDBQueryExpression<User>()
                    .withHashKeyValues(lookup);
            result = mapper.query(User.class, queryExpression);
        } else {
            LOGGER.warn("Query - {}", WARN_MESSAGE);
        }

        return result;
    }

    public void delete(User userToDelete) {
        if (isValid(userToDelete)) {
            mapper.delete(userToDelete);
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
