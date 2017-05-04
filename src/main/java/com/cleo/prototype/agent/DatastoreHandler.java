package com.cleo.prototype.agent;

import com.cleo.prototype.entities.common.AgentException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;

import java.io.File;
import java.io.IOException;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class DatastoreHandler {
    private static final ObjectMapper objectMapper = new ObjectMapper();

    static {
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    private static final JSONObject SUCCESS = new JSONObject();

    static {
        SUCCESS.put("result", "succeeded");
    }

    private enum EventType {
        DATASTORE_CREATE_EVENT,
        DATASTORE_READ_EVENT,
        DATASTORE_UPDATE_EVENT,
        DATASTORE_DELETE_EVENT,
        DATASTORE_LIST_EVENT
    }

    public static JSONObject handle(File datasourceDir, String messageType, JSONObject request) throws AgentException {
        EventType eventType = EventType.valueOf(messageType);
        String id = (String) request.get("id");
        File file = new File(datasourceDir, id + ".json");

        switch (eventType) {
            case DATASTORE_CREATE_EVENT:
                return doCreate(file, request);
            case DATASTORE_READ_EVENT:
                return doRead(file, request);
            case DATASTORE_UPDATE_EVENT:
                return doUpdate(file, request);
            case DATASTORE_DELETE_EVENT:
                return doDelete(file, request);
            case DATASTORE_LIST_EVENT:
                return doList(datasourceDir, request);
        }

        throw new AgentException("ERROR", "BAD_EVENT_TYPE", String.format("The event type '%s' is unknown.", messageType))
                .addArgs("type", "Datastore")
                .addArgs("messageType", messageType);
    }

    private static JSONObject doCreate(File file, JSONObject request) throws AgentException {
        if (file.exists()) {
            throw new AgentException("ERROR", "DUPLICATE_ENTITY", String.format("The datastore '%s' exists already.", request.get("id")))
                    .addArgs("type", "Datastore")
                    .addArgs("id", request.get("id"));
        }

        try {
            request.remove("links");
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(file, request);
            return SUCCESS;
        } catch (IOException e) {
            log.error("Unable to save to file: {}. cause: {}", file, e, e);
            throw new AgentException("ERROR", "UNABLE_TO_SAVE_DATASTORE", String.format("The datastore '%s' could not be saved.", request.get("id")))
                    .addArgs("type", "Datastore")
                    .addArgs("id", request.get("id"))
                    .addArgs("cause", e);

        }
    }

    private static JSONObject doRead(File file, JSONObject request) throws AgentException {
        if (!file.exists()) {
            throw new AgentException("ERROR", "ENTITY_NOT_FOUND", String.format("The datastore '%s' does not exist.", request.get("id")))
                    .addArgs("type", "Datastore")
                    .addArgs("id", request.get("id"));
        }

        try {
            return objectMapper.readValue(file, JSONObject.class);
        } catch (IOException e) {
            log.error("Unable to read to file: {}. cause: {}", file, e, e);
            throw new AgentException("ERROR", "UNABLE_TO_READ_DATASTORE", String.format("The datastore '%s' could not be read.", request.get("id")))
                    .addArgs("type", "Datastore")
                    .addArgs("id", request.get("id"))
                    .addArgs("cause", e);
        }
    }

    private static JSONObject doUpdate(File file, JSONObject request) throws AgentException {
        if (!file.exists()) {
            throw new AgentException("ERROR", "ENTITY_NOT_FOUND", String.format("The datastore '%s' does not exist.", request.get("id")))
                    .addArgs("type", "Datastore")
                    .addArgs("id", request.get("id"));
        }

        try {
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(file, request);
            return SUCCESS;
        } catch (IOException e) {
            log.error("Unable to write to file: {}. cause: {}", file, e, e);
            throw new AgentException("ERROR", "UNABLE_TO_READ_DATASTORE", String.format("The datastore '%s' could not be read.", request.get("id")))
                    .addArgs("type", "Datastore")
                    .addArgs("id", request.get("id"))
                    .addArgs("cause", e);
        }
    }

    private static JSONObject doDelete(File file, JSONObject request) throws AgentException {
        if (!file.exists()) {
            throw new AgentException("ERROR", "ENTITY_NOT_FOUND", String.format("The datastore '%s' does not exist.", request.get("id")))
                    .addArgs("type", "Datastore")
                    .addArgs("id", request.get("id"));
        }

        file.delete();
        return SUCCESS;
    }

    // Not implemented
    private static JSONObject doList(File datastoreDir, JSONObject request) throws AgentException {
        JSONObject response = new JSONObject();
        JSONArray jsonArray = new JSONArray();
        response.put("list", jsonArray);
        File[] files = datastoreDir.listFiles();
        if (files == null) {
            return response;
        }

        try {
            for (File file : files) {
                jsonArray.add(objectMapper.readValue(file, JSONObject.class));
            }
        } catch (IOException e) {
            log.error("Unable to list files. cause: {}", e, e);
            throw new AgentException("ERROR", "UNABLE_TO_LIST_DATASTORE", "Unable to list datastores.")
                    .addArgs("type", "Datastore")
                    .addArgs("cause", e);
        }
        return response;
    }
}
