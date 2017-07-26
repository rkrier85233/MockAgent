package com.cleo.prototype.entities.telemetry;

import com.cleo.prototype.entities.common.JacksonConfig;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import java.util.Date;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TransferCompleteEvent extends TransferEvent {
    @JsonSerialize(using = JacksonConfig.DateSerializer.class)
    @JsonDeserialize(using = JacksonConfig.DateDeserializer.class)
    private Date timestamp;
    private String state;
    private long total;
    private long succeeded;
    private long failed;
    private long totalBytes;

    public TransferCompleteEvent(String dataflowId, String jobId, String jobToken, String agentId, Date startDate) {
        super(dataflowId, jobId, jobToken, agentId, startDate);
    }

    @Override
    public String getType() {
        return "result";
    }
}
