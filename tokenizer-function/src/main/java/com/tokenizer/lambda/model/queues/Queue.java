package com.tokenizer.lambda.model.queues;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBAttribute;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBHashKey;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapperFieldModel;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTable;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTyped;

import java.util.Objects;

@DynamoDBTable(tableName = Queue.TABLE_NAME)
public class Queue {
    public static final int DEFAULT_MAX_SIZE = 99;
    public static final String TABLE_NAME = "queues";
    public static final String COL_QUEUE_ID = "queue_id";
    public static final String COL_LAST_GEN_TOKEN = "last_generated_token";
    public static final String COL_LAST_PROC_TOKEN = "last_processed_token";
    public static final String COL_MAX_SIZE = "max_size";
    public static final String COL_DISABLED = "disabled";

    private String queueId;
    private Integer lastGeneratedToken;
    private Integer lastProcessedToken;
    private Integer maxSize;
    private boolean disabled;

    public Queue(String queueId) {
        this.queueId = queueId;
    }

    public Queue(String queueId, Integer lastGeneratedToken, Integer lastProcessedToken, Integer maxSize) {
        this.queueId = queueId;
        this.lastGeneratedToken = lastGeneratedToken;
        this.lastProcessedToken = lastProcessedToken;
        this.maxSize = maxSize != null ? maxSize : DEFAULT_MAX_SIZE;
    }

    @DynamoDBHashKey(attributeName = COL_QUEUE_ID)
    public String getQueueId() {
        return queueId;
    }

    public void setQueueId(String queueId) {
        this.queueId = queueId;
    }

    @DynamoDBAttribute(attributeName = COL_LAST_GEN_TOKEN)
    public Integer getLastGeneratedToken() {
        return lastGeneratedToken;
    }

    public void setLastGeneratedToken(Integer lastGeneratedToken) {
        this.lastGeneratedToken = lastGeneratedToken;
    }

    @DynamoDBAttribute(attributeName = COL_LAST_PROC_TOKEN)
    public Integer getLastProcessedToken() {
        return lastProcessedToken;
    }

    public void setLastProcessedToken(Integer lastProcessedToken) {
        this.lastProcessedToken = lastProcessedToken;
    }

    @DynamoDBAttribute(attributeName = COL_MAX_SIZE)
    public Integer getMaxSize() {
        return maxSize;
    }

    public void setMaxSize(Integer maxSize) {
        this.maxSize = maxSize;
    }

    @DynamoDBTyped(DynamoDBMapperFieldModel.DynamoDBAttributeType.BOOL)
    @DynamoDBAttribute(attributeName = COL_DISABLED)
    public boolean isDisabled() {
        return disabled;
    }

    public void setDisabled(boolean disabled) {
        this.disabled = disabled;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Queue queue = (Queue) o;
        return disabled == queue.disabled &&
                queueId.equals(queue.queueId) &&
                Objects.equals(lastGeneratedToken, queue.lastGeneratedToken) &&
                Objects.equals(lastProcessedToken, queue.lastProcessedToken) &&
                Objects.equals(maxSize, queue.maxSize);
    }

    @Override
    public int hashCode() {
        return Objects.hash(queueId);
    }

    @Override
    public String toString() {
        return "Queue{" +
                "queueId='" + queueId + '\'' +
                ", lastGeneratedToken=" + lastGeneratedToken +
                ", lastProcessedToken=" + lastProcessedToken +
                ", maxSize=" + maxSize +
                ", disabled=" + disabled +
                '}';
    }
}
