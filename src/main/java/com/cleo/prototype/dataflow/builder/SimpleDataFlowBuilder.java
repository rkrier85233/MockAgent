package com.cleo.prototype.dataflow.builder;

import com.cleo.prototype.entities.dataflow.DataFlow;
import com.cleo.prototype.entities.dataflow.Destination;
import com.cleo.prototype.entities.dataflow.Endpoint;
import com.cleo.prototype.entities.dataflow.Source;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.jboss.resteasy.client.jaxrs.ResteasyClient;
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import org.jboss.resteasy.client.jaxrs.ResteasyWebTarget;
import org.jboss.resteasy.client.jaxrs.engines.URLConnectionEngine;
import org.jboss.resteasy.plugins.providers.jackson.ResteasyJackson2Provider;

import java.util.List;
import java.util.Scanner;

import javax.ws.rs.client.Entity;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import static com.cleo.prototype.Constants.AUTH_TOKEN;
import static com.cleo.prototype.Constants.BASE_URL;

public class SimpleDataFlowBuilder {
    private static final ObjectMapper objectMapper = new ObjectMapper();

    static {
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    private ResteasyClient client;
    private String authHeader = "Bearer " + AUTH_TOKEN;

    public SimpleDataFlowBuilder() {
        ResteasyJackson2Provider resteasyJacksonProvider = new ResteasyJackson2Provider();
        resteasyJacksonProvider.setMapper(objectMapper);

        client = new ResteasyClientBuilder()
                .httpEngine(new URLConnectionEngine())
                .register(resteasyJacksonProvider)
                .connectionPoolSize(20)
                .build();
    }

    public void removeAllDataFlows(String url) {
        ResteasyWebTarget target = client.target(url);
        List<DataFlow> dataFlows = target.request()
                .header(HttpHeaders.AUTHORIZATION, authHeader)
                .get(new GenericType<List<DataFlow>>() {
                });
        for (DataFlow dataFlow : dataFlows) {
            String location = dataFlow.getLink("delete").getHref();
            target = client.target(location);
            Response response = target.request()
                    .header(HttpHeaders.AUTHORIZATION, authHeader)
                    .delete();
            if (response.getStatus() != 204) {
                System.out.println("Unable to delete data flow at location: " + location + " status: " + response.getStatus());
            }
        }
    }

    public DataFlow createDataFlow(String url, DataFlow dataFlow) {
        ResteasyWebTarget target = client.target(url);

        Entity<DataFlow> entity = Entity.entity(dataFlow, MediaType.APPLICATION_JSON_TYPE);

        Response response = target.request()
                .header(HttpHeaders.AUTHORIZATION, authHeader)
                .post(entity);
        String location = response.getHeaderString("location");

        target = client.target(location);
        return target.request()
                .header(HttpHeaders.AUTHORIZATION, authHeader)
                .get(DataFlow.class);
    }

    public Source addSource(DataFlow dataFlow, Source source) {
        String url = dataFlow.getLink("sources").getHref();
        ResteasyWebTarget target = client.target(url);

        Entity<Source> entity = Entity.entity(source, MediaType.APPLICATION_JSON_TYPE);
        Response response = target.request()
                .header(HttpHeaders.AUTHORIZATION, authHeader)
                .post(entity);
        String location = response.getHeaderString("location");

        target = client.target(location);
        return target.request()
                .header(HttpHeaders.AUTHORIZATION, authHeader)
                .get(Source.class);
    }

    public Endpoint addSourceEndpoint(Source source, Endpoint endpoint) {
        String url = source.getLink("endpoints").getHref();
        ResteasyWebTarget target = client.target(url);

        Entity<Endpoint> entity = Entity.entity(endpoint, MediaType.APPLICATION_JSON_TYPE);
        Response response = target.request()
                .header(HttpHeaders.AUTHORIZATION, authHeader)
                .post(entity);
        String location = response.getHeaderString("location");

        target = client.target(location);
        return target.request()
                .header(HttpHeaders.AUTHORIZATION, authHeader)
                .get(Endpoint.class);
    }

    public Destination addDestination(DataFlow dataFlow, Destination destination) {
        String url = dataFlow.getLink("destinations").getHref();
        ResteasyWebTarget target = client.target(url);

        Entity<Destination> entity = Entity.entity(destination, MediaType.APPLICATION_JSON_TYPE);
        Response response = target.request()
                .header(HttpHeaders.AUTHORIZATION, authHeader)
                .post(entity);
        String location = response.getHeaderString("location");

        target = client.target(location);
        return target.request()
                .header(HttpHeaders.AUTHORIZATION, authHeader)
                .get(Destination.class);
    }

    public Endpoint addDestinationEndpoint(Destination destination, Endpoint endpoint) {
        String url = destination.getLink("endpoints").getHref();
        ResteasyWebTarget target = client.target(url);

        Entity<Endpoint> entity = Entity.entity(endpoint, MediaType.APPLICATION_JSON_TYPE);
        Response response = target.request()
                .header(HttpHeaders.AUTHORIZATION, authHeader)
                .post(entity);
        String location = response.getHeaderString("location");

        target = client.target(location);
        return target.request()
                .header(HttpHeaders.AUTHORIZATION, authHeader)
                .get(Endpoint.class);
    }

    public Response doAction(String url) {
        ResteasyWebTarget target = client.target(url);
        return target.request()
                .header(HttpHeaders.AUTHORIZATION, authHeader)
                .method("PUT");
    }

//    public void addThePlayers(DataFlow dataFlow, Source source, Destination destination) {
//        String url = dataFlow.getLink("sources").getHref();
//        ResteasyWebTarget target = client.target(url);
//
//        Entity<Source> sourceEntity = Entity.entity(source, MediaType.APPLICATION_JSON_TYPE);
//        Response response = target.request().post(sourceEntity);
//        String sLoc = response.getHeaderString("location");
//
//        url = dataFlow.getLink("destinations").getHref();
//        target = client.target(url);
//
//        Entity<Destination> destinationEntity = Entity.entity(destination, MediaType.APPLICATION_JSON_TYPE);
//        response = target.request().post(destinationEntity);
//        String dLoc = response.getHeaderString("location");
//
//        target = client.target(sLoc);
//        Source sPersisted = target.request().get(Source.class);
//        source.setAgentId(sPersisted.getAgentId());
//        source.setLinks(sPersisted.getLinks());
//
//        target = client.target(dLoc);
//        Destination dPersisted = target.request().get(Destination.class);
//        destination.setAgentId(dPersisted.getAgentId());
//        destination.setTransport(dPersisted.getTransport());
//        destination.setLinks(dPersisted.getLinks());
//    }

    public static void main(String[] args) {
        Scanner reader = new Scanner(System.in);
        System.out.print("Enter SaaS URL: (ex: " + BASE_URL + ")> ");
        String saasUrl = reader.nextLine();
        if (saasUrl == null || saasUrl.trim().length() == 0) {
            saasUrl = BASE_URL;
        }
        System.out.print("Enter source agent ID:> ");
        String sourceAgentId = reader.nextLine();
        System.out.print("Enter destination agent ID:> ");
        String destinationId = reader.nextLine();
        System.out.print("Enter data flow name:> ");
        String dataFlowName = reader.nextLine();
        System.out.print("Clean old data flows (Y/n):> ");
        String clean = reader.nextLine();

        SimpleDataFlowBuilder builder = new SimpleDataFlowBuilder();
        if (clean.startsWith("Y")) {
            builder.removeAllDataFlows(saasUrl + "/api/dataflow");
            System.out.println("Removed old data flows.");
        }

        if (dataFlowName == null || dataFlowName.trim().length() == 0) {
            dataFlowName = "Mock Data Flow";
        }
        DataFlow dataFlow = DataFlow.builder()
                .name(dataFlowName)
                .description("This is a mock data flow.")
                .operation(DataFlow.FlowOperation.COPY)
                .build();

        dataFlow = builder.createDataFlow(saasUrl + "/api/dataflow", dataFlow);
        System.out.println("Added data flow ID: " + dataFlow.getId());

        Source source = Source.builder()
                .accessPointId(sourceAgentId)
                .build();

        source = builder.addSource(dataFlow, source);
        System.out.println("Added source agent to data flow ID: " + dataFlow.getId());

        Destination destination = Destination.builder()
                .accessPointId(destinationId)
                .build();

        destination = builder.addDestination(dataFlow, destination);
        System.out.println("Added destination agent to data flow ID: " + dataFlow.getId());

        Endpoint sourceEndpoint = Endpoint.builder()
                .uri("file:///foo/bar")
                .config("config-1")
                .build();

        sourceEndpoint = builder.addSourceEndpoint(source, sourceEndpoint);
        System.out.println("Added source endpoint to data flow ID: " + dataFlow.getId());

        Endpoint destinationEndpoint = Endpoint.builder()
                .uri("file:///remote/dir/rec")
                .config("config-2")
                .build();

        destinationEndpoint = builder.addDestinationEndpoint(destination, destinationEndpoint);
        System.out.println("Added destination endpoint to data flow ID: " + dataFlow.getId());

        String actionUrl = dataFlow.getLink("configure").getHref();
        Response response = builder.doAction(actionUrl);
        System.out.println("Configure action for data flow ID: " + dataFlow.getId() + " returned: " + response.getStatus());
        builder.client.close();
    }
}
