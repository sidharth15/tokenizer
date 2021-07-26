package com.tokenizer.lambda.requests.handlers;

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.tokenizer.lambda.requests.EventHandler;
import com.tokenizer.lambda.service.QueueService;
import com.tokenizer.lambda.service.UserService;
import com.tokenizer.lambda.util.ApiGatewayUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
}
