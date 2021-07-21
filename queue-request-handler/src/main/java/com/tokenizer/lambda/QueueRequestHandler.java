package com.tokenizer.lambda;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tokenizer.lambda.model.SampleResponse;

import java.util.HashMap;
import java.util.Map;

public class QueueRequestHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {
    public static final String CONTENTTYPE = "Content-Type";
    public static final String APPLICATION_JSON = "application/json";

    ObjectMapper mapper = new ObjectMapper();

    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent input, Context context) {
        APIGatewayProxyResponseEvent response = new APIGatewayProxyResponseEvent();
        response.setStatusCode(200);

        Map<String, String> headers = new HashMap<>();
        headers.put(CONTENTTYPE, APPLICATION_JSON);
        response.setHeaders(headers);

        SampleResponse responseBody = new SampleResponse(context.getAwsRequestId(), context.getMemoryLimitInMB(), true);
        try {
            response.setBody(mapper.writeValueAsString(responseBody));
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }

        return response;
    }
}
