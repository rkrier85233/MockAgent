package com.cleo.prototype.dataflow.builder;

import com.cleo.prototype.entities.dataflow.DataFlow;
import com.cleo.prototype.entities.dataflow.Destination;
import com.cleo.prototype.entities.dataflow.FlowOperation;
import com.cleo.prototype.entities.dataflow.ItemMatch;
import com.cleo.prototype.entities.dataflow.Recurrence;
import com.cleo.prototype.entities.dataflow.Source;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.jboss.resteasy.client.jaxrs.ResteasyClient;
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import org.jboss.resteasy.client.jaxrs.ResteasyWebTarget;
import org.jboss.resteasy.client.jaxrs.engines.URLConnectionEngine;
import org.jboss.resteasy.plugins.providers.jackson.ResteasyJackson2Provider;

import java.net.URI;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;

import javax.ws.rs.client.Entity;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Link;
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
            Link location = dataFlow.getLink("delete");
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
        if (response.getStatus() != 201) {
            String body = response.readEntity(String.class);
            System.out.println("Unable to create data flow at status: " + response.getStatus());
            System.out.println(body);
            return null;
        }
        URI location = response.getLocation();

        target = client.target(location);
        dataFlow = target.request()
                .header(HttpHeaders.AUTHORIZATION, authHeader)
                .get(DataFlow.class);
        System.out.println("Added data flow ID: " + dataFlow.getId());
        return dataFlow;
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
        System.out.print("Enter source data store ID:> ");
        String sourceDatastoreId = reader.nextLine();
        System.out.print("Enter destination data store ID:> ");
        String destinationDatastoreId = reader.nextLine();
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
                .operation(FlowOperation.COPY)
                .build();

        Recurrence recurrence = Recurrence.builder()
                .interval(6)
                .timeUnit(TimeUnit.HOURS)
                .build();

        dataFlow.setRecurrence(recurrence);

        Source source = Source.builder()
                .datastoreId(sourceDatastoreId)
                .subPath("foo/bar")
                .itemMatch(ItemMatch.PATTERN_MATCH)
                .pattern("*.pdf")
                .build();

        dataFlow.setSource(source);

        Destination destination = Destination.builder()
                .datastoreId(destinationDatastoreId)
                .subPath("foo/bar")
                .build();

        dataFlow.setDestination(destination);


        dataFlow = builder.createDataFlow(saasUrl + "/api/dataflow", dataFlow);
//        String actionUrl = dataFlow.getLink("configure").getHref();
//        Response response = builder.doAction(actionUrl);
//        System.out.println("Configure action for data flow ID: " + dataFlow.getId() + " returned: " + response.getStatus());
        builder.client.close();
    }
}
