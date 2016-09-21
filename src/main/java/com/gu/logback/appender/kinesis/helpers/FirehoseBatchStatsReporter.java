package com.gu.logback.appender.kinesis.helpers;

import com.amazonaws.handlers.AsyncHandler;
import com.amazonaws.services.kinesisfirehose.model.PutRecordBatchRequest;
import com.amazonaws.services.kinesisfirehose.model.PutRecordBatchResult;
import com.gu.logback.appender.kinesis.FirehoseAppender;

/**
 * @author Mikhail Tyamin <a href="mailto:mikhail.tiamine@gmail.com>mikhail.tiamine@gmail.com</a>
 */
public class FirehoseBatchStatsReporter implements AsyncHandler<PutRecordBatchRequest, PutRecordBatchResult> {
    private final String appenderName;
    private long successfulRequestCount;
    private long failedRequestCount;
    private final FirehoseAppender<?> appender;

    public FirehoseBatchStatsReporter(FirehoseAppender<?> appender) {
        this.appenderName = appender.getStreamName();
        this.appender = appender;
    }

    @Override
    public void onError(Exception exception) {
        failedRequestCount++;
        appender.addError("Failed to publish a log entries to firehose using appender: " + appenderName, exception);
    }

    @Override
    public void onSuccess(PutRecordBatchRequest request, PutRecordBatchResult putRecordBatchResult) {
        successfulRequestCount++;
    }
}
