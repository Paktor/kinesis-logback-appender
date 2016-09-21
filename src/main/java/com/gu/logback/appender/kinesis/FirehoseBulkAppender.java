package com.gu.logback.appender.kinesis;

import com.amazonaws.services.kinesisfirehose.AmazonKinesisFirehoseAsyncClient;
import com.amazonaws.services.kinesisfirehose.model.Record;
import com.gu.logback.appender.kinesis.helpers.FirehoseBatchStatsReporter;

import java.nio.ByteBuffer;

/**
 * @author Mikhail Tyamin <a href="mailto:mikhail.tiamine@gmail.com>mikhail.tiamine@gmail.com</a>
 */
public class FirehoseBulkAppender extends FirehoseAppender {
    private FirehoseBatchStatsReporter asyncCallHandler = new FirehoseBatchStatsReporter(this);

    private FirehoseBulkSender firehoseBulkSender;

    @Override
    public void start() {
        super.start();
        firehoseBulkSender = new FirehoseBulkSender(getBatchSize(),
                getDelayInMillis(),
                (AmazonKinesisFirehoseAsyncClient) getClient(),
                asyncCallHandler,
                getStreamName());
        firehoseBulkSender.startService();
    }

    @Override
    protected void putMessage(String message) throws Exception {
        ByteBuffer data = ByteBuffer.wrap(message.getBytes(getEncoding()));
        Record record = new Record().withData(data);
        firehoseBulkSender.addRecord(record);
    }
}
