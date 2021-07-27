package com.tokenizer.lambda.requests.handlers;

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tokenizer.lambda.model.queues.Queue;
import com.tokenizer.lambda.model.users.User;
import com.tokenizer.lambda.requests.EventHandler;
import com.tokenizer.lambda.service.QueueService;
import com.tokenizer.lambda.service.UserService;
import com.tokenizer.lambda.util.ApiGatewayUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

public class QueueEventHandler implements EventHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(QueueEventHandler.class);

    private UserService userService;
    private QueueService queueService;
    private ObjectMapper mapper;

    public QueueEventHandler(UserService userService, QueueService queueService, ObjectMapper mapper) {
        this.userService = userService;
        this.queueService = queueService;
        this.mapper = mapper;
    }

    @Override
    public String handleEvent(APIGatewayProxyRequestEvent input) {
        String result = null;
        String userId = ApiGatewayUtil.parseUsername(input);

        if (userId != null) {
            String httpMethod = input.getHttpMethod();

            switch (httpMethod) {
                case ApiGatewayUtil.POST :
                    String maxSize = ApiGatewayUtil.parseQueryStringParameter(input, "queue_max_size");
                    String disabled = ApiGatewayUtil.parseQueryStringParameter(input, "disabled");

                    result = createNewQueue(
                            userId,
                            maxSize != null ? Integer.parseInt(maxSize): Queue.DEFAULT_MAX_SIZE,
                            Boolean.parseBoolean(disabled)
                    );
                    break;

                case ApiGatewayUtil.DELETE:
                    String queueToDelete = ApiGatewayUtil.parseQueryStringParameter(input, "queue_id");
                    result = queueToDelete != null ? deleteQueue(queueToDelete, userId) : "false";
                    break;

                case ApiGatewayUtil.GET:
                    String queueToDescribe = ApiGatewayUtil.parseQueryStringParameter(input, "queue_id");
                    result = describeQueue(queueToDescribe);
                    break;

                default:
                    result = "Generic Response - OK";
                    break;
            }
        } else {
            LOGGER.error("No user_id information found. User is not logged in.");
        }

        return result;
    }

    /**
     * Method to handle creation of a new queue.
     * @param userId The ID of the user creating the queue.
     * @param maxSize The max size of the queue.
     * @param disabled The status of the queue to be initialized with.
     * @return The ID of the newly created queue.
     * */
    private String createNewQueue(String userId, Integer maxSize, boolean disabled) {
        String queueId = userService.createNewQueueForUser(userId);
        queueService.initNewQueue(queueId, maxSize, disabled);

        return queueId;
    }

    /**
     * Method to delete a queue and unsubscribe all subscribers of the queue.
     * @param queueId The ID of the queue to delete.
     * @param userId The ID of the user invoking the delete request.
     * @return True if queue was successfully deleted. Else returns false.
     * */
    private String deleteQueue(String queueId, String userId) {
        boolean deleted = false;
        boolean isOwner = userService.isQueueOwner(userId, queueId);

        if (isOwner) {
            LOGGER.info("Deleting queue {}", queueId);

            // unsubscribe all subscribers from the queue
            List<User> subscribers = userService.getSubscribersToQueue(queueId);
            Optional.ofNullable(subscribers)
                    .map(Collection::stream)
                    .orElseGet(Stream::empty)
                    .forEach(subscriber -> {
                        LOGGER.info("Unsubscribing user {} from queue {}", subscriber.getUserId(), queueId);
                        userService.unsubscribeUserFromQueue(subscriber.getUserId(), queueId);
                    });

            // delete the queue status item
            queueService.deleteQueue(queueId);

            // delete user record of the owner linking to the queue
            userService.deleteUserQueueLink(userId, queueId);
            deleted = true;
        } else {
            LOGGER.warn("User {} cannot delete queue {}. Not the owner", userId, queueId);
        }

        return Boolean.toString(deleted);
    }

    public String describeQueue(String queueId) {
        String result = null;
        Queue queueDetails = queueService.describeQueue(queueId);

        try {
            result = mapper.writeValueAsString(queueDetails);
        } catch (JsonProcessingException e) {
            LOGGER.error("Error occurred while converting queue details: ", e);
        }

        return result;
    }
}
