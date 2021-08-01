package com.tokenizer.lambda.model.users;

public enum UserState {
    /**
     * State when the subscribed user has been processed by
     * the owner and can now be removed from the table as a
     * subscriber, if needed.
     * */
    DONE,

    /**
     * State when the subscribed user is yet to be processed by
     * the owner.
     * */
    WAITING
}
