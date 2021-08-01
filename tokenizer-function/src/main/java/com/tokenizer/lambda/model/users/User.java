package com.tokenizer.lambda.model.users;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBAttribute;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBHashKey;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBIndexHashKey;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBIndexRangeKey;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapperFieldModel;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBRangeKey;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTable;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTyped;

import java.util.Objects;

@DynamoDBTable(tableName = User.TABLE_NAME)
public class User {
    public static final String TABLE_NAME = "tokenizer_users";
    public static final String QUEUE_GSI = "queue_gsi";

    public static final String COL_USER_ID = "user_id";
    public static final String COL_USER_NAME = "user_name";
    public static final String COL_QUEUE_ID = "queue_id";
    public static final String COL_QUEUE_OWNER = "owner";
    public static final String COL_TOKEN_NUM = "token_num";

    private String userId;
    private String queueId;
    private boolean owner;
    private Integer tokenNumber;
    private UserState state;

    public User() {}

    public User(String userId, String queueId) {
        this.userId = userId;
        this.queueId = queueId;
    }

    public User(String queueId, Integer tokenNumber) {
        this.queueId = queueId;
        this.tokenNumber = tokenNumber;
    }

    public User(String userId) {
        this.userId = userId;
    }

    @DynamoDBHashKey(attributeName = COL_USER_ID)
    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    @DynamoDBRangeKey(attributeName = COL_QUEUE_ID)
    @DynamoDBIndexHashKey(attributeName = COL_QUEUE_ID, globalSecondaryIndexName = QUEUE_GSI)
    public String getQueueId() {
        return queueId;
    }

    public void setQueueId(String queueId) {
        this.queueId = queueId;
    }

    @DynamoDBTyped(DynamoDBMapperFieldModel.DynamoDBAttributeType.BOOL)
    @DynamoDBAttribute(attributeName = COL_QUEUE_OWNER)
    public boolean isOwner() {
        return owner;
    }

    public void setOwner(boolean owner) {
        this.owner = owner;
    }

    @DynamoDBIndexRangeKey(attributeName = COL_TOKEN_NUM, globalSecondaryIndexName = QUEUE_GSI)
    public Integer getTokenNumber() {
        return tokenNumber;
    }

    public void setTokenNumber(Integer tokenNumber) {
        this.tokenNumber = tokenNumber;
    }

    public UserState getState() {
        return state;
    }

    public void setState(UserState state) {
        this.state = state;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        User user = (User) o;
        return owner == user.owner &&
                userId.equals(user.userId) &&
                queueId.equals(user.queueId) &&
                Objects.equals(tokenNumber, user.tokenNumber) &&
                state == user.state;
    }

    @Override
    public int hashCode() {
        return Objects.hash(userId, queueId, tokenNumber);
    }

    @Override
    public String toString() {
        return "User{" +
                "userId='" + userId + '\'' +
                ", queueId='" + queueId + '\'' +
                ", owner=" + owner +
                ", tokenNumber=" + tokenNumber +
                ", state=" + state +
                '}';
    }
}