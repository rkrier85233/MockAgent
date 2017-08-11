package com.cleo.prototype.agent;

import com.amazonaws.AbortedException;
import com.amazonaws.auth.BasicSessionCredentials;
import com.amazonaws.regions.RegionUtils;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSClient;
import com.amazonaws.services.sqs.model.AmazonSQSException;
import com.amazonaws.services.sqs.model.DeleteMessageRequest;
import com.amazonaws.services.sqs.model.Message;
import com.amazonaws.services.sqs.model.MessageAttributeValue;
import com.amazonaws.services.sqs.model.ReceiveMessageRequest;
import com.cleo.prototype.IotJobScheduler;
import com.cleo.prototype.JobScheduler;
import com.cleo.prototype.entities.activation.ActivationRequest;
import com.cleo.prototype.entities.activation.AgentInfo;
import com.cleo.prototype.entities.browse.ResourceBrowseRequest;
import com.cleo.prototype.entities.browse.ResourceBrowseResponse;
import com.cleo.prototype.entities.common.AgentException;
import com.cleo.prototype.entities.event.DataFlowEvent;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;

import net.minidev.json.JSONObject;

import org.jboss.resteasy.client.jaxrs.ResteasyClient;
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import org.jboss.resteasy.client.jaxrs.ResteasyWebTarget;
import org.jboss.resteasy.client.jaxrs.engines.URLConnectionEngine;
import org.jboss.resteasy.plugins.providers.jackson.ResteasyJackson2Provider;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.List;

import javax.ws.rs.client.Entity;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Link;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import lombok.extern.slf4j.Slf4j;

import static com.cleo.prototype.Constants.AUTH_TOKEN;
import static com.cleo.prototype.agent.CertificateGenerator.generatePemCer;
import static com.cleo.prototype.entities.common.LinkUtil.getLink;

@Slf4j
public class MockAgent {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    static {
        OBJECT_MAPPER.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    private File rootDir;
    private File datastoreDir;
    private File dataflowsDir;
    private MessageHandler messageHandler;
    private JobScheduler jobScheduler;
    private HeartbeatPublisher heartbeatPublisher;

    public AgentInfo getMyInfo(String baseUrl, String name) throws Exception {
        ResteasyClient client = newResteasyClient();

        try {
            rootDir = toFile(baseUrl);
            rootDir.mkdirs();
            File file = new File(rootDir, name + ".json");
            AgentInfo agentInfo;
            if (file.exists()) {
                agentInfo = OBJECT_MAPPER.readValue(file, AgentInfo.class);
                Link selfLink = agentInfo.getLink("self");
                Response response = client.target(selfLink).request().get();
                if (response.getStatusInfo().equals(Response.Status.NOT_FOUND)) {
                    agentInfo = activate(client, baseUrl, name);
                } else {
                    agentInfo = response.readEntity(AgentInfo.class);
                }
            } else {
                agentInfo = activate(client, baseUrl, name);
            }
            OBJECT_MAPPER.writerWithDefaultPrettyPrinter().writeValue(file, agentInfo);

            datastoreDir = new File(rootDir, agentInfo.getAgentId() + "/datasource");
            datastoreDir.mkdirs();
            dataflowsDir = new File(rootDir, agentInfo.getAgentId() + "/dataflow");
            dataflowsDir.mkdirs();
            jobScheduler = new IotJobScheduler(agentInfo);
            jobScheduler.start(dataflowsDir);
            messageHandler = new MessageHandler(agentInfo);
            messageHandler.start();
            heartbeatPublisher = new HeartbeatPublisher(agentInfo);
            heartbeatPublisher.start();
            return agentInfo;
        } finally {
            client.close();
        }
    }

