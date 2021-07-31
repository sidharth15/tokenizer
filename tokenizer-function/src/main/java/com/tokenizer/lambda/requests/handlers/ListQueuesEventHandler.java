package com.tokenizer.lambda.requests.handlers;

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tokenizer.lambda.model.queues.Queue;
import com.tokenizer.lambda.model.response.ResponseModel;
import com.tokenizer.lambda.requests.EventHandler;
import com.tokenizer.lambda.service.QueueService;
import com.tokenizer.lambda.util.ApiGatewayUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class ListQueuesEventHandler implements EventHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(ListQueuesEventHandler.class);
    private QueueService queueService;
    private ObjectMapper mapper;

    public ListQueuesEventHandler(QueueService queueService, ObjectMapper mapper) {
        this.queueService = queueService;
        this.mapper = mapper;
    }

    @Override
    public String handleEvent(APIGatewayProxyRequestEvent input) {
        String result = null;
        String userId = ApiGatewayUtil.parseUsername(input);

        if (userId != null) {
            String httpMethod = input.getHttpMethod();
            String paginationToken = ApiGatewayUtil.parseQueryStringParameter(input, ApiGatewayUtil.PAGINATION_TOKEN);

            switch (httpMethod) {
                case ApiGatewayUtil.GET:
                    try {
                        List<Queue> queueList = queueService.listQueues(paginationToken);

                        result = ApiGatewayUtil.getResponseJsonString(mapper,
                                buildSuccessMessage(queueList, paginationToken));
                    } catch (Exception e) {
                        LOGGER.error("Error occurred while listing queues: ", e);

                        result = ApiGatewayUtil.getResponseJsonString(mapper,
                                buildFailureMessage(ResponseModel.FAILURE_MESSAGE));
                    }
                    break;

                default:
                    result = ApiGatewayUtil.getResponseJsonString(mapper,
                            buildFailureMessage("Invalid method requested"));
                    break;
            }
        }

        return result;
    }

    private ResponseModel<List<Queue>> buildSuccessMessage(List<Queue> queues, String paginationToken) {
        return new ResponseModel<>(200, ResponseModel.SUCCESS_MESSAGE, queues, paginationToken);
    }

    private ResponseModel<List<Queue>> buildFailureMessage(String errorMessage) {
        return new ResponseModel<>(502, errorMessage, null, null);
    }
}
