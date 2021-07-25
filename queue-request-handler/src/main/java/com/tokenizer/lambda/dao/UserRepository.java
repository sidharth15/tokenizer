package com.tokenizer.lambda.dao;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.tokenizer.lambda.model.users.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UserRepository {
    private static final Logger LOGGER = LoggerFactory.getLogger(UserRepository.class);
    private DynamoDBMapper mapper;

    public UserRepository(DynamoDBMapper mapper) {
        this.mapper = mapper;
    }

    public boolean save(User user) {
        boolean result = false;

        if (user != null
                && user.getUserId() != null) {
            LOGGER.info("Saving user {}", user);
            mapper.save(user);
            result = true;
        } else {
            LOGGER.warn("'User' entity is null or does not have required key attributes.");
        }

        return result;
    }

    public User load(User lookup) {
        User result = null;

        if (lookup != null
                && lookup.getUserId() != null
                && lookup.getQueueId() != null) {
            LOGGER.info("Loading user {}", lookup);
            result = mapper.load(lookup);
        } else {
            LOGGER.warn("Lookup user entity was null or no user_id/queue_id was provided.");
        }

        return result;
    }

    public User load(String userId, String queueId) {
        return load(new User(userId, queueId));
    }
}
