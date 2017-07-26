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

import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class HeartbeatPublisher {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final AgentInfo agentInfo;
    private AWSIotMqttClient client;
    private Timer timer;

    public HeartbeatPublisher(AgentInfo agentInfo) {
        this.agentInfo = agentInfo;
    }

    public void start() {
        final String topic = agentInfo.getLink("heartbeatTopic").getUri().toString();
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

        timer = new Timer(true);
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                try {
                    log.debug("Publishing heartbeat.");
                    Map<String, Object> map = new HashMap<>();
                    map.put("subscriptions", client.getSubscriptions().keySet());
                    String msg = OBJECT_MAPPER.writeValueAsString(map);
                    client.publish(new AWSIotMessage(topic, AWSIotQos.QOS0, msg));
                    log.debug("Finished publishing heartbeat, payload: {}", msg);
                } catch (AWSIotException e) {
                    log.debug("Unuable to publish heartbeat, cause: ", e, e);
                } catch (JsonProcessingException e) {
                    log.error("Unable to serialize message.");
                }
            }
        }, 0, (10 * 1000));
    }
}
