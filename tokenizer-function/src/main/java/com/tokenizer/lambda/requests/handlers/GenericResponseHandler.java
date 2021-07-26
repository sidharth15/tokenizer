package com.tokenizer.lambda.requests.handlers;

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.tokenizer.lambda.requests.EventHandler;

public class GenericResponseHandler implements EventHandler {
    private static final String GENERIC_RESPONSE = "{\"message\":\"Invalid resource requested\"}";

    @Override
    public String handleEvent(APIGatewayProxyRequestEvent input) {
        return GENERIC_RESPONSE;
    }
}
