package com.cleo.prototype.iot;

import com.amazonaws.services.iot.client.AWSIotException;
import com.amazonaws.services.iot.client.AWSIotMqttClient;
import com.amazonaws.services.iot.client.AWSIotTimeoutException;
import com.cleo.prototype.entities.activation.AgentInfo;

import java.time.Duration;
import java.time.Instant;
import java.util.Timer;
import java.util.TimerTask;
import java.util.function.Function;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ResilientAWSIotMqttClient extends AWSIotMqttClient {
    private final AgentInfo agentInfo;
    private final Function<Void, AgentInfo.Credentials> credentialsProducer;
    private final Timer timer;

    public ResilientAWSIotMqttClient(AgentInfo agentInfo, Function<Void, AgentInfo.Credentials> credentialsProducer) {
        super(agentInfo.getIotEndpoint(), agentInfo.getAgentId(), agentInfo.getCredentials().getAccessKeyId(),
                agentInfo.getCredentials().getSecretAccessKey(),
                agentInfo.getCredentials().getSessionToken());
        this.agentInfo = agentInfo;
        this.credentialsProducer = credentialsProducer;
        this.timer = new Timer(true);
    }

    @Override
    public void connect() throws AWSIotException {
        super.connect();
        schedule();
    }

    @Override
    public void connect(long timeout) throws AWSIotException, AWSIotTimeoutException {
        super.connect(timeout);
        schedule();
    }

    @Override
    public void connect(long timeout, boolean blocking) throws AWSIotException, AWSIotTimeoutException {
        super.connect(timeout, blocking);
        schedule();
    }

    @Override
    public void disconnect() throws AWSIotException {
        super.disconnect();
        timer.cancel();
    }

    @Override
    public void disconnect(long timeout) throws AWSIotException, AWSIotTimeoutException {
        super.disconnect(timeout);
        timer.cancel();
    }

    @Override
    public void disconnect(long timeout, boolean blocking) throws AWSIotException, AWSIotTimeoutException {
        super.disconnect(timeout, blocking);
        timer.cancel();
    }

    @Override
    public void onConnectionSuccess() {
        super.onConnectionSuccess();
    }

    @Override
    public void onConnectionFailure() {
//        log.warn("Connection failure: {}={}", getConnection().getConnectCallback());
        super.onConnectionFailure();

    }

    @Override
    public void onConnectionClosed() {
        super.onConnectionClosed();
    }

    private void schedule() {
        AgentInfo.Credentials credentials = agentInfo.getCredentials();
        long nextRun = Duration.between(Instant.now().plusSeconds(-2 * 60), credentials.getExpiration().toInstant()).toMillis();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                AgentInfo.Credentials credentials = credentialsProducer.apply(null);
                ResilientAWSIotMqttClient.this.updateCredentials(credentials.getAccessKeyId(), credentials.getSecretAccessKey(), credentials.getSessionToken());
                long nextRun = Duration.between(Instant.now().plusSeconds(-2 * 60), credentials.getExpiration().toInstant()).toMillis();
                timer.schedule(this, nextRun);
            }
        }, nextRun);
    }
}
