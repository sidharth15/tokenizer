package com.tokenizer.lambda.service;

import com.tokenizer.lambda.dao.QueueRepository;
import com.tokenizer.lambda.model.queues.Queue;

public class QueueService {
    private QueueRepository repository;

    public QueueService(QueueRepository repository) {
        this.repository = repository;
    }

    public void createNewQueue(String queueId, Integer maxSize) {
        repository.save(new Queue(queueId, 0,0, maxSize));
    }
}
