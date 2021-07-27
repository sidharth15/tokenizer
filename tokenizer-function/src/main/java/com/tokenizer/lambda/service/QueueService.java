package com.tokenizer.lambda.service;

import com.tokenizer.lambda.dao.QueueRepository;
import com.tokenizer.lambda.model.queues.Queue;

public class QueueService {
    private QueueRepository repository;

    public QueueService(QueueRepository repository) {
        this.repository = repository;
    }

    public void initNewQueue(String queueId, String queueName, Integer maxSize, boolean disabled) {
        repository.save(new Queue(queueId, queueName,0,0, maxSize, disabled));
    }

    public Queue describeQueue(String queueId) {
        return repository.load(new Queue(queueId));
    }

    public void deleteQueue(String queueId) {
        repository.delete(new Queue(queueId));
    }

    public void updateQueueMaxSize(String queueId, Integer maxSize) {
        repository.update(queueId, Queue.COL_MAX_SIZE, maxSize.toString());
    }

    public void updateQueueLastGeneratedToken(String queueId, Integer lastGeneratedToken) {
        repository.update(queueId, Queue.COL_LAST_GEN_TOKEN, lastGeneratedToken.toString());
    }

    public void updateQueueLastProcessedToken(String queueId, Integer lastProcessedToken) {
        repository.update(queueId, Queue.COL_LAST_PROC_TOKEN, lastProcessedToken.toString());
    }

    public void enableQueue(String queueId) {
        repository.update(queueId, Queue.COL_DISABLED, Boolean.FALSE.toString());
    }

    public void disableQueue(String queueId) {
        repository.update(queueId, Queue.COL_DISABLED, Boolean.TRUE.toString());
    }
}
