package com.tokenizer.lambda.util;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;

public class DynamoUtil {
    public static final AmazonDynamoDB DYNAMO_CLIENT;

    static {
        DYNAMO_CLIENT = AmazonDynamoDBClientBuilder.standard().build();
    }

}
