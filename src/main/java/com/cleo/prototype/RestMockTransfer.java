package com.cleo.prototype;

import com.cleo.prototype.entities.event.DataFlowEvent;
import com.cleo.prototype.entities.telemetry.TransferCompleteEvent;
import com.cleo.prototype.entities.telemetry.TransferDetailEvent;
import com.cleo.prototype.entities.telemetry.TransferInitiatedEvent;
import com.cleo.prototype.entities.telemetry.TransferStatusEvent;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.jboss.resteasy.client.jaxrs.ResteasyClient;
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import org.jboss.resteasy.client.jaxrs.ResteasyWebTarget;
import org.jboss.resteasy.client.jaxrs.engines.URLConnectionEngine;
import org.jboss.resteasy.plugins.providers.jackson.ResteasyJackson2Provider;

import java.net.URI;
import java.text.DecimalFormat;
import java.time.Instant;
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

import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;

import lombok.Builder;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Getter
@Builder
public class RestMockTransfer implements Runnable {
    private static final ObjectMapper objectMapper = new ObjectMapper();

    static {
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    private DataFlowEvent event;

    @Override
    public void run() {
        ExecutorService executor = Executors.newFixedThreadPool(5);

        final String dataflowId = event.getId();
        final String jobToken = event.getJobToken();
        final String jobId = UUID.randomUUID().toString();
        final URI initiate = event.getLink("initiate").getUri();
        final URI details = event.getLink("details").getUri();
        final URI status = event.getLink("status").getUri();
        final URI complete = event.getLink("result").getUri();
        final String agentId = event.getSources().get(0).getAgentId();
        final String destAgentId = event.getDestinations().get(0).getAgentId();

        final long oneMeg = (long) Math.pow(1024, 2);
        final long[] lens = new long[]{
                10 * oneMeg,
                20 * oneMeg,
                15 * oneMeg,
                3 * oneMeg,
                0,
                12 * oneMeg,
                7 * oneMeg,
                1 * oneMeg,
                4 * oneMeg,
                25 * oneMeg,
                11 * oneMeg,
                6 * oneMeg
        };

//        final long[] lens = new long[0];

        final String[] srcFiles = new String[lens.length];
        final String[] destFiles = new String[lens.length];
        final DecimalFormat fmt = new DecimalFormat("000");
        for (int i = 0; i < lens.length; i++) {
            String name = "file" + fmt.format(i) + ".bin";
            srcFiles[i] = "/foo/bar/" + name;
            destFiles[i] = "/remote/dir/rec/" + name;
        }

        final ResteasyJackson2Provider resteasyJacksonProvider = new ResteasyJackson2Provider();
        resteasyJacksonProvider.setMapper(objectMapper);

        final ResteasyClient client = new ResteasyClientBuilder()
                .register(resteasyJacksonProvider)
                .httpEngine(new URLConnectionEngine())
                .connectionPoolSize(10)
                .build();

        log.info("Executing mock transfer for data flow: {}, jobId: {}.", event.getName(), jobId);
        Date startDate = new Date();

        URI uri = UriBuilder.fromUri(initiate).build();
        final TransferInitiatedEvent transferInitiatedEvent = new TransferInitiatedEvent(dataflowId, jobId, jobToken, agentId, startDate);
        waitFor(executor.submit(TransferInitiatedTask.builder()
                .target(client.target(uri))
                .event(transferInitiatedEvent)
                .build()));

        TransferDetailEvent transferDetailEvent = new TransferDetailEvent(dataflowId, jobId, jobToken, agentId, startDate);
        transferDetailEvent.setTotalItems(srcFiles.length);
        transferDetailEvent.setTotalBytes(LongStream.of(lens).sum());
        for (int i = 0; i < srcFiles.length; i++) {
            transferDetailEvent.getItems().add(TransferDetailEvent.Item.builder()
                    .name(srcFiles[i])
                    .size(lens[i])
                    .build());
        }
        waitFor(executor.submit(TransferDetailTask.builder()
                .target(client.target(details))
                .event(transferDetailEvent)
                .build()));

        List<Future<Void>> futures = new ArrayList<>(srcFiles.length);
        for (int i = 0; i < srcFiles.length; i++) {
            final String srcFileName = srcFiles[i];
            final String destFileName = destFiles[i];
            final long fileSize = lens[i];

            futures.add(executor.submit(TransferStatusTask.builder()
                    .target(client.target(status))
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
                    .target(client.target(status))
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

        futures.forEach(RestMockTransfer::waitFor);

        // Send a mock final status from the source agent ID for the entire transfer.
        TransferCompleteEvent transferCompleteEvent = new TransferCompleteEvent(dataflowId, jobId, jobToken, agentId, startDate);
        transferCompleteEvent.setStatus("SUCCESS");
        transferCompleteEvent.setTotalComplete(srcFiles.length);
        transferCompleteEvent.setTotalSucceeded(srcFiles.length);
        transferCompleteEvent.setTotalFailed(0);
        transferCompleteEvent.setTotalBytesTransferred(LongStream.of(lens).sum());
        waitFor(executor.submit(TransferCompleteTask.builder()
                .target(client.target(complete))
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
        private ResteasyWebTarget target;
        private TransferInitiatedEvent event;

        @Override
        public Void call() throws Exception {
            postWithRetry(target, event);
            return null;
        }
    }

    @Builder
    private static class TransferDetailTask implements Callable<Void> {
        private ResteasyWebTarget target;
        private TransferDetailEvent event;

        @Override
        public Void call() throws Exception {
            event.setTimestamp(new Date());
            postWithRetry(target, event);
            return null;
        }
    }

    private static class TransferStatusTask implements Callable<Void> {
        private ResteasyWebTarget target;
        private TransferStatusEvent event;

        @Builder
        public TransferStatusTask(ResteasyWebTarget target, String topic, String dataflowId, String jobId, String jobToken,
                                  String agentId, Date startDate, String direction, String name, long size) {
            this.target = target;
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
            postWithRetry(target, event);
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
                postWithRetry(target, event);
                sleep(500, TimeUnit.MILLISECONDS);
            }

            event.setStatus("SUCCESS");
            event.setBytesTransferred((long) bytesSent);
            postWithRetry(target, event);
            return null;
        }
    }

    @Builder
    private static class TransferCompleteTask implements Callable<Void> {
        private ResteasyWebTarget target;
        private TransferCompleteEvent event;

        @Override
        public Void call() throws Exception {
            event.setTimestamp(new Date());
            postWithRetry(target, event);
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

    private static void postWithRetry(ResteasyWebTarget target, Object body) {
        Response response = null;
        for (int i = 0; i < 5; i++) {
            Instant start = Instant.now();
            response = target.request().post(Entity.entity(body, MediaType.APPLICATION_JSON_TYPE));
            Instant end = Instant.now();
            if (response.getStatusInfo() == Response.Status.NO_CONTENT) {
                break;
            }
            long millis = end.toEpochMilli() - start.toEpochMilli();
            String error = response.readEntity(String.class);
            log.warn("Response from SaaS was: {}-{}-{}, retry: {} of 5. duration {} milliseconds.", response.getStatus(), response.getStatusInfo(), error, i + 1, millis);
            sleep(1, TimeUnit.SECONDS);
        }
        if (response.getStatusInfo() != Response.Status.NO_CONTENT) {
            String msg = String.format("Response from SaaS was: %s-%s after all retries failed.", response.getStatus(), response.getStatusInfo());
            throw new RuntimeException(msg);
        }
    }
}
