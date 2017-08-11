package com.cleo.prototype;

import com.cleo.prototype.entities.activation.AgentInfo;
import com.cleo.prototype.entities.event.DataFlowEvent;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class RestJobScheduler extends JobScheduler {

    public RestJobScheduler(AgentInfo agentInfo) {
        super(agentInfo);
    }

    @Override
    protected Runnable createMockTransfer(DataFlowEvent event) {
        return RestMockTransfer.builder()
                .event(event)
                .build();
    }
}
