package com.cleo.prototype.agent;

import com.cleo.prototype.entities.activation.AgentInfo;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.jboss.resteasy.client.jaxrs.ResteasyClient;
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import org.jboss.resteasy.client.jaxrs.ResteasyWebTarget;
import org.jboss.resteasy.client.jaxrs.engines.URLConnectionEngine;
import org.jboss.resteasy.plugins.providers.jackson.ResteasyJackson2Provider;

import java.util.List;
import java.util.Scanner;

import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.Link;
import javax.ws.rs.core.Response;

import static com.cleo.prototype.Constants.BASE_URL;

public class AgentCleaner {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    static {
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    private ResteasyClient client;

    public AgentCleaner() {
        ResteasyJackson2Provider resteasyJacksonProvider = new ResteasyJackson2Provider();
        resteasyJacksonProvider.setMapper(objectMapper);

        client = new ResteasyClientBuilder()
                .register(resteasyJacksonProvider)
                .httpEngine(new URLConnectionEngine())
                .connectionPoolSize(20)
                .build();
    }

    public void deleteAllAgents(String url) throws Exception {
        ResteasyWebTarget target = client.target(url);
        List<AgentInfo> agents = target.request().get(new GenericType<List<AgentInfo>>() {
        });
        for (AgentInfo agent : agents) {
            Link location = agent.getLink("delete");
            target = client.target(location);
            Response response = target.request().delete();
            if (response.getStatus() != 204) {
                System.out.println("Unable to delete agent at location: " + location + " status: " + response.getStatus());
            }
        }
    }

    public static void main(String[] args) throws Exception {
        Scanner reader = new Scanner(System.in);
        System.out.print("Enter SaaS URL: (ex: " + BASE_URL + ")> ");
        String saasUrl = reader.nextLine();
        if (saasUrl == null || saasUrl.trim().length() == 0) {
            saasUrl = BASE_URL;
        }

        AgentCleaner cleaner = new AgentCleaner();
        cleaner.deleteAllAgents(saasUrl + "/api/agent");
    }
}
