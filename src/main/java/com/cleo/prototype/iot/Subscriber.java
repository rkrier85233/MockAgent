package com.cleo.prototype.iot;

import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.services.iot.client.AWSIotDeviceErrorCode;
import com.amazonaws.services.iot.client.AWSIotException;
import com.amazonaws.services.iot.client.AWSIotMessage;
import com.amazonaws.services.iot.client.AWSIotMqttClient;
import com.amazonaws.services.iot.client.AWSIotQos;
import com.amazonaws.services.iot.client.AWSIotTopic;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class Subscriber {
    public static void main(String[] args) throws AWSIotException {
        String clientEndpoint = "a2p50suclsf82w.iot.us-west-2.amazonaws.com";       // replace <prefix> and <region> with your own
        String clientId = "Subscriber";
        String accessKeyId = DefaultAWSCredentialsProviderChain.getInstance().getCredentials().getAWSAccessKeyId();
        String secretKey = DefaultAWSCredentialsProviderChain.getInstance().getCredentials().getAWSSecretKey();

        AWSIotMqttClient client = new AWSIotMqttClient(clientEndpoint, clientId, accessKeyId, secretKey);
        client.connect();

        String topic = "my/own/topic";
        AWSIotQos qos = AWSIotQos.QOS0;
        // milliseconds

        MyTopic myTopic = new MyTopic(topic, qos);
        client.subscribe(myTopic);
    }

    public static class MyTopic extends AWSIotTopic {
        public MyTopic(String topic, AWSIotQos qos) {
            super(topic, qos);
        }

        @Override
        public void onMessage(AWSIotMessage message) {
            log.info(message.getStringPayload());
        }

        @Override
        public AWSIotDeviceErrorCode getErrorCode() {
            return super.getErrorCode();
        }
    }
}
