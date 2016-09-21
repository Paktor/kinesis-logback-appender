package com.gu.logback.appender.kinesis;

import com.amazonaws.services.kinesisfirehose.AmazonKinesisFirehoseAsyncClient;
import com.amazonaws.services.kinesisfirehose.model.PutRecordBatchRequest;
import com.amazonaws.services.kinesisfirehose.model.Record;
import com.google.common.util.concurrent.AbstractScheduledService;
import com.gu.logback.appender.kinesis.helpers.FirehoseBatchStatsReporter;

import javax.annotation.PreDestroy;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.stream.IntStream;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.stream.Collectors.toList;

/**
 * @author Mikhail Tyamin <a href="mailto:mikhail.tiamine@gmail.com>mikhail.tiamine@gmail.com</a>
 */
public class FirehoseBulkSender extends AbstractScheduledService {
    private static final long TIMEOUT_IN_MILLIS = 3_000L;
    private final LinkedBlockingQueue<Record> recordQueue;
    private final AmazonKinesisFirehoseAsyncClient client;
    private final long delayInMillis;
    private final FirehoseBatchStatsReporter asyncCallHandler;
    private final String streamName;
    private final int batchSize;

    public FirehoseBulkSender(int batchSize, long delayInMillis,
                              AmazonKinesisFirehoseAsyncClient client,
                              FirehoseBatchStatsReporter asyncCallHandler,
                              String streamName) {
        this.batchSize = batchSize;
        this.recordQueue = new LinkedBlockingQueue<>(this.batchSize);
        this.delayInMillis = delayInMillis;
        this.client = client;
        this.asyncCallHandler = asyncCallHandler;
        this.streamName = streamName;
    }

    @Override
    protected void runOneIteration() throws Exception {
        drainRecordQueue();
    }

    public void addRecord(Record record) throws InterruptedException {
        if (recordQueue.offer(record)) {
            drainRecordQueue();
        }
        recordQueue.offer(record, TIMEOUT_IN_MILLIS, MILLISECONDS);
    }

    private void drainRecordQueue() {
        List<Record> recordBatch = IntStream.range(0, batchSize).
                mapToObj(index -> recordQueue.poll()).
                filter(Objects::nonNull).
                collect(toList());
        sendBatch(recordBatch);
    }

    private void sendBatch(List<Record> records) {
        if (records.isEmpty()) {
            return;
        }
        PutRecordBatchRequest putRecordBatchRequest = new PutRecordBatchRequest().
                withDeliveryStreamName(streamName).
                withRecords(records);
        client.putRecordBatchAsync(putRecordBatchRequest, asyncCallHandler);
    }

    @Override
    protected Scheduler scheduler() {
        return Scheduler.newFixedRateSchedule(0, delayInMillis, MILLISECONDS);
    }

    public void startService() {
        if (!isRunning()) {
            startAsync();
            awaitRunning();
        }
    }

    @PreDestroy
    public void stopService() {
        drainRecordQueue();
        stopAsync();
    }
}
