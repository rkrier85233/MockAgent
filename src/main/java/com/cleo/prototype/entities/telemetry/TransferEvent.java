package com.cleo.prototype.entities.telemetry;

import lombok.Getter;

@Getter
public abstract class TransferEvent {
    private String dataflowId;
    private String jobId;
    private String jobToken;
    private String agentId;

    public TransferEvent(String dataflowId, String jobId, String jobToken, String agentId) {
        this.dataflowId = dataflowId;
        this.jobId = jobId;
        this.jobToken = jobToken;
        this.agentId = agentId;
    }
}
