package com.cleo.prototype.entities.activation;


import com.cleo.prototype.entities.common.JacksonConfig;
import com.cleo.prototype.entities.common.ResourceSupport;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import java.util.Date;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AgentInfo extends ResourceSupport {
    private String agentId;
    private String name;
    private String publicCertificate;
    private String sqsEndpoint;
    private String awsRegion;
    private Credentials credentials;

    @Getter
    public static class Credentials extends ResourceSupport {
        private String accessKeyId;
        private String secretAccessKey;
        private String sessionToken;
        @JsonSerialize(using = JacksonConfig.DateSerializer.class)
        @JsonDeserialize(using = JacksonConfig.DateDeserializer.class)
        private Date expiration;

    }
}
