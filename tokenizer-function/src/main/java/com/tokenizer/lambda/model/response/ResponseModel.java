package com.tokenizer.lambda.model.response;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ResponseModel<T> {
    public static final String STATUS_CODE = "status_code";
    public static final String MESSAGE = "message";
    public static final String VALUES = "values";
    public static final String PAGINATION_TOKEN = "pagination_token";
    public static final String SUCCESS_MESSAGE = "SUCCESS";
    public static final String FAILURE_MESSAGE = "FAILURE";

    private Integer statusCode;
    private String message;
    private T object;
    private String paginationToken;

    public ResponseModel(Integer statusCode, String message, T object, String paginationToken) {
        this.statusCode = statusCode;
        this.message = message;
        this.object = object;
        this.paginationToken = paginationToken;
    }

    @JsonProperty(STATUS_CODE)
    public Integer getStatusCode() {
        return statusCode;
    }

    public void setStatusCode(Integer statusCode) {
        this.statusCode = statusCode;
    }

    @JsonProperty(MESSAGE)
    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    @JsonProperty(VALUES)
    public T getObject() {
        return object;
    }

    public void setObject(T object) {
        this.object = object;
    }

    @JsonProperty(PAGINATION_TOKEN)
    public String getPaginationToken() {
        return paginationToken;
    }

    public void setPaginationToken(String paginationToken) {
        this.paginationToken = paginationToken;
    }
}
