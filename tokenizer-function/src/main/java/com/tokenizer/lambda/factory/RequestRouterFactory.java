package com.tokenizer.lambda.factory;

import com.tokenizer.lambda.requests.RequestRouter;
import com.tokenizer.lambda.requests.handlers.GenericResponseHandler;
import com.tokenizer.lambda.requests.handlers.QueueEventHandler;
import com.tokenizer.lambda.service.QueueService;
import com.tokenizer.lambda.service.UserService;

public class RequestRouterFactory {
    public static RequestRouter createRequestRouter(UserService userService,
                                             QueueService queueService) {
        return new RequestRouter(
                new QueueEventHandler(userService, queueService),
                new GenericResponseHandler());
    }
}
