package com.cleo.prototype.agent;

import com.amazonaws.services.iot.client.AWSIotConnectionStatus;
import com.amazonaws.services.iot.client.AWSIotException;
import com.amazonaws.services.iot.client.AWSIotMessage;
import com.amazonaws.services.iot.client.AWSIotMqttClient;
import com.amazonaws.services.iot.client.AWSIotQos;
import com.amazonaws.services.iot.client.AWSIotTopic;
import com.cleo.prototype.entities.activation.AgentInfo;
import com.cleo.prototype.entities.common.ApiResponseMessage;
import com.cleo.prototype.iot.AWSIotMqttClientFactory;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.Map;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class MessageHandler {
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final AgentInfo agentInfo;
    private AWSIotMqttClient client;

    public MessageHandler(AgentInfo agentInfo) {
        this.agentInfo = agentInfo;
    }

    public void start() {
        final String topic = agentInfo.getLink("messageTopic").getUri().toString();
        client = AWSIotMqttClientFactory.getClient(agentInfo);
        if (client.getConnectionStatus() != AWSIotConnectionStatus.CONNECTED) {
            log.info("Connecting");
            try {
                client.connect();
            } catch (AWSIotException e) {
                log.error("Unable to connect, cause: {}", e, e);
                return;
            }
        }

        log.info("Subscribing to topic: {}", topic);
        try {
            client.subscribe(new AWSIotTopic(topic, AWSIotQos.QOS0) {
                @Override
                public void onFailure() {
                    log.error("On failure: {}-{}", getErrorCode(), getErrorMessage());
                }

                @Override
                public void onMessage(AWSIotMessage message) {
                    String topic = message.getTopic();
                    log.info("Received message on topic: {}", topic);
                    if (topic.endsWith("/dataflow/test")) {
                        log.info("Handling test message.");
                        Map map = null;
                        try {
                            map = objectMapper.readValue(message.getStringPayload(), Map.class);
                        } catch (IOException e) {
                            log.error("Unable to deserialize request message, cause: {}", e, e);
                            return;
                        }
                        try {
                            String responseTopic = (String) map.get("responseTopic");
                            log.info("Responding on topic: {}", responseTopic);
                            ApiResponseMessage responseMessage = ApiResponseMessage.builder()
                                    .level(ApiResponseMessage.Level.INFO)
                                    .reasonCode("GOOD_TEST")
                                    .message("Test succeeded")
                                    .build();

                            String payload = objectMapper.writeValueAsString(responseMessage);
                            AWSIotMessage publishMessage = new AWSIotMessage(responseTopic, AWSIotQos.QOS0, payload);
                            client.publish(publishMessage);
                            log.info("Responded on topic: {}", responseTopic);
                        } catch (JsonProcessingException e) {
                            log.error("Unable to serialize response message, cause: {}", e, e);
                        } catch (Exception e) {
                            log.error("Unable to publish response message, cause: {}", e, e);
                        }
                    }
                }
            });
        } catch (AWSIotException e) {
            log.error("Unable to subscribe to topic {}, cause: {}", topic, e, e);
        }
    }


}
