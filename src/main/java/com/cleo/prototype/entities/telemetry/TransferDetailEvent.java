package com.cleo.prototype.entities.telemetry;

import com.cleo.prototype.entities.common.JacksonConfig;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TransferDetailEvent extends TransferEvent {
    @JsonSerialize(using = JacksonConfig.DateSerializer.class)
    @JsonDeserialize(using = JacksonConfig.DateDeserializer.class)
    private Date timestamp;
    private long totalBytes;
    private long totalItems;
    private List<Item> items = new ArrayList<>();

    public TransferDetailEvent(String dataflowId, String jobId, String jobToken, String agentId, Date startDate) {
        super(dataflowId, jobId, jobToken, agentId, startDate);
    }

    @Override
    public String getType() {
        return "details";
    }

    @Getter
    @Setter
    public static class Item {
        private String name;
        private long size;

        public Item() {
        }

        @Builder
        public Item(String name, long size) {
            this.name = name;
            this.size = size;
        }
    }

}
