package com.cleo.prototype.agent;

import com.cleo.prototype.entities.telemetry.TransferCompleteEvent;
import com.cleo.prototype.entities.telemetry.TransferDetailEvent;
import com.cleo.prototype.entities.telemetry.TransferInitiatedEvent;
import com.cleo.prototype.entities.telemetry.TransferStatusEvent;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import net.minidev.json.JSONObject;

import org.jboss.resteasy.client.jaxrs.ResteasyClient;
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import org.jboss.resteasy.client.jaxrs.ResteasyWebTarget;
import org.jboss.resteasy.client.jaxrs.engines.URLConnectionEngine;
import org.jboss.resteasy.plugins.providers.jackson.ResteasyJackson2Provider;

import java.text.DecimalFormat;
import java.time.Instant;
import java.util.Date;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.LongStream;

import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import lombok.Builder;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import static com.cleo.prototype.entities.common.Link.getLink;

@Slf4j
@Getter
@Builder
public class MockTransfer implements Runnable {
    private static final ObjectMapper objectMapper = new ObjectMapper();

    private static ExecutorService executor = Executors.newFixedThreadPool(5);

    static {
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    private JSONObject event;
    private String agentId;
    private String destAgentId;

    @Override
    public void run() {
        getLink(event, "initiate");
        final String initiate = getLink(event, "initiate").getHref();
        final String details = getLink(event, "details").getHref();
        final String status = getLink(event, "status").getHref();
        final String complete = getLink(event, "result").getHref();

        final long oneMeg = (long) Math.pow(1024, 2);
        final long[] lens = new long[]{
                10 * oneMeg,
                20 * oneMeg,
                15 * oneMeg,
                3 * oneMeg,
                12 * oneMeg,
                7 * oneMeg,
                1 * oneMeg,
                4 * oneMeg,
                25 * oneMeg,
                11 * oneMeg,
                6 * oneMeg
        };

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

        final TransferInitiatedEvent transferInitiatedEvent = new TransferInitiatedEvent();
        transferInitiatedEvent.setAgentId(agentId);
        executor.execute(TransferInitiatedTask.builder()
                .target(client.target(initiate))
                .event(transferInitiatedEvent)
                .build());

        TransferDetailEvent transferDetailEvent = new TransferDetailEvent();
        transferDetailEvent.setAgentId(agentId);
        transferDetailEvent.setTotalItems(srcFiles.length);
        transferDetailEvent.setTotalBytes(LongStream.of(lens).sum());
        for (int i = 0; i < srcFiles.length; i++) {
            transferDetailEvent.getItems().add(TransferDetailEvent.Item.builder()
                    .name(srcFiles[i])
                    .size(lens[i])
                    .build());
        }
        executor.execute(TransferDetailTask.builder()
                .target(client.target(details))
                .event(transferDetailEvent)
                .build());

        for (int i = 0; i < srcFiles.length; i++) {
            final String srcFileName = srcFiles[i];
            final String destFileName = destFiles[i];
            final long fileSize = lens[i];

            executor.execute(TransferStatusTask.builder()
                    .target(client.target(status))
                    .agentId(agentId)
                    .direction("outbound")
                    .name(srcFileName)
                    .size(fileSize)
                    .build());

            executor.execute(TransferStatusTask.builder()
                    .target(client.target(status))
                    .agentId(destAgentId)
                    .direction("INBOUND")
                    .name(destFileName)
                    .size(fileSize)
                    .build());
        }

        // Send a mock final status from the source agent ID for the entire transfer.
        TransferCompleteEvent transferCompleteEvent = new TransferCompleteEvent();
        transferCompleteEvent.setAgentId(agentId);
        transferCompleteEvent.setState("SUCCESS");
        transferCompleteEvent.setTotal(srcFiles.length);
        transferCompleteEvent.setSucceeded(srcFiles.length);
        transferCompleteEvent.setFailed(0);
        executor.execute(TransferCompleteTask.builder()
                .target(client.target(complete))
                .event(transferCompleteEvent)
                .build());
    }

    @Builder
    private static class TransferInitiatedTask implements Runnable {
        private ResteasyWebTarget target;
        private TransferInitiatedEvent event;

        @Override
        public void run() {
            event.setTimestamp(new Date());
            postWithRetry(target, event);
        }
    }

    @Builder
    private static class TransferDetailTask implements Runnable {
        private ResteasyWebTarget target;
        private TransferDetailEvent event;

        @Override
        public void run() {
            event.setTimestamp(new Date());
            postWithRetry(target, event);
        }
    }

    private static class TransferStatusTask implements Runnable {
        private ResteasyWebTarget target;
        private TransferStatusEvent event;

        @Builder
        public TransferStatusTask(ResteasyWebTarget target, String agentId, String direction, String name, long size) {
            this.target = target;
            event = new TransferStatusEvent();
            event.setAgentId(agentId);
            event.setDirection(direction);
            event.setName(name);
            event.setSize(size);
        }

        @Override
        public void run() {
            final TransferStatusEvent event = this.event.copy();
            event.setState("IN_PROGRESS");
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
                event.setState("IN_PROGRESS");
                event.setBytesTransferred(bytesSent);
                event.setTimestamp(new Date());
                postWithRetry(target, event);
                sleep(2, TimeUnit.MILLISECONDS);
            }

            event.setState("SUCCESS");
            event.setBytesTransferred((long) bytesSent);
            postWithRetry(target, event);
        }
    }

    @Builder
    private static class TransferCompleteTask implements Runnable {
        private ResteasyWebTarget target;
        private TransferCompleteEvent event;

        @Override
        public void run() {
            event.setTimestamp(new Date());
            postWithRetry(target, event);
            log.info("Transfer complete sent.");
        }
    }

    private static void sleep(long timeout, TimeUnit unit) {
        try {
            unit.sleep(timeout);
        } catch (InterruptedException e) {
            log.warn("Unable to sleep for {}:{}.", unit, timeout);
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
            log.warn("Response from SaaS was: {}-{}, retry: {} of 5. duration {} milliseconds.", response.getStatus(), response.getStatusInfo(), i + 1, millis);
            sleep(1, TimeUnit.SECONDS);
        }
        if (response.getStatusInfo() != Response.Status.NO_CONTENT) {
            String msg = String.format("Response from SaaS was: %s-%s after all retries failed.", response.getStatus(), response.getStatusInfo());
            throw new RuntimeException(msg);
        }
    }
}