    private AgentInfo activate(ResteasyClient client, String baseUrl, String name) throws Exception {
        String url = baseUrl + "/api/accesspoints";
        JSONObject requestBody = new JSONObject();
        requestBody.put("name", name);
        requestBody.put("platform", "RED_HAT");

        String authHeader = "Bearer " + AUTH_TOKEN;
        Response response = client.target(url).request()
                .header(HttpHeaders.AUTHORIZATION, authHeader)
                .post(Entity.entity(requestBody, MediaType.APPLICATION_JSON_TYPE));
        if (!response.getStatusInfo().equals(Response.Status.CREATED)) {
            throw new Exception("Response from activation registration was: " + response.getStatusInfo().getStatusCode() + "\n" + response.readEntity(String.class));
        }

        JSONObject responseBody = response.readEntity(JSONObject.class);
        String activationCode = (String) responseBody.get("activationCode");
        String activationTokenUrl = new String(Base64.getDecoder().decode(activationCode), "UTF-8");
        response = client.target(activationTokenUrl).request().get();
        if (!response.getStatusInfo().equals(Response.Status.OK)) {
            throw new Exception("Response from get token was: " + response.getStatusInfo().getStatusCode() + "\n" + response.readEntity(String.class));
        }
        String token = response.readEntity(String.class);

        ActivationRequest activationRequest = ActivationRequest.builder()
                .name(name)
                .activationToken(token)
                .publicCertData(generatePemCer())
                .build();

        SignedJWT signedJWT = SignedJWT.parse(token);
        JWTClaimsSet claimsSet = signedJWT.getJWTClaimsSet();
//        String saasBaseUrl = claimsSet.getStringClaim("saas_url");
        String saasActivationUrl = claimsSet.getStringClaim("saas_activation");

        response = client.target(saasActivationUrl).request().post(Entity.entity(activationRequest, MediaType.APPLICATION_JSON_TYPE));
        if (!response.getStatusInfo().equals(Response.Status.OK)) {
            throw new Exception("Response from get activation was: " + response.getStatusInfo().getStatusCode() + "\n" + response.readEntity(String.class));
        }

        return response.readEntity(AgentInfo.class);
    }


    public void processEvents(AgentInfo agentInfo) {
        log.info("Expiration is: {}", agentInfo.getCredentials().getExpiration());
        final String agentId = agentInfo.getAgentId();
        final String queueUrl = agentInfo.getLink("queue").getUri().toString();
        for (; ; ) {
            ReceiveMessageRequest receiveMessageRequest = new ReceiveMessageRequest(queueUrl)
                    .withWaitTimeSeconds(20)
                    .withMaxNumberOfMessages(10);
            AmazonSQS sqs = newAmazonSQS(agentInfo);
            try {
                List<Message> messages = sqs.receiveMessage(receiveMessageRequest.withMessageAttributeNames("All")).getMessages();
                for (Message message : messages) {
                    String type = message.getMessageAttributes().get("message-type").getStringValue();
                    MessageAttributeValue mav = message.getMessageAttributes().get("expiration");
                    if (mav != null) {
                        if (Instant.parse(mav.getStringValue()).isBefore(Instant.now())) {
                            log.info("Ignoring message ID: {} because it expired at: {}.", type, mav.getStringValue());
                            String messageReceiptHandle = message.getReceiptHandle();
                            sqs.deleteMessage(new DeleteMessageRequest(queueUrl, messageReceiptHandle));
                            continue;
                        }
                    }

                    if ("DATA_FLOW_EVENT".equalsIgnoreCase(type)) {
                        handleDataFlowEvent(sqs, queueUrl, message, agentId);
                    } else if ("RESOURCE_BROWSE_EVENT".equalsIgnoreCase(type)) {
                        handleRemoteBrowse(sqs, queueUrl, message);
                    } else if (type.startsWith("AGENT_DELETE_EVENT")) {
                        handleDeleteEvent(sqs, queueUrl, message);
                    } else if (type.startsWith("DATASTORE")) {
                        handleDatastoreEvent(agentId, sqs, queueUrl, message);
                    } else {
                        log.warn("Unexpected message type: {} received from SQS queue.", type);
                    }
                }
            } catch (AbortedException e) {
                log.info("Stopping event processing.");
                return;
            } catch (AmazonSQSException e) {
                log.error("Got an expired token, expiration is: {}", agentInfo.getCredentials().getExpiration(), e);
                return;
            } catch (Exception e) {
                e.printStackTrace();
                return;
            }
        }
    }

