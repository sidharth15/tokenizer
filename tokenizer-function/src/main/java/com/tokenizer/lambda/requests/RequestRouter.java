package com.tokenizer.lambda.requests;

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.tokenizer.lambda.requests.handlers.GenericResponseHandler;
import com.tokenizer.lambda.requests.handlers.QueueEventHandler;

public class RequestRouter {
    private static final String QUEUES = "/queues/queue";

    private QueueEventHandler queueRequestHandler;
    private GenericResponseHandler genericResponseHandler;

    public RequestRouter(QueueEventHandler queueRequestHandler, GenericResponseHandler genericResponseHandler) {
        this.queueRequestHandler = queueRequestHandler;
        this.genericResponseHandler = genericResponseHandler;
    }

    public EventHandler getHandler(APIGatewayProxyRequestEvent input) {
        EventHandler result;
        String resourcePath = input.getPath();

        switch (resourcePath) {
            case QUEUES:
                result = queueRequestHandler;
                break;
            default:
                result = genericResponseHandler;
                break;
        }

        return result;
    }
}
