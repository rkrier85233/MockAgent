package com.cleo.prototype.entities.telemetry;

import com.cleo.prototype.entities.common.JacksonConfig;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import java.util.Date;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TransferStatusEvent extends TransferEvent {
    private String direction;
    private String name;
    private long size;
    private long bytesTransferred;
    private String state;
    private String message;
    @JsonSerialize(using = JacksonConfig.DateSerializer.class)
    @JsonDeserialize(using = JacksonConfig.DateDeserializer.class)
    private Date timestamp;

    public TransferStatusEvent(String dataflowId, String jobId, String jobToken, String agentId) {
        super(dataflowId, jobId, jobToken, agentId);
    }

    public TransferStatusEvent copy() {
        TransferStatusEvent copy = new TransferStatusEvent(this.getDataflowId(), this.getJobId(), this.getJobToken(), this.getAgentId());
        copy.setDirection(direction);
        copy.setName(name);
        copy.setSize(size);
        return copy;
    }
}
