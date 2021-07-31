package com.tokenizer.lambda.requests.handlers;

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tokenizer.lambda.model.response.ResponseModel;
import com.tokenizer.lambda.model.users.User;
import com.tokenizer.lambda.requests.EventHandler;
import com.tokenizer.lambda.service.UserService;
import com.tokenizer.lambda.util.ApiGatewayUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.stream.Collectors;

public class UserEventHandler implements EventHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(UserEventHandler.class);

    private UserService userService;
    private ObjectMapper mapper;

    public UserEventHandler(UserService userService, ObjectMapper mapper) {
        this.userService = userService;
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
                case ApiGatewayUtil.GET:
                    try {
                        List<User> userQueues = describeUser(userId, Boolean.parseBoolean(owner));
                        response = buildSuccessResponse(userQueues, ResponseModel.SUCCESS_MESSAGE);
                    } catch (Exception e) {
                        LOGGER.error("Error occurred while describing user {}", userId, e);
                        response = buildFailureResponse(502, "An unexpected error occurred.");
                    }
                    break;

                case ApiGatewayUtil.DELETE:
                    response = unsubscribeFromQueue(userId, queueId) ?
                            buildSuccessResponse(null, ResponseModel.SUCCESS_MESSAGE) :
                            buildFailureResponse(502, queueId == null ? ResponseModel.FAILURE_MESSAGE : "queue_id parameter is missing");
                    break;

                default:
                    response = buildFailureResponse(400, "Invalid method requested.");
                    break;
            }
        }

        return ApiGatewayUtil.getResponseJsonString(mapper, response);
    }

    private List<User> describeUser(String userId, boolean ownedByUser) {
        // list of queues user is subscribed to or owns
        List<User> userQueues = userService.describeUser(userId);

        return ownedByUser ?
                userQueues.stream().filter(User::isOwner).collect(Collectors.toList()):
                userQueues;
    }

    private boolean unsubscribeFromQueue(String userId, String queueId) {
        boolean success = false;
        if (queueId == null) return false;
        try {
            success = userService.unsubscribeUserFromQueue(userId, queueId);
        } catch (Exception e) {
            LOGGER.error("Error occurred while un-subscribing user {} from queue {}", userId, queueId, e);
        }

        return success;
    }

    private ResponseModel<List<User>> buildSuccessResponse(List<User> users, String message) {
        return new ResponseModel<>(200, message, users, null);
    }

    private ResponseModel<List<User>> buildFailureResponse(int statusCode, String message) {
        return new ResponseModel<>(statusCode, message, null, null);
    }
}