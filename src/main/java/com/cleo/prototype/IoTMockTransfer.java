package com.cleo.prototype;

import com.cleo.prototype.agent.JobStatusPublisher;
import com.cleo.prototype.entities.event.DataFlowEvent;
import com.cleo.prototype.entities.telemetry.TransferCompleteEvent;
import com.cleo.prototype.entities.telemetry.TransferDetailEvent;
import com.cleo.prototype.entities.telemetry.TransferInitiatedEvent;
import com.cleo.prototype.entities.telemetry.TransferStatusEvent;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.LongStream;

import lombok.Builder;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Getter
@Builder
public class IoTMockTransfer implements Runnable {
    private static final ObjectMapper objectMapper = new ObjectMapper();
    static {
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    private static final long ONE_MEG = (long) Math.pow(1024, 2);
    private static final long[] FILE_LENGTHS = new long[]{
            10 * ONE_MEG,
            20 * ONE_MEG,
            15 * ONE_MEG,
            3 * ONE_MEG,
            0,
            12 * ONE_MEG,
            7 * ONE_MEG,
            1 * ONE_MEG,
            4 * ONE_MEG,
            25 * ONE_MEG,
            11 * ONE_MEG,
            6 * ONE_MEG
    };

    private JobStatusPublisher publisher;
    private DataFlowEvent event;

    @Override
    public void run() {
        ExecutorService executor = Executors.newFixedThreadPool(5);

        final String dataflowId = event.getId();
        final String jobToken = event.getJobToken();
        final String jobId = UUID.randomUUID().toString();

        final String mqttTopicTemplate = event.getMqttTopicTemplate();
        final String mqttInitiate = String.format(mqttTopicTemplate, dataflowId, jobId, "initiated");
        final String mqttDetails = String.format(mqttTopicTemplate, dataflowId, jobId, "details");
        final String mqttStatus = String.format(mqttTopicTemplate, dataflowId, jobId, "status");
        final String mqttComplete = String.format(mqttTopicTemplate, dataflowId, jobId, "result");

        final String agentId = event.getSources().get(0).getAgentId();
        final String destAgentId = event.getDestinations().get(0).getAgentId();

        final String[] srcFiles = new String[FILE_LENGTHS.length];
        final String[] destFiles = new String[FILE_LENGTHS.length];
        final DecimalFormat fmt = new DecimalFormat("000");
        for (int i = 0; i < FILE_LENGTHS.length; i++) {
            String name = "file" + fmt.format(i) + ".bin";
            srcFiles[i] = "/foo/bar/" + name;
            destFiles[i] = "/remote/dir/rec/" + name;
        }

        log.info("Executing mock transfer for data flow: {}, jobId: {}.", event.getName(), jobId);
        Date startDate = new Date();

        final TransferInitiatedEvent transferInitiatedEvent = new TransferInitiatedEvent(dataflowId, jobId, jobToken, agentId, startDate);
        waitFor(executor.submit(TransferInitiatedTask.builder()
                .target(publisher)
                .topic(mqttInitiate)
                .event(transferInitiatedEvent)
                .build()));

        TransferDetailEvent transferDetailEvent = new TransferDetailEvent(dataflowId, jobId, jobToken, agentId, startDate);
        transferDetailEvent.setTotalItems(srcFiles.length);
        transferDetailEvent.setTotalBytes(LongStream.of(FILE_LENGTHS).sum());
        for (int i = 0; i < srcFiles.length; i++) {
            transferDetailEvent.getItems().add(TransferDetailEvent.Item.builder()
                    .name(srcFiles[i])
                    .size(FILE_LENGTHS[i])
                    .build());
        }
        waitFor(executor.submit(TransferDetailTask.builder()
                .target(publisher)
                .topic(mqttDetails)
                .event(transferDetailEvent)
                .build()));

        List<Future<Void>> futures = new ArrayList<>(srcFiles.length);
        for (int i = 0; i < srcFiles.length; i++) {
            final String srcFileName = srcFiles[i];
            final String destFileName = destFiles[i];
            final long fileSize = FILE_LENGTHS[i];

            futures.add(executor.submit(TransferStatusTask.builder()
                    .target(publisher)
                    .topic(mqttStatus)
                    .dataflowId(dataflowId)
                    .jobToken(jobToken)
                    .jobId(jobId)
                    .agentId(agentId)
                    .startDate(startDate)
                    .direction("outbound")
                    .name(srcFileName)
                    .size(fileSize)
                    .build()));

            futures.add(executor.submit(TransferStatusTask.builder()
                    .target(publisher)
                    .topic(mqttStatus)
                    .dataflowId(dataflowId)
                    .jobToken(jobToken)
                    .jobId(jobId)
                    .agentId(destAgentId)
                    .startDate(startDate)
                    .direction("INBOUND")
                    .name(destFileName)
                    .size(fileSize)
                    .build()));
        }

        futures.forEach(IoTMockTransfer::waitFor);

        // Send a mock final status from the source agent ID for the entire transfer.
        TransferCompleteEvent transferCompleteEvent = new TransferCompleteEvent(dataflowId, jobId, jobToken, agentId, startDate);
        transferCompleteEvent.setStatus("SUCCESS");
        transferCompleteEvent.setTotalComplete(srcFiles.length);
        transferCompleteEvent.setTotalSucceeded(srcFiles.length);
        transferCompleteEvent.setTotalFailed(0);
        transferCompleteEvent.setTotalBytesTransferred(LongStream.of(FILE_LENGTHS).sum());
        waitFor(executor.submit(TransferCompleteTask.builder()
                .topic(mqttComplete)
                .target(publisher)
                .event(transferCompleteEvent)
                .build()));

        try {
            executor.shutdown();
            executor.awaitTermination(10, TimeUnit.MINUTES);
            log.info("Finished mocking a transfer for data flow: {}, jobId: {}.", event.getName(), jobId);
        } catch (InterruptedException e) {
            log.error("Unable to wait for mock transfer for data flow: {} to complete.", event.getName());
        }
    }

    @Builder
    private static class TransferInitiatedTask implements Callable<Void> {
        private JobStatusPublisher target;
        private String topic;
        private TransferInitiatedEvent event;

        @Override
        public Void call() throws Exception {
            target.publish(topic, event);
            return null;
        }
    }

    @Builder
    private static class TransferDetailTask implements Callable<Void> {
        private JobStatusPublisher target;
        private String topic;
        private TransferDetailEvent event;

        @Override
        public Void call() throws Exception {
            event.setTimestamp(new Date());
            target.publish(topic, event);
            return null;
        }
    }

    private static class TransferStatusTask implements Callable<Void> {
        private JobStatusPublisher target;
        private String topic;
        private TransferStatusEvent event;

        @Builder
        public TransferStatusTask(JobStatusPublisher target, String topic, String dataflowId, String jobId, String jobToken,
                                  String agentId, Date startDate, String direction, String name, long size) {
            this.target = target;
            this.topic = topic;
            event = new TransferStatusEvent(dataflowId, jobId, jobToken, agentId, startDate);
            event.setDirection(direction);
            event.setName(name);
            event.setSize(size);
        }

        @Override
        public Void call() throws Exception {
            final TransferStatusEvent event = this.event.copy();
            event.setStatus("IN_PROGRESS");
            event.setBytesTransferred(0);
            event.setTimestamp(new Date());
            target.publish(topic, event);
            sleep(5, TimeUnit.MILLISECONDS);

            final long oneMeg = (long) Math.pow(1024, 2);
            long bytesSent = 0;
            while (bytesSent < event.getSize()) {
                bytesSent += oneMeg;
                bytesSent = Math.min(bytesSent, event.getSize());
                // Send a mock item status, updating bytes sent using agent ID.
                event.setStatus("IN_PROGRESS");
                event.setBytesTransferred(bytesSent);
                event.setTimestamp(new Date());
                target.publish(topic, event);
                sleep(500, TimeUnit.MILLISECONDS);
            }

            event.setStatus("SUCCESS");
            event.setBytesTransferred((long) bytesSent);
            target.publish(topic, event);
            return null;
        }
    }

    @Builder
    private static class TransferCompleteTask implements Callable<Void> {
        private JobStatusPublisher target;
        private String topic;
        private TransferCompleteEvent event;

        @Override
        public Void call() throws Exception {
            event.setTimestamp(new Date());
            target.publish(topic, event);
            log.info("Transfer complete sent.");
            return null;
        }
    }

    private static void sleep(long timeout, TimeUnit unit) {
        try {
            unit.sleep(timeout);
        } catch (InterruptedException e) {
            log.warn("Unable to sleep for {}:{}.", unit, timeout);
        }
    }

    private static void waitFor(Future<Void> future) {
        try {
            future.get();
        } catch (InterruptedException e) {
            log.warn("Task interrupted, cause: {}", e, e);
        } catch (ExecutionException e) {
            log.warn("Task failed, cause: {}", e, e);
        }
    }
}
