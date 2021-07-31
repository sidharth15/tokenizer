package com.tokenizer.lambda.requests.handlers;

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tokenizer.lambda.model.queues.Queue;
import com.tokenizer.lambda.model.response.ResponseModel;
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
        ResponseModel<Queue> response;
        String userId = ApiGatewayUtil.parseUsername(input);

        if (userId != null) {
            String httpMethod = input.getHttpMethod();
            String queueId = ApiGatewayUtil.parseQueryStringParameter(input, Queue.COL_QUEUE_ID);
            String queueName = ApiGatewayUtil.parseQueryStringParameter(input, Queue.COL_QUEUE_NAME);
            String queueSize = ApiGatewayUtil.parseQueryStringParameter(input, Queue.COL_MAX_SIZE);
            String queueDisabled = ApiGatewayUtil.parseQueryStringParameter(input, Queue.COL_DISABLED);

            switch (httpMethod) {
                case ApiGatewayUtil.POST :

                    String newQueueId = createNewQueue(
                            userId,
                            queueName != null ? queueName: userId + "_queue_" + input.getRequestContext().getRequestId(),
                            queueSize,
                            queueDisabled);

                    if (newQueueId != null) {
                        response = buildSuccessMessage(new Queue(newQueueId), ResponseModel.SUCCESS_MESSAGE);
                    } else {
                        response = buildFailureMessage(502, "Failed to create queue");
                    }
                    break;

                case ApiGatewayUtil.DELETE:
                    response = deleteQueue(queueId, userId) ?
                            buildSuccessMessage(null, "Successfully deleted queue " + queueId) :
                            buildFailureMessage(502, "Failed to delete queue " + queueId);
                    break;

                case ApiGatewayUtil.GET:
                    Queue queueDetails = describeQueue(queueId);
                    response = queueDetails != null ?
                            buildSuccessMessage(queueDetails, ResponseModel.SUCCESS_MESSAGE):
                            buildFailureMessage(404, "Queue with id " + queueId + " not found.");
                    break;

                case ApiGatewayUtil.PUT:
                    response = updateQueue(queueId, queueName, queueSize, queueDisabled) ?
                            buildSuccessMessage(null, ResponseModel.SUCCESS_MESSAGE) :
                            buildFailureMessage(502, "Failed to update queue.");
                    break;

                default:
                    response = new ResponseModel<>(400, "Invalid method request.", null, null);
                    break;
            }
        } else {
            response = new ResponseModel<>(
                    401,
                    "No user_id information found. User is not logged in.",
                    null, null);
        }

        return ApiGatewayUtil.getResponseJsonString(mapper, response);
    }

    /**
     * Method to handle creation of a new queue.
     * @param userId The ID of the user creating the queue.
     * @param maxSize The max size of the queue.
     * @param disabled The status of the queue to be initialized with.
     * @return The ID of the newly created queue.
     * */
    private String createNewQueue(String userId, String queueName, String maxSize, String disabled) {
        Integer size;
        boolean disabledStatus = Boolean.parseBoolean(disabled);
        try {
            size = Integer.parseInt(maxSize);
        } catch (NumberFormatException e) {
            size = Queue.DEFAULT_MAX_SIZE;
        }

        String queueId = userService.createNewQueueForUser(userId);
        queueService.initNewQueue(queueId, queueName, size, disabledStatus);

        return queueId;
    }

    /**
     * Method to delete a queue and unsubscribe all subscribers of the queue.
     * @param queueId The ID of the queue to delete.
     * @param userId The ID of the user invoking the delete request.
     * @return True if queue was successfully deleted. Else returns false.
     * */
    private boolean deleteQueue(String queueId, String userId) {
        boolean deleted = false;
        if (queueId == null) return deleted;

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

        return deleted;
    }

    /**
     * Method to describe queue configurations.
     * @param queueId The ID of the queue.
     * @return Queue configurations if queue exists, else returns null.
     * */
    private Queue describeQueue(String queueId) {
        Queue queueDetails = null;

        if (queueId != null) {
            queueDetails = queueService.describeQueue(queueId);
        }

        return queueDetails;
    }

    private boolean updateQueue(String queueId, String queueName, String maxSize, String disabled) {
        boolean result = false;
        Integer size = null;
        Boolean disabledStatus = null;

        try {
            size = Integer.parseInt(maxSize);
        } catch (NumberFormatException e) {}

        // we need to explicitly check like this so we don't overwrite
        // the queue's current status when the user does not wish to.
        if (disabled != null) {
            if (disabled.equalsIgnoreCase(Boolean.TRUE.toString())) {
                disabledStatus = Boolean.TRUE;
            } else if (disabled.equalsIgnoreCase(Boolean.FALSE.toString())) {
                disabledStatus = Boolean.FALSE;
            }
        }

        try {
            queueService.updateQueue(queueId, queueName, size, disabledStatus);
            result = true;
        } catch (Exception e) {
            LOGGER.error("Exception occurred while updating queue {}", queueId, e);
        }

        return result;
    }

    private ResponseModel<Queue> buildSuccessMessage(Queue queue, String message) {
        return new ResponseModel<>(200, message, queue, null);
    }

    private ResponseModel<Queue> buildFailureMessage(int statusCode, String errorMessage) {
        return new ResponseModel<>(statusCode, errorMessage, null, null);
    }
}
