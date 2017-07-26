package com.cleo.prototype.iot;

import com.amazonaws.services.iot.client.AWSIotMqttClient;
import com.cleo.prototype.entities.activation.AgentInfo;

import java.util.concurrent.ConcurrentHashMap;

public class AWSIotMqttClientFactory {
    private static final ConcurrentHashMap<String, AWSIotMqttClient> CLIENTS_BY_ID = new ConcurrentHashMap<>();

    private AWSIotMqttClientFactory() {
    }

    public static AWSIotMqttClient getClient(AgentInfo agentInfo) {
        return CLIENTS_BY_ID.computeIfAbsent(agentInfo.getAgentId(), v -> newAWSIotMqttClient(agentInfo));
    }

    private static AWSIotMqttClient newAWSIotMqttClient(AgentInfo agentInfo) {
        final AgentInfo.Credentials agentCredentials = agentInfo.getCredentials();

        return new AWSIotMqttClient(agentInfo.getIotEndpoint(), agentInfo.getAgentId(), agentCredentials.getAccessKeyId(),
                agentCredentials.getSecretAccessKey(),
                agentCredentials.getSessionToken());
    }
}
