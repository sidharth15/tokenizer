package com.tokenizer.lambda.dao;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapperConfig;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBSaveExpression;
import com.amazonaws.services.dynamodbv2.model.ExpectedAttributeValue;
import com.tokenizer.lambda.model.queues.Queue;
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
                put(Queue.COL_QUEUE_ID, new ExpectedAttributeValue().withExists(true));
            }};
            DynamoDBSaveExpression saveExpression = new DynamoDBSaveExpression().withExpected(expectedAttributes);
            mapper.save(queue, saveExpression, mapperConfig);
        } else {
            LOGGER.warn("Update - {}", WARN_MESSAGE);
        }
    }

    public void delete(Queue queueToDelete) {
        if (isValid(queueToDelete)) {
            LOGGER.info("Deleting queue {}", queueToDelete);
            mapper.delete(queueToDelete);
        } else {
            LOGGER.warn("Delete - {}", WARN_MESSAGE);
        }
    }

    private boolean isValid(Queue queue) {
        return queue != null && queue.getQueueId() != null;
    }
}
