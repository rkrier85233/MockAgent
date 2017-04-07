package com.cleo.prototype.entities.common;

import net.minidev.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

import lombok.Getter;

@Getter
public class AgentException extends Exception {
    private String level;
    private String reasonCode;
    private Integer httpStatus;
    private final Map<String, Object> args;

    public AgentException(String level, String reasonCode, String message) {
        super(message);
        this.level = level;
        this.reasonCode = reasonCode;
        this.args = new HashMap<>();
    }

    public AgentException(String level, String reasonCode, String message, Integer httpStatus) {
        super(message);
        this.level = level;
        this.reasonCode = reasonCode;
        this.httpStatus = httpStatus;
        this.args = new HashMap<>();
    }

    public AgentException addArgs(String key, Object value) {
        args.put(key, value);
        return this;
    }

    public JSONObject toJsonObject() {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("level", level);
        jsonObject.put("reasonCode", reasonCode);
        jsonObject.put("message", getMessage());
        jsonObject.put("args", args);
        if (httpStatus != null) {
            jsonObject.put("httpStatus", httpStatus);
        }
        return jsonObject;
    }
}
