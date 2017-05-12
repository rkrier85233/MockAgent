package com.cleo.prototype;

import com.cleo.prototype.agent.MockTransfer;
import com.cleo.prototype.entities.dataflow.Recurrence;
import com.cleo.prototype.entities.event.DataFlowEvent;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CancellationException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class JobScheduler {

    private ScheduledExecutorService executor = Executors.newScheduledThreadPool(5);
    private Map<String, ScheduledFuture<?>> tasks = new HashMap<>();
    private String agentId;

    public JobScheduler(String agentId) {
        this.agentId = agentId;
    }

    public void start(File dataFlowDir) {
        File[] files = dataFlowDir.listFiles(pathname -> pathname.isFile() && pathname.getName().endsWith(".json"));
        files = files == null ? new File[0] : files;
        ObjectMapper objectMapper = new ObjectMapper();
        for (File jsonFile : files) {
            try {
                DataFlowEvent event = objectMapper.readValue(jsonFile, DataFlowEvent.class);
                schedule(event);
            } catch (IOException e) {
                log.warn("Unable to load file: {}, cause: {}", jsonFile, e, e);
            }
        }
    }

    public void schedule(DataFlowEvent event) {
        if (!isMine(event)) {
            return;
        }
        waitTaskCompletion(event);
        Recurrence recurrence = event.getRecurrence();
        if (recurrence != null) {
            MockTransfer mockTransfer = MockTransfer.builder()
                    .event(event)
                    .build();
            ScheduledFuture<?> future = executor.scheduleWithFixedDelay(mockTransfer, recurrence.getInterval(), recurrence.getInterval(), recurrence.getTimeUnit());
            tasks.put(event.getId(), future);
            log.info("Scheduled data flow: {} to run in {} {}", event.getName(), recurrence.getInterval(), recurrence.getTimeUnit());
        }
    }

    public void runNow(DataFlowEvent event) {
        if (!isMine(event)) {
            return;
        }

        waitTaskCompletion(event);
        Runnable task = () -> {
            MockTransfer mockTransfer = MockTransfer.builder()
                    .event(event)
                    .build();
            mockTransfer.run();
            schedule(event);
        };

        ScheduledFuture future = executor.schedule(task, 10, TimeUnit.MILLISECONDS);
        tasks.put(event.getId(), future);
        log.info("Running immediately data flow: {}.", event.getName());
    }

    private void waitTaskCompletion(DataFlowEvent event) {
        ScheduledFuture task = tasks.get(event.getId());
        if (task != null) {
            task.cancel(false);
            try {
                task.get();
            } catch (CancellationException e) {
                // Do nothing.
            } catch (Exception e) {
                log.warn("Unable to wait for previous completion data flow: {}, cause: {}", event.getName(), e, e);
            }
        }
    }

    private boolean isMine(DataFlowEvent event) {
        String srcAgentId = event.getSources().get(0).getAgentId();
        return srcAgentId.equals(agentId);
    }
}
