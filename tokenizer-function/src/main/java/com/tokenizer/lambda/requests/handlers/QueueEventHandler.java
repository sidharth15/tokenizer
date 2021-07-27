package com.tokenizer.lambda.requests.handlers;

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
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

    public QueueEventHandler(UserService userService, QueueService queueService) {
        this.userService = userService;
        this.queueService = queueService;
    }

    @Override
    public String handleEvent(APIGatewayProxyRequestEvent input) {
        String result = null;
        String userId = ApiGatewayUtil.parseUsername(input);

        if (userId != null) {
            String httpMethod = input.getHttpMethod();

            switch (httpMethod) {
                case ApiGatewayUtil.POST :
                    result = createNewQueue(userId, 50);
                    break;
                case ApiGatewayUtil.DELETE:
                    String queueToDelete = input.getQueryStringParameters() != null ? input.getQueryStringParameters().get("queue_id"): null;
                    result = queueToDelete != null ? deleteQueue(queueToDelete, userId) : "false";
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

    private String createNewQueue(String userId, Integer maxSize) {
        String queueId = userService.createNewQueueForUser(userId);
        queueService.initNewQueue(queueId, maxSize);

        return queueId;
    }

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

}
