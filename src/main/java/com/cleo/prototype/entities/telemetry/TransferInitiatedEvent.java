package com.cleo.prototype.entities.telemetry;

import com.cleo.prototype.entities.common.JacksonConfig;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import java.util.Date;

import lombok.Getter;

@Getter
//@Setter
public class TransferInitiatedEvent extends TransferEvent {
    @JsonSerialize(using = JacksonConfig.DateSerializer.class)
    @JsonDeserialize(using = JacksonConfig.DateDeserializer.class)
    private Date timestamp;

    public TransferInitiatedEvent(String dataflowId, String jobId, String jobToken, String agentId, Date startDate) {
        super(dataflowId, jobId, jobToken, agentId, startDate);
        this.timestamp = startDate;
    }

    @Override
    public String getType() {
        return "initiated";
    }
}
