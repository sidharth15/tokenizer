package com.tokenizer.lambda;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tokenizer.lambda.dao.QueueRepository;
import com.tokenizer.lambda.dao.UserRepository;
import com.tokenizer.lambda.factory.RequestRouterFactory;
import com.tokenizer.lambda.requests.EventHandler;
import com.tokenizer.lambda.requests.RequestRouter;
import com.tokenizer.lambda.service.QueueService;
import com.tokenizer.lambda.service.UserService;
import com.tokenizer.lambda.util.DynamoUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

public class TokenizerFunction implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {
    private static final Logger LOGGER = LoggerFactory.getLogger(TokenizerFunction.class);
    private static final String CONTENTTYPE = "Content-Type";
    private static final String APPLICATION_JSON = "application/json";

    private ObjectMapper objectMapper;
    private DynamoDBMapper dynamoDBMapper;
    private UserRepository userRepository;
    private QueueRepository queueRepository;
    private UserService userService;
    private QueueService queueService;
    private RequestRouter router;

    private void init() {
        LOGGER.info("Initializing Lambda...");

        this.objectMapper = new ObjectMapper();
        this.dynamoDBMapper = new DynamoDBMapper(DynamoUtil.DYNAMO_CLIENT);
        this.userRepository = new UserRepository(dynamoDBMapper);
        this.queueRepository = new QueueRepository(dynamoDBMapper);
        this.userService = new UserService(userRepository);
        this.queueService = new QueueService(queueRepository);
        this.router = RequestRouterFactory.createRequestRouter(userService, queueService, objectMapper);

        LOGGER.info("Initialization complete.");
    }

    private boolean isInitialized() {
        return objectMapper != null
                && dynamoDBMapper != null
                && userRepository != null
                && queueRepository != null
                && userService != null
                && queueService != null
                && router != null;
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

        try {
            LOGGER.debug("Received event: {}", objectMapper.writeValueAsString(input));
            EventHandler eventHandler = router.getHandler(input);
            String responseBody = eventHandler.handleEvent(input);
            response.setBody(responseBody);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }

        LOGGER.info("Response: {}", response);

        return response;
    }
}
