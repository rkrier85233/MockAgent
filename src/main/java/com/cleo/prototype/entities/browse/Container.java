package com.cleo.prototype.entities.browse;

import com.cleo.prototype.entities.common.JacksonConfig;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Container {
    @JsonIgnore
    private String fullPath;
    private String name;
    @JsonSerialize(using = JacksonConfig.DateSerializer.class)
    @JsonDeserialize(using = JacksonConfig.DateDeserializer.class)
    private Date modified;

    @JsonInclude(value = JsonInclude.Include.NON_EMPTY)
    private List<Container> containers = new ArrayList<>();

    @JsonInclude(value = JsonInclude.Include.NON_EMPTY)
    private List<ContainerItem> containerItems = new ArrayList<>();
}
