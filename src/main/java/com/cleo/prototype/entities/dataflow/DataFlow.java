package com.cleo.prototype.entities.dataflow;

import com.cleo.prototype.entities.common.ResourceSupport;
import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DataFlow extends ResourceSupport {
    @JsonInclude(value = JsonInclude.Include.NON_NULL)
    private String id;
    private String name;
    private String description;
    private FlowOperation operation;
    private Recurrence recurrence;
    private Source source;
    private Destination destination;

    public DataFlow() {
    }

    @Builder
    public DataFlow(String name, String description, FlowOperation operation) {
        this.name = name;
        this.description = description;
        this.operation = operation;
    }
}
