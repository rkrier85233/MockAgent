package com.cleo.prototype.entities.event;

import com.cleo.prototype.entities.common.ResourceSupport;
import com.cleo.prototype.entities.dataflow.Recurrence;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DataFlowEvent extends ResourceSupport {
    private String id;
    private String name;
    private String action;
    private String jobToken;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String operation;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Recurrence recurrence;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private List<Source> sources;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private List<Destination> destinations;

    @Setter
    @Getter
    public static class Source {
        private String agentId;
        private String x509Certificate;
        private String itemMatch;
        private String pattern;
        private String subPath;
        private String datastoreId;
    }

    @Getter
    @Setter
    public static class Destination {
        private String agentId;
        private String x509Certificate;
        private String subPath;
        private String datastoreId;
        private String transport;
    }
}
