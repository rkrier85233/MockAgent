package com.cleo.prototype.entities.dataflow;

import com.cleo.prototype.entities.common.ResourceSupport;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Source extends ResourceSupport {
    private String accessPointId;

    public Source() {
    }

    @Builder
    public Source(String accessPointId) {
        this.accessPointId = accessPointId;
    }
}