    private void handleDataFlowEvent(AmazonSQS sqs, String queueUrl, Message message, String agentId) throws IOException {
        JSONObject event = OBJECT_MAPPER.readValue(message.getBody(), JSONObject.class);
        String id = (String) event.get("id");
        String action = (String) event.get("action");
        log.info("Received: " + action + " action, flow ID: " + id + ".");
        String messageReceiptHandle = message.getReceiptHandle();
        sqs.deleteMessage(new DeleteMessageRequest(queueUrl, messageReceiptHandle));

        if (action.equalsIgnoreCase("CONFIGURE")) {
            File file = new File(dataflowsDir, id + ".json");
            OBJECT_MAPPER.writerWithDefaultPrettyPrinter().writeValue(file, event);
            DataFlowEvent dataFlowEvent = OBJECT_MAPPER.readValue(message.getBody(), DataFlowEvent.class);
            jobScheduler.schedule(dataFlowEvent);
        } else if ("CONFIGURE_AND_RUN".equalsIgnoreCase(action)) {
            File file = new File(dataflowsDir, id + ".json");
            OBJECT_MAPPER.writerWithDefaultPrettyPrinter().writeValue(file, event);
            DataFlowEvent dataFlowEvent = OBJECT_MAPPER.readValue(message.getBody(), DataFlowEvent.class);
            jobScheduler.runNow(dataFlowEvent);
        } else if ("DELETE".equalsIgnoreCase(action)) {
            File file = new File(dataflowsDir, id + ".json");
            if (file.exists()) {
                DataFlowEvent dataFlowEvent = OBJECT_MAPPER.readValue(file, DataFlowEvent.class);
                jobScheduler.delete(dataFlowEvent);
                file.delete();
            }
        }
    }

    private void handleRemoteBrowse(AmazonSQS sqs, String queueUrl, Message message) throws IOException {
        log.info("Received: remote browse request.");

        String messageId = message.getMessageAttributes().get("message-id").getStringValue();
        String messageReceiptHandle = message.getReceiptHandle();
        sqs.deleteMessage(new DeleteMessageRequest(queueUrl, messageReceiptHandle));

        ResourceBrowseRequest request = OBJECT_MAPPER.readValue(message.getBody(), ResourceBrowseRequest.class);
        final URI responseUrl = request.getLink("response").getUri();
        Entity<?> entity;
        try {
            final ResourceBrowseResponse response = ResourceBrowseHandler.build(request);
            entity = Entity.entity(response, MediaType.APPLICATION_JSON_TYPE);
        } catch (AgentException e) {
            log.warn(e.getLevel(), e);
            entity = Entity.entity(e.toJsonObject(), MediaType.APPLICATION_JSON_TYPE);
        }
        ResteasyClient client = newResteasyClient();
        ResteasyWebTarget target = client.target(responseUrl);
        try {
            Response webResponse = target.request().header("RefToMessageId", messageId).post(entity);
            if (webResponse.getStatusInfo() != Response.Status.NO_CONTENT) {
                throw new RuntimeException("Response was not a 204, response: " + webResponse.getStatusInfo());
            }
            log.info("Responded to remote browse request.");
        } finally {
            client.close();
        }
    }

    private void handleDatastoreEvent(String agentId, AmazonSQS sqs, String queueUrl, Message message) throws IOException {
        log.info("Received: datastore request.");

        String type = message.getMessageAttributes().get("message-type").getStringValue();
        String messageId = message.getMessageAttributes().get("message-id").getStringValue();
        String messageReceiptHandle = message.getReceiptHandle();
        sqs.deleteMessage(new DeleteMessageRequest(queueUrl, messageReceiptHandle));

        JSONObject request = OBJECT_MAPPER.readValue(message.getBody(), JSONObject.class);
        final String responseUrl = getLink(request, "response");
        Entity<?> entity;
        try {
            final JSONObject response = DatastoreHandler.handle(datastoreDir, type, request);
            entity = Entity.entity(response, MediaType.APPLICATION_JSON_TYPE);
        } catch (AgentException e) {
            log.warn(e.getLevel(), e);
            entity = Entity.entity(e.toJsonObject(), MediaType.APPLICATION_JSON_TYPE);
        }
        ResteasyClient client = newResteasyClient();
        ResteasyWebTarget target = client.target(responseUrl);
        try {
            Response webResponse = target.request().header("RefToMessageId", messageId).post(entity);
            if (webResponse.getStatusInfo() != Response.Status.NO_CONTENT) {
                throw new RuntimeException("Response was not a 204, response: " + webResponse.getStatusInfo());
            }
            log.info("Responded to datastore request.");
        } finally {
            client.close();
        }
    }

