package com.cleo.prototype.entities.dataflow;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum FlowOperation {
    MOVE,
    COPY;

    @Override
    @JsonValue
    public String toString() {
        return name();
    }

    @JsonCreator
    public static FlowOperation fromString(String param) {
        if (param == null) {
            throw new IllegalArgumentException("FlowOperation cannot be null.");
        }
        return valueOf(param.toUpperCase());
    }
}
