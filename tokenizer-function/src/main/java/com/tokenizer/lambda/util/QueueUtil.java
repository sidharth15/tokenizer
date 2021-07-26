package com.tokenizer.lambda.util;

import java.util.UUID;

public class QueueUtil {
    public static synchronized String generateQueueId() {
        return UUID.randomUUID().toString();
    }
}
