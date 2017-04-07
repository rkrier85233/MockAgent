package com.cleo.prototype.entities.dataflow;

import com.cleo.prototype.entities.common.ResourceSupport;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Destination extends ResourceSupport {
    private String accessPointId;
    private String transport;

    public Destination() {
    }

    @Builder
    public Destination(String accessPointId, String transport) {
        this.accessPointId = accessPointId;
        this.transport = transport;
    }
}
