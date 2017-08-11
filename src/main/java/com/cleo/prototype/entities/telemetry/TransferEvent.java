package com.cleo.prototype.entities.telemetry;

import com.cleo.prototype.entities.common.JacksonConfig;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import java.util.Date;

import lombok.Getter;

@Getter
public abstract class TransferEvent {
    private String dataflowId;
    private String jobId;
    private String jobToken;
    private String agentId;
    @JsonSerialize(using = JacksonConfig.DateSerializer.class)
    @JsonDeserialize(using = JacksonConfig.DateDeserializer.class)
    private Date startDate;

    public TransferEvent(String dataflowId, String jobId, String jobToken, String agentId, Date startDate) {
        this.dataflowId = dataflowId;
        this.jobId = jobId;
        this.jobToken = jobToken;
        this.agentId = agentId;
        this.startDate = startDate;
    }

    //    @JsonIgnore
    public abstract String getType();
}
