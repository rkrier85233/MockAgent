package com.cleo.prototype.entities.event;

import com.cleo.prototype.entities.common.ResourceSupport;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DataFlowEvent extends ResourceSupport {
    private String id;
    private String action;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String operation;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private List<Source> sources;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private List<Destination> destinations;

    @Getter
    public static class Source {
        private String agentId;
        private String x509Certificate;
        private List<EventEndpoint> endpoints;
    }

    @Getter
    @Setter
    public static class Destination {
        private String agentId;
        private String x509Certificate;
        private String transport;
        private List<EventEndpoint> endpoints;
    }

    @Getter
    @Setter
    public static class EventEndpoint {
        private String uri;
        private String config;
    }
}
