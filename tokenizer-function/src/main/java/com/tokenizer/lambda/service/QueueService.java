package com.tokenizer.lambda.service;

import com.amazonaws.services.dynamodbv2.datamodeling.ScanResultPage;
import com.tokenizer.lambda.dao.QueueRepository;
import com.tokenizer.lambda.model.queues.Queue;

import java.util.ArrayList;
import java.util.List;

public class QueueService {
    private QueueRepository repository;

    public QueueService(QueueRepository repository) {
        this.repository = repository;
    }

    public Queue initNewQueue(String queueId, String queueName, Integer maxSize, Boolean disabled) {
        Queue newQueue = new Queue(queueId, queueName,0,0, maxSize, disabled);
        repository.save(newQueue);

        return newQueue;
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

    /**
     * Method to scan and list the queues table.
     * The pagination token parameter is updated on each scan and
     * can be returned to the user.
     * @param paginationToken The pagination token passed by the user.
     * @return list of Queue objects in the current scanned page.
     * */
    public List<Queue> listQueues(String paginationToken) {
        List<Queue> queues = null;
        ScanResultPage<Queue> scanResult = repository.scan(paginationToken);
        if (scanResult != null) {
            queues = scanResult.getResults();
            paginationToken = scanResult.getLastEvaluatedKey() != null ?
                    scanResult.getLastEvaluatedKey().get(Queue.COL_QUEUE_ID).getS(): null;
        }

        return queues != null ? queues: new ArrayList<>();
    }
}
