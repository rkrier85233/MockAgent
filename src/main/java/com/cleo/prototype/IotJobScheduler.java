package com.cleo.prototype;

import com.cleo.prototype.agent.JobStatusPublisher;
import com.cleo.prototype.entities.activation.AgentInfo;
import com.cleo.prototype.entities.event.DataFlowEvent;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class IotJobScheduler extends JobScheduler {

    private JobStatusPublisher publisher;

    public IotJobScheduler(AgentInfo agentInfo) {
        super(agentInfo);
        this.publisher = new JobStatusPublisher(agentInfo);
    }

    @Override
    protected Runnable createMockTransfer(DataFlowEvent event) {
        return IoTMockTransfer.builder()
                .publisher(publisher)
                .event(event)
                .build();
    }
}
