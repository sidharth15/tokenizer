package com.tokenizer.lambda.requests.handlers;

import com.amazonaws.services.dynamodbv2.model.ConditionalCheckFailedException;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tokenizer.lambda.model.response.ResponseModel;
import com.tokenizer.lambda.model.users.User;
import com.tokenizer.lambda.requests.EventHandler;
import com.tokenizer.lambda.service.QueueService;
import com.tokenizer.lambda.service.UserService;
import com.tokenizer.lambda.util.ApiGatewayUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class UserEventHandler implements EventHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(UserEventHandler.class);

    private UserService userService;
    private QueueService queueService;
    private ObjectMapper mapper;

    public UserEventHandler(UserService userService, QueueService queueService, ObjectMapper mapper) {
        this.userService = userService;
        this.queueService = queueService;
        this.mapper = mapper;
    }

    @Override
    public String handleEvent(APIGatewayProxyRequestEvent input) {
        ResponseModel<List<User>> response = null;
        String userId = ApiGatewayUtil.parseUsername(input);

        if (userId != null) {
            String httpMethod = input.getHttpMethod();
            String owner = ApiGatewayUtil.parseQueryStringParameter(input, User.COL_QUEUE_OWNER);
            String queueId = ApiGatewayUtil.parseQueryStringParameter(input, User.COL_QUEUE_ID);

            switch (httpMethod) {
                case ApiGatewayUtil.PUT:
                    response = queueId != null ?
                            processItemFromQueue(userId, queueId):
                            buildFailureResponse(400, "queue_id parameter is missing");
                    break;

                case ApiGatewayUtil.GET:
                    response = describeUser(userId, owner == null ? null : Boolean.parseBoolean(owner));
                    break;

                case ApiGatewayUtil.DELETE:
                     response = queueId != null ?
                             unsubscribeFromQueue(userId, queueId):
                             buildFailureResponse(400, "queue_id parameter is missing");
                    break;

                default:
                    response = buildFailureResponse(400, "Invalid method requested.");
                    break;
            }
        }

        return ApiGatewayUtil.getResponseJsonString(mapper, response);
    }

    /**
     * Method to process the item at the HEAD of the queue.
     * This method does 2 things:
     * 1. Increment the last_processed_token of the queue by 1.
     * 2. Mark the user at the last_processed_token position of the queue as DONE.
     *
     * @param userId The ID of the user invoking the process-item request.
     *               This should equal to the ID of the owner of the queue
     *               for the operation to succeed.
     *
     * @param queueId The ID of the queue in which the item is being processed.
     *
     * @return Response to the user with the last_processed_token
     * */
    private ResponseModel<List<User>> processItemFromQueue(String userId, String queueId) {
        ResponseModel<List<User>> response;
        try{
            if (userService.isQueueOwner(userId, queueId)) {
                String lastProcessedToken = queueService.processItemFromQueue(queueId);

                boolean unsubscribed = userService.markSubscriberAsProcessed(queueId, lastProcessedToken);
                if (!unsubscribed) {
                    LOGGER.warn("Could not unsubscribe user at position {} of queue {}", lastProcessedToken, queueId);
                }

                response = buildSuccessResponse(null, lastProcessedToken);

            } else {
                response = buildFailureResponse(401, "Queues does not exist or " +
                        "you are not authorized to update queue " + queueId);
            }

        } catch (ConditionalCheckFailedException e) {

            LOGGER.warn("No more items to process.");
            response = buildFailureResponse(400, "No more items to process in queue " + queueId);

        } catch (Exception e) {

            LOGGER.error("Exception occurred while updating last_processed_token: ", e);
            response = buildFailureResponse(502, ResponseModel.FAILURE_MESSAGE);
        }

        return response;
    }

    private ResponseModel<List<User>> describeUser(String userId, Boolean ownedByUser) {
        ResponseModel<List<User>> response;
        try {
            // list of queues user is subscribed to or owns
            List<User> userQueues = userService.describeUser(userId, ownedByUser);
            response = buildSuccessResponse(userQueues, ResponseModel.SUCCESS_MESSAGE);
        } catch (Exception e) {
            LOGGER.error("Error occurred while describing user {}", userId, e);
            response = buildFailureResponse(502, "An unexpected error occurred.");
        }

        return response;
    }

    private ResponseModel<List<User>> unsubscribeFromQueue(String userId, String queueId) {
        ResponseModel<List<User>> response;

        try {
            userService.unsubscribeUserFromQueue(userId, queueId);
            response = buildSuccessResponse(null, ResponseModel.SUCCESS_MESSAGE);

        } catch (ConditionalCheckFailedException e) {
            LOGGER.warn("Cannot unsubscribe user {} from queue {}. User is the owner of the queue.", userId, queueId, e);
            response = buildFailureResponse(400, "Cannot un-subscribe owner from their queue.");
        } catch (Exception e) {
            LOGGER.error("Unexpected error occurred: ", e);
            response = buildFailureResponse(502, "An unexpected error occurred.");
        }

        return response;
    }

    private ResponseModel<List<User>> buildSuccessResponse(List<User> users, String message) {
        return new ResponseModel<>(200, message, users, null);
    }

    private ResponseModel<List<User>> buildFailureResponse(int statusCode, String message) {
        return new ResponseModel<>(statusCode, message, null, null);
    }
}
