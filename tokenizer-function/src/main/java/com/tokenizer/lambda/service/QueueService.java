package com.tokenizer.lambda.service;

import com.tokenizer.lambda.dao.QueueRepository;
import com.tokenizer.lambda.model.queues.Queue;

public class QueueService {
    private QueueRepository repository;

    public QueueService(QueueRepository repository) {
        this.repository = repository;
    }

    public void initNewQueue(String queueId, String queueName, Integer maxSize, Boolean disabled) {
        repository.save(new Queue(queueId, queueName,0,0, maxSize, disabled));
    }

    public Queue describeQueue(String queueId) {
        return repository.load(new Queue(queueId));
    }

    public void deleteQueue(String queueId) {
        repository.delete(new Queue(queueId));
    }

    public void updateQueue(String queueId, String queueName, Integer maxSize, Boolean disabled) {
        repository.update(new Queue(queueId, queueName, null, null, maxSize, disabled));
    }
}
