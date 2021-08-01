package com.tokenizer.lambda;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.ConditionalCheckFailedException;
import com.amazonaws.services.dynamodbv2.model.PutItemRequest;
import com.amazonaws.services.dynamodbv2.model.ReturnValue;
import com.amazonaws.services.dynamodbv2.model.UpdateItemRequest;
import com.amazonaws.services.dynamodbv2.model.UpdateItemResult;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tokenizer.lambda.model.queues.Queue;
import com.tokenizer.lambda.model.users.User;
import com.tokenizer.lambda.model.users.UserState;
import com.tokenizer.lambda.util.ApiGatewayUtil;
import com.tokenizer.lambda.util.DynamoUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

public class SubscriberFunction implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {
    private static final Logger LOGGER = LoggerFactory.getLogger(SubscriberFunction.class);
    private static final String CONTENTTYPE = "Content-Type";
    private static final String APPLICATION_JSON = "application/json";

    private ObjectMapper objectMapper;
    private AmazonDynamoDB dynamoDbClient;
    private Map<String, String> ean;
    private Map<String, AttributeValue> eav;
    private Map<String, String> headers;

    private void init() {
        this.objectMapper = new ObjectMapper();
        this.dynamoDbClient = DynamoUtil.DYNAMO_CLIENT;

        this.ean = new HashMap<String, String>() {{
            put("#last_generated_token", Queue.COL_LAST_GEN_TOKEN);
            put("#max_size", Queue.COL_MAX_SIZE);
        }};

        this.eav = new HashMap<String, AttributeValue>() {{
            put(":one", new AttributeValue().withN("1"));
        }};

        this.headers = new HashMap<String, String>() {{
            put(CONTENTTYPE, APPLICATION_JSON);
        }};

    }

    private boolean isInitialized() {
        return objectMapper != null;
    }

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent input, Context context) {
        if (!isInitialized()) init();

        APIGatewayProxyResponseEvent response = new APIGatewayProxyResponseEvent();
        response.setStatusCode(200);
        response.setHeaders(headers);

        String responseBody;

        try {
            LOGGER.debug("Received event: {}", objectMapper.writeValueAsString(input));
            String userId = ApiGatewayUtil.parseUsername(input);
            String queueId = ApiGatewayUtil.parseQueryStringParameter(input, User.COL_QUEUE_ID);

            if (userId != null) {
                if (queueId != null) {
                    String tokenNum = incrementLastGeneratedToken(queueId);
                    createSubscriptionLink(userId, queueId, tokenNum);
                    responseBody = "{\"token_number\":\"" + tokenNum +" \"}";
                } else {
                    response.setStatusCode(400);
                    responseBody = "{\"errorMessage\":\"queue_id parameter is missing\"}";
                }
            } else {
                response.setStatusCode(401);
                responseBody = "{\"errorMessage\":\"Unauthenticated. Login before invoking the API.\"}";
            }

        } catch (JsonProcessingException e) {

            LOGGER.error("Error marshalling to JSON: ", e);
            response.setStatusCode(502);
            responseBody = "{\"errorMessage\":\"An unexpected error occurred. Please try later.\"}";

        } catch (ConditionalCheckFailedException e) {

            LOGGER.warn("Conditional check failed - max limit for the must have been reached.", e);
            response.setStatusCode(400);
            responseBody = "{\"errorMessage\":\"An error occurred - max size for queue reached\"}";

        } catch (Exception e) {

            response.setStatusCode(502);
            LOGGER.error("Fatal error occurred: ", e);
            responseBody = "{\"errorMessage\":\"Fatal error - contact support with this reference id"
                    + input.getRequestContext().getRequestId() + "\"}";

        }

        response.setBody(responseBody);

        LOGGER.info("Response: {}", response);

        return response;
    }

    private String incrementLastGeneratedToken(String queueId) throws ConditionalCheckFailedException {
        Map<String, AttributeValue> key = new HashMap<String, AttributeValue>() {{
            put(Queue.COL_QUEUE_ID, new AttributeValue(queueId));
        }};

        UpdateItemRequest updateItemRequest = new UpdateItemRequest()
                .withTableName(Queue.TABLE_NAME)
                .withKey(key)
                .withUpdateExpression("set #last_generated_token = #last_generated_token + :one")
                .withConditionExpression("#last_generated_token < #max_size")
                .withExpressionAttributeNames(ean)
                .withExpressionAttributeValues(eav)
                .withReturnValues(ReturnValue.UPDATED_NEW);

        UpdateItemResult updateItemResult = dynamoDbClient.updateItem(updateItemRequest);

        String tokenNumber = updateItemResult.getAttributes().get(Queue.COL_LAST_GEN_TOKEN).getN();
        LOGGER.info("Updated last_generated_token for queue {} to {}", queueId, tokenNumber);

        return tokenNumber;
    }

    private void createSubscriptionLink(String userId, String queueId, String tokenNumber)
            throws ConditionalCheckFailedException {
        Map<String, AttributeValue> attributeValues = new HashMap<>();
        attributeValues.put(User.COL_USER_ID, new AttributeValue(userId));
        attributeValues.put(User.COL_QUEUE_ID, new AttributeValue(queueId));
        attributeValues.put(User.COL_QUEUE_OWNER, new AttributeValue().withBOOL(false));
        attributeValues.put(User.COL_TOKEN_NUM, new AttributeValue().withN(tokenNumber));
        attributeValues.put(User.COL_USER_STATE, new AttributeValue(UserState.WAITING.name()));

        PutItemRequest putItemRequest = new PutItemRequest()
                .withTableName(User.TABLE_NAME)
                .withItem(attributeValues)
                .withConditionExpression("#token_number > :new_token_num");

        dynamoDbClient.putItem(putItemRequest);

        LOGGER.info("Created subscription link to queue {} for user {}", queueId, userId);
    }
}
