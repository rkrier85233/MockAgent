package com.cleo.prototype.entities.dataflow;

import com.cleo.prototype.entities.common.ResourceSupport;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Endpoint extends ResourceSupport {
    private String uri;
    private String config;

    public Endpoint() {
    }

    @Builder
    public Endpoint(String uri, String config) {
        this.uri = uri;
        this.config = config;
    }
}
