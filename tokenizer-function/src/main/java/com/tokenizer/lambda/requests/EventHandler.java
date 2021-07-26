package com.tokenizer.lambda.requests;

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;

public interface EventHandler {
    public String handleEvent(APIGatewayProxyRequestEvent input);
}
