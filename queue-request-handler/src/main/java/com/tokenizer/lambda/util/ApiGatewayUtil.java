package com.tokenizer.lambda.util;

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;

import java.util.Map;

public class ApiGatewayUtil {
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
}
