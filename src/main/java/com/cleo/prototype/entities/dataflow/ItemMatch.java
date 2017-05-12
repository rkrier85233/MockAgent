package com.cleo.prototype.entities.dataflow;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum ItemMatch {
    ALL,
    PATTERN_MATCH,
    CONTAINS;

    @Override
    @JsonValue
    public String toString() {
        return name();
    }

    @JsonCreator
    public static ItemMatch fromString(String param) {
        if (param == null) {
            throw new IllegalArgumentException("ItemMatch cannot be null.");
        }
        return valueOf(param.toUpperCase());
    }
}
