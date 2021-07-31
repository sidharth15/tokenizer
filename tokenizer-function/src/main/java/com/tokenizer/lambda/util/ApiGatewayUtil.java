package com.tokenizer.lambda.util;

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tokenizer.lambda.model.queues.Queue;
import com.tokenizer.lambda.model.response.ResponseModel;

import java.util.Map;

public class ApiGatewayUtil {
    public static final String POST = "POST";
    public static final String PUT = "PUT";
    public static final String GET = "GET";
    public static final String DELETE = "DELETE";
    public static final String PAGINATION_TOKEN = "pagination_token";
    private static final String EMPTY_STRING = "";
    private static final String RESPONSE_ERROR_JSON = "{\"statusCode\":\"502\", \"message\": \"An unexpected error occurred\"}";

    /**
     * Method to parse username from an API Gateway event.
     * @param event The API Gateway event.
     * @return the username if it exists else return null.
     */
    public static String parseUsername(APIGatewayProxyRequestEvent event) {
        String username = null;

        if (event != null
                && event.getRequestContext() != null
                && event.getRequestContext().getAuthorizer() != null) {
            Map<String, String> authorizerClaimsMap = (Map<String, String>) event.getRequestContext().getAuthorizer().get("claims");
            username = authorizerClaimsMap.get("username");
        }

        return username;
    }

    /**
     * Method to parse the value for a query string parameter
     * from the API Gateway request event.
     * @param event The input API Gateway request event.
     * @param parameterName The query string parameter name.
     * @return Value of the query string parameter as a String if it exists. Else returns null.
     * */
    public static String parseQueryStringParameter(APIGatewayProxyRequestEvent event, String parameterName) {
        String result = null;

        if (isQueryStringParametersValid(event)) {
            String paramValue = event.getQueryStringParameters().get(parameterName);
            result = EMPTY_STRING.equals(paramValue) ? null: paramValue;
        }

        return result;
    }

    /**
     * Method to parse the queue max size parameter as an Integer
     * from the API Gateway request event.
     * @param event The input API Gaeway request event.
     * @return Integer object with maxSize value if specified in the request.
     * Else returns default queue size.
     * */
    public static Integer parseQueueMaxSize(APIGatewayProxyRequestEvent event) {
        Integer result = null;

        if (isQueryStringParametersValid(event)) {
            String paramValue = event.getQueryStringParameters().get(Queue.COL_MAX_SIZE);

            try {
                result = Integer.parseInt(paramValue);
            } catch (NumberFormatException e) {
                result = Queue.DEFAULT_MAX_SIZE;
            }
        }

        return result;
    }

    /**
     * Method to parse queue disabled status from input
     * API Gateway request event.
     * @param event The input API Gateway request event.
     * @return boolean value of disabled status if specified in the request.
     * Else returns false.
     * */
    public static boolean parseQueueDisabledStatus(APIGatewayProxyRequestEvent event) {
        boolean result = false;

        if (isQueryStringParametersValid(event)) {
            String paramValue = event.getQueryStringParameters().get(Queue.COL_DISABLED);
            result = Boolean.parseBoolean(paramValue);
        }

        return result;
    }

    public static String getResponseJsonString(ObjectMapper mapper, ResponseModel response) {
        String result = null;
        try {
            mapper.writeValueAsString(response);
        } catch (JsonProcessingException e) {
            result = RESPONSE_ERROR_JSON;
        }

        return result;
    }

    private static boolean isQueryStringParametersValid(APIGatewayProxyRequestEvent event) {
        return event != null && event.getQueryStringParameters() != null;
    }
}
