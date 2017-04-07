package com.cleo.prototype.entities.dataflow;

import com.cleo.prototype.entities.common.ResourceSupport;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DataFlow extends ResourceSupport {
    private String id;
    private String name;
    private String description;
    private FlowOperation operation;

    public DataFlow() {
    }

    @Builder
    public DataFlow(String name, String description, FlowOperation operation) {
        this.name = name;
        this.description = description;
        this.operation = operation;
    }

    public enum FlowOperation {
        MOVE("move"),
        COPY("copy"),
        REPLICATE("replicate"),
        SYNCHRONIZE("synchronize"),
        DELETE("delete");

        @Getter
        private String externalName;

        FlowOperation() {
            this.externalName = name();
        }

        FlowOperation(String externalName) {
            this.externalName = externalName;
        }

        @Override
        @JsonValue
        public String toString() {
            return externalName;
        }

        @JsonCreator
        public static FlowOperation fromName(String name) {
            try {
                return Enum.valueOf(FlowOperation.class, name);
            } catch (IllegalArgumentException e) {
                for (FlowOperation operation : values()) {
                    if (operation.externalName.equals(name)) {
                        return operation;
                    }
                }
            }
            throw new IllegalArgumentException(
                    "No enum constant " + FlowOperation.class.getCanonicalName() + "." + name);
        }
    }
}
