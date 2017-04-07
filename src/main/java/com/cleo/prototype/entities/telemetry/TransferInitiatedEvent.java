package com.cleo.prototype.entities.telemetry;

import com.cleo.prototype.entities.common.JacksonConfig;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import java.util.Date;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TransferInitiatedEvent {
    private String agentId;
    @JsonSerialize(using = JacksonConfig.DateSerializer.class)
    @JsonDeserialize(using = JacksonConfig.DateDeserializer.class)
    private Date timestamp;
}
