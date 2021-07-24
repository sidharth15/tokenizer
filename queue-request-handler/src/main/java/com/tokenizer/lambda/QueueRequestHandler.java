package com.tokenizer.lambda;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tokenizer.lambda.model.SampleResponse;
import com.tokenizer.lambda.util.ApiGatewayUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

public class QueueRequestHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {
    private static final Logger LOGGER = LoggerFactory.getLogger(QueueRequestHandler.class);
    private static final String CONTENTTYPE = "Content-Type";
    private static final String APPLICATION_JSON = "application/json";

    private ObjectMapper mapper;

    private void init() {
        LOGGER.info("Initializing Lambda...");
        this.mapper = new ObjectMapper();
        LOGGER.info("Initialization complete.");
    }

    private boolean isInitialized() {
        return mapper != null;
    }

    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent input, Context context) {
        if (!isInitialized()) {
            init();
        }

        APIGatewayProxyResponseEvent response = new APIGatewayProxyResponseEvent();
        response.setStatusCode(200);

        Map<String, String> headers = new HashMap<>();
        headers.put(CONTENTTYPE, APPLICATION_JSON);
        response.setHeaders(headers);

        SampleResponse responseBody = new SampleResponse(context.getAwsRequestId(), context.getMemoryLimitInMB(), true);
        try {
            LOGGER.debug("Received event: {}", mapper.writeValueAsString(input));
            LOGGER.debug("username is {}", ApiGatewayUtil.parseUsername(input));
            response.setBody(mapper.writeValueAsString(responseBody));
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }

        LOGGER.info("Response: {}", response);

        return response;
    }
}
