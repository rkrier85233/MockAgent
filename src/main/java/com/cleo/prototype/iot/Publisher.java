package com.cleo.prototype.iot;

import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.services.iot.client.AWSIotException;
import com.amazonaws.services.iot.client.AWSIotMessage;
import com.amazonaws.services.iot.client.AWSIotMqttClient;
import com.amazonaws.services.iot.client.AWSIotQos;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class Publisher {
    public static void main(String[] args) throws AWSIotException {
        String clientEndpoint = "a2p50suclsf82w.iot.us-west-2.amazonaws.com";       // replace <prefix> and <region> with your own
        String clientId = "Publisher";
        String accessKeyId = DefaultAWSCredentialsProviderChain.getInstance().getCredentials().getAWSAccessKeyId();
        String secretKey = DefaultAWSCredentialsProviderChain.getInstance().getCredentials().getAWSSecretKey();

        AWSIotMqttClient client = new AWSIotMqttClient(clientEndpoint, clientId, accessKeyId, secretKey);
        client.connect();

        String topic = "my/own/topic";
        AWSIotQos qos = AWSIotQos.QOS0;
        String payload = "any payload";
        long timeout = 3000;                    // milliseconds

        MyMessage message = new MyMessage(topic, qos, payload);
        client.publish(message, timeout);
        client.disconnect();
    }

    public static class MyMessage extends AWSIotMessage {
        public MyMessage(String topic, AWSIotQos qos, String payload) {
            super(topic, qos, payload);
        }

        @Override
        public void onSuccess() {
            log.info("success");
        }

        @Override
        public void onFailure() {
            log.info("failure");
        }

        @Override
        public void onTimeout() {
            log.info("timout");
        }
    }
}
