package com.cleo.prototype.agent;

import com.amazonaws.services.iot.client.AWSIotConnectionStatus;
import com.amazonaws.services.iot.client.AWSIotException;
import com.amazonaws.services.iot.client.AWSIotMessage;
import com.amazonaws.services.iot.client.AWSIotMqttClient;
import com.amazonaws.services.iot.client.AWSIotQos;
import com.cleo.prototype.entities.activation.AgentInfo;
import com.cleo.prototype.iot.AWSIotMqttClientFactory;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Date;
import java.util.concurrent.TimeUnit;

import lombok.extern.slf4j.Slf4j;

import static com.cleo.prototype.agent.MockAgent.renewCredentials;

@Slf4j
public class JobStatusPublisher {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final AgentInfo agentInfo;
    private final AWSIotMqttClient client;

    public JobStatusPublisher(AgentInfo agentInfo) {
        this.agentInfo = agentInfo;
        final AgentInfo.Credentials agentCredentials = agentInfo.getCredentials();
        client = AWSIotMqttClientFactory.getClient(agentInfo);
    }

    public void publish(String topic, Object payload) throws JsonProcessingException, AWSIotException {
        AWSIotMessage message = new AWSIotMessage(topic, AWSIotQos.QOS0);
        message.setStringPayload(OBJECT_MAPPER.writeValueAsString(payload));
        updateCredentialsIfNeeded();
        if (client.getConnectionStatus() != AWSIotConnectionStatus.CONNECTED) {
            log.debug("Reconnecting");
            client.connect();
        }

        for (; ; ) {
            try {
                client.publish(message);
                break;
            } catch (AWSIotException e) {
                log.error("Unable to publish message, cause: {}", e);
                try {
                    TimeUnit.MILLISECONDS.sleep(500);
                } catch (InterruptedException e1) {
                    e1.printStackTrace();
                }
            }
        }
    }

    private void updateCredentialsIfNeeded() {
        AgentInfo.Credentials credentials = agentInfo.getCredentials();
        final Date twoMinutesFromNow = new Date(System.currentTimeMillis() + (2 * 60 * 1000));
        if (credentials.getExpiration().before(twoMinutesFromNow)) {
            // Token will expire in two minutes or less, time to renew.
            log.info("Token expiring... renewing token.");
            credentials = renewCredentials(agentInfo);
            agentInfo.setCredentials(credentials);
            log.info("Renewed Expiration is: {}", credentials.getExpiration());
            client.updateCredentials(credentials.getAccessKeyId(), credentials.getSecretAccessKey(), credentials.getSessionToken());
        }
    }
}
