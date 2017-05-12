package com.cleo.prototype.entities.common;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import java.util.ArrayList;
import java.util.List;

import lombok.Getter;
import lombok.Setter;

public abstract class ResourceSupport {
    @Getter
    @Setter
    @JsonInclude(value = JsonInclude.Include.NON_EMPTY)
    @JsonDeserialize(contentUsing = JacksonConfig.LinkDeserializer.class)
    @JsonSerialize(contentUsing = JacksonConfig.LinkSerializer.class)
    private List<javax.ws.rs.core.Link> links = new ArrayList<>();

    public void add(javax.ws.rs.core.Link link) {
        links.add(link);
    }

    public javax.ws.rs.core.Link getLink(String rel) {
        return links.stream().filter(o -> o.getRel().equals(rel)).findFirst().orElse(null);
    }
}
