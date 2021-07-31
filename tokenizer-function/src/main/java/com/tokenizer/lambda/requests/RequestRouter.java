package com.tokenizer.lambda.requests;

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.tokenizer.lambda.requests.handlers.GenericResponseHandler;
import com.tokenizer.lambda.requests.handlers.ListQueuesEventHandler;
import com.tokenizer.lambda.requests.handlers.QueueEventHandler;
import com.tokenizer.lambda.requests.handlers.UserEventHandler;

public class RequestRouter {
    private static final String QUEUE = "/queues/queue";
    private static final String QUEUES = "/queues";
    private static final String USER = "/user";

    private QueueEventHandler queueRequestHandler;
    private ListQueuesEventHandler listQueuesEventHandler;
    private UserEventHandler userEventHandler;
    private GenericResponseHandler genericResponseHandler;

    public RequestRouter(QueueEventHandler queueRequestHandler,
                         ListQueuesEventHandler listQueuesEventHandler,
                         UserEventHandler userEventHandler,
                         GenericResponseHandler genericResponseHandler) {
        this.queueRequestHandler = queueRequestHandler;
        this.listQueuesEventHandler = listQueuesEventHandler;
        this.userEventHandler = userEventHandler;
        this.genericResponseHandler = genericResponseHandler;
    }

    public EventHandler getHandler(APIGatewayProxyRequestEvent input) {
        EventHandler result;
        String resourcePath = input.getResource();

        switch (resourcePath) {
            case QUEUES:
                result = listQueuesEventHandler;
                break;
            case QUEUE:
                result = queueRequestHandler;
                break;
            default:
                result = genericResponseHandler;
                break;
        }

        return result;
    }
}
