package com.tokenizer.lambda.util;

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;

import java.util.Map;

public class ApiGatewayUtil {
    public static final String POST = "POST";
    public static final String PUT = "PUT";
    public static final String GET = "GET";
    public static final String DELETE = "DELETE";
    private static final String EMPTY_STRING = "";

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

        if (event != null && event.getQueryStringParameters() != null) {
            String paramValue = event.getQueryStringParameters().get(parameterName);
            result = EMPTY_STRING.equals(paramValue) ? null: paramValue;
        }

        return result;
    }
}
