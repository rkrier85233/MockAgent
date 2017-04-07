package com.cleo.prototype.entities.activation;

import lombok.Builder;
import lombok.Getter;

@Getter
public class ActivationRequest {
    private String name;
    private String activationToken;
    private String publicCertData;

    private ActivationRequest() {
    }

    @Builder
    public ActivationRequest(String name, String activationToken, String publicCertData) {
        this.name = name;
        this.activationToken = activationToken;
        this.publicCertData = publicCertData;
    }
}
