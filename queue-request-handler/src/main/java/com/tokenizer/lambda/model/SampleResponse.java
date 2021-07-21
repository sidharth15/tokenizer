package com.tokenizer.lambda.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class SampleResponse {
    private String text;
    private int number;
    private boolean flag;

    public SampleResponse(String text, int number, boolean flag) {
        this.text = text;
        this.number = number;
        this.flag = flag;
    }

    @JsonProperty("text")
    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    @JsonProperty("number")
    public int getNumber() {
        return number;
    }

    public void setNumber(int number) {
        this.number = number;
    }

    @JsonProperty("flag")
    public boolean isFlag() {
        return flag;
    }

    public void setFlag(boolean flag) {
        this.flag = flag;
    }
}
