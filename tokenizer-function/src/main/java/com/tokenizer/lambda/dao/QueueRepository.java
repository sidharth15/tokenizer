package com.tokenizer.lambda.dao;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapperConfig;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBSaveExpression;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBScanExpression;
import com.amazonaws.services.dynamodbv2.datamodeling.ScanResultPage;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.ConditionalCheckFailedException;
import com.amazonaws.services.dynamodbv2.model.ExpectedAttributeValue;
import com.amazonaws.services.dynamodbv2.model.ReturnValue;
import com.amazonaws.services.dynamodbv2.model.UpdateItemRequest;
import com.amazonaws.services.dynamodbv2.model.UpdateItemResult;
import com.tokenizer.lambda.model.queues.Queue;
import com.tokenizer.lambda.util.DynamoUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

public class QueueRepository {
    private static final String WARN_MESSAGE = "Entity is null or no queue_id provided.";
    private static final Logger LOGGER = LoggerFactory.getLogger(QueueRepository.class);
    private DynamoDBMapper mapper;

    public QueueRepository(DynamoDBMapper mapper) {
        this.mapper = mapper;
    }

    public void save(Queue queue) {
        if (isValid(queue)) {
            LOGGER.info("Saving Queue {}", queue);
            mapper.save(queue);
        } else {
            LOGGER.warn("Save - {}", WARN_MESSAGE);
        }
    }

    public Queue load(Queue lookup) {
        Queue result = null;

        if (isValid(lookup)) {
            LOGGER.info("Loading Queue {}", lookup.getQueueId());
            result = mapper.load(lookup);
        } else {
            LOGGER.warn("Load - {}", WARN_MESSAGE);
        }

        return result;
    }

    /**
     * Method to update a queue attributes.
     * We do a 'partial' update here using SaveBehavior.UPDATE_SKIP_NULL_ATTRIBUTES,
     * so that only non-null attributes are persisted to the database.
     *
     * @param queue Queue object with values only for attributes that need to be updated.
     * */
    public void update(Queue queue) {
        if (isValid(queue)) {
            DynamoDBMapperConfig mapperConfig = DynamoDBMapperConfig.builder()
                    .withSaveBehavior(DynamoDBMapperConfig.SaveBehavior.UPDATE_SKIP_NULL_ATTRIBUTES)
                    .withConsistentReads(DynamoDBMapperConfig.ConsistentReads.CONSISTENT)
                    .build();

            // update the queue only if it already exists
            Map<String, ExpectedAttributeValue> expectedAttributes = new HashMap<String, ExpectedAttributeValue>() {{
                put(Queue.COL_QUEUE_ID, new ExpectedAttributeValue()
                        .withValue(new AttributeValue(queue.getQueueId()))
                        .withExists(true));
            }};
            DynamoDBSaveExpression saveExpression = new DynamoDBSaveExpression().withExpected(expectedAttributes);
            mapper.save(queue, saveExpression, mapperConfig);
        } else {
            LOGGER.warn("Update - {}", WARN_MESSAGE);
        }
    }

    /**
     * Method to increment the last_processed_token of a queue by 1.
     * Used when the user processes the current subscriber on the queue.
     *
     * We do not use the DynamoDbMapper here, since it does not have an
     * option to directly increment the value of an attribute.
     *
     * @param queueId The ID of the queue to update.
     * @return The updated value for last_processed_token.
     * */
    public String incrementLastProcessedToken(String queueId) throws ConditionalCheckFailedException {
        AmazonDynamoDB dynamoDbClient = DynamoUtil.DYNAMO_CLIENT;

        UpdateItemRequest updateItemRequest = new UpdateItemRequest()
                .withTableName(Queue.TABLE_NAME)
                .withKey(new HashMap<String, AttributeValue>() {{
                    put(Queue.COL_QUEUE_ID, new AttributeValue(queueId));
                }})
                .withUpdateExpression("set #last_processed_token = #last_processed_token + :one")
                .withConditionExpression("last_processed_token < #last_generated_token")
                .withExpressionAttributeNames(new HashMap<String, String>() {{
                    put("#last_processed_token", Queue.COL_LAST_PROC_TOKEN);
                    put("#last_generated_token", Queue.COL_LAST_GEN_TOKEN);
                }})
                .withExpressionAttributeValues(new HashMap<String, AttributeValue>() {{
                    put(":one", new AttributeValue().withN("1"));
                }})
                .withReturnValues(ReturnValue.UPDATED_NEW);
        UpdateItemResult updateItemResult = dynamoDbClient.updateItem(updateItemRequest);
        return updateItemResult.getAttributes().get(Queue.COL_LAST_PROC_TOKEN).getN();
    }

    public void delete(Queue queueToDelete) {
        if (isValid(queueToDelete)) {
            LOGGER.info("Deleting queue {}", queueToDelete);
            mapper.delete(queueToDelete);
        } else {
            LOGGER.warn("Delete - {}", WARN_MESSAGE);
        }
    }

    public ScanResultPage<Queue> scan(String paginationToken) {
        DynamoDBScanExpression scanExpression = new DynamoDBScanExpression();
        if (paginationToken != null) {
            scanExpression.withExclusiveStartKey(new HashMap<String, AttributeValue>() {{
                put(Queue.COL_QUEUE_ID, new AttributeValue(paginationToken));
            }});
        }

        return mapper.scanPage(Queue.class, scanExpression);
    }

    private boolean isValid(Queue queue) {
        return queue != null && queue.getQueueId() != null;
    }
}
