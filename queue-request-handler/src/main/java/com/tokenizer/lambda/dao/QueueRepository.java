package com.tokenizer.lambda.dao;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.AttributeValueUpdate;
import com.amazonaws.services.dynamodbv2.model.UpdateItemRequest;
import com.tokenizer.lambda.model.queues.Queue;
import com.tokenizer.lambda.util.DynamoUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
     * Method to update the value of last generated token or last processed token of a Queue.
     * @param queueId The ID of the queue.
     * @param attributeName The attribute to update - either last_generated_token OR last_processed_token.
     * @param attributeValue The new attribute value.
     * */
    public void update(String queueId, String attributeName, String attributeValue) {
        if (queueId != null && attributeName != null && attributeValue != null) {
            AmazonDynamoDB dynamoDB = DynamoUtil.DYNAMO_CLIENT;
            UpdateItemRequest updateItemRequest = new UpdateItemRequest()
                    .withTableName(Queue.TABLE_NAME)
                    .addKeyEntry(Queue.COL_QUEUE_ID, new AttributeValue().withS(queueId))
                    .addAttributeUpdatesEntry(attributeName,
                            new AttributeValueUpdate().withValue(new AttributeValue().withN(attributeName)));

            dynamoDB.updateItem(updateItemRequest);
        } else {
            LOGGER.warn("Update - {}", WARN_MESSAGE);
        }
    }

    private boolean isValid(Queue queue) {
        return queue != null && queue.getQueueId() != null;
    }
}
