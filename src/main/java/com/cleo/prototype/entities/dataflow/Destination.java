package com.cleo.prototype.entities.dataflow;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Destination {
    private String datastoreId;
    private String subPath;

    // Public no arg constructor needed by persistence layer.
    public Destination() {
    }

    @Builder
    private Destination(String datastoreId, String subPath) {
        this.datastoreId = datastoreId;
        this.subPath = subPath;
    }
}