    private void handleDeleteEvent(AmazonSQS sqs, String queueUrl, Message message) throws IOException {
        log.info("Received: delete request.");

        String type = message.getMessageAttributes().get("message-type").getStringValue();
        String messageId = message.getMessageAttributes().get("message-id").getStringValue();
        String messageReceiptHandle = message.getReceiptHandle();
        sqs.deleteMessage(new DeleteMessageRequest(queueUrl, messageReceiptHandle));

        JSONObject request = OBJECT_MAPPER.readValue(message.getBody(), JSONObject.class);
        final String responseUrl = getLink(request, "response");
        ResteasyClient client = newResteasyClient();
        ResteasyWebTarget target = client.target(responseUrl);
        try {
            Response webResponse = target.request().header("RefToMessageId", messageId).post(null);
            if (webResponse.getStatusInfo() != Response.Status.NO_CONTENT) {
                throw new RuntimeException("Response was not a 204, response: " + webResponse.getStatusInfo());
            }
            log.info("Responded to delete request.");
            System.exit(0);
        } finally {
            client.close();
        }
    }

    private AmazonSQS newAmazonSQS(AgentInfo agentInfo) {
        AgentInfo.Credentials credentials = agentInfo.getCredentials();

        final Date twoMinutesFromNow = new Date(System.currentTimeMillis() + (2 * 60 * 1000));
        log.info("Credentials expiration: {}, two minutes from now: {}", credentials.getExpiration(), twoMinutesFromNow);
        if (credentials.getExpiration().before(twoMinutesFromNow)) {
            // Token will expire in two minutes or less, time to renew.
            log.info("Token expiring... renewing token.");
            credentials = renewCredentials(agentInfo);
            agentInfo.setCredentials(credentials);
            log.info("Renewed Expiration is: {}", credentials.getExpiration());
        }
        AgentInfo.Credentials creds = agentInfo.getCredentials();
        BasicSessionCredentials sessionCredentials = new BasicSessionCredentials(
                creds.getAccessKeyId(),
                creds.getSecretAccessKey(),
                creds.getSessionToken());

        AmazonSQS sqs = new AmazonSQSClient(sessionCredentials);
        if (agentInfo.getAwsRegion() != null) {
            sqs.setRegion(RegionUtils.getRegion(agentInfo.getAwsRegion()));
        }
        return sqs;
    }

    public static AgentInfo.Credentials renewCredentials(AgentInfo agentInfo) {
        Link renew = agentInfo.getCredentials().getLink("renew");
        ResteasyClient client = newResteasyClient();
        ResteasyWebTarget target = client.target(renew);

        try {
            return target.request().get(AgentInfo.Credentials.class);
        } finally {
            client.close();
        }
    }

    public static ResteasyClient newResteasyClient() {
        ResteasyJackson2Provider resteasyJacksonProvider = new ResteasyJackson2Provider();
        resteasyJacksonProvider.setMapper(OBJECT_MAPPER);

        return new ResteasyClientBuilder()
                .register(resteasyJacksonProvider)
                .httpEngine(new URLConnectionEngine())
                .build();
    }

    private File toFile(String baseUrl) {
        URI uri = URI.create(baseUrl);
        String host = uri.getHost();
        int port = uri.getPort() == -1 ? ("http".equalsIgnoreCase(uri.getScheme()) ? 80 : 443) : uri.getPort();
        String dirName = host + "-" + port;
        return new File("./data", dirName);
    }
}
