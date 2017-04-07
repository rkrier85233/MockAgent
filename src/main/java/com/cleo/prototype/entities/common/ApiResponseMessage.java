package com.cleo.prototype.entities.common;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Singular;

//@Builder
@Getter
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ApiResponseMessage {
    public enum Level {
        INFO,
        WARN,
        ERROR,
        FATAL
    }

    private ApiResponseMessage.Level level;
    private String reasonCode;
    private Map<String, Object> args;
    private String message;
    private List<ApiResponseMessage> children;
    private Date timestamp;

    @Builder
    private ApiResponseMessage(Level level, String reasonCode, @Singular Map<String, Object> args, String message, Date timestamp) {
        this.level = level;
        this.reasonCode = reasonCode;
        this.args = args;
        this.message = message;
        this.children = new ArrayList<>();
        this.timestamp = timestamp == null ? new Date() : timestamp;
    }

    public ApiResponseMessage addChild(ApiResponseMessage child) {
        children.add(child);
        return this;
    }
}
