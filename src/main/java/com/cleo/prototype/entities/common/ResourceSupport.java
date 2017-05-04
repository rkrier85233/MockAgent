package com.cleo.prototype.entities.common;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.ArrayList;
import java.util.List;

import lombok.Getter;
import lombok.Setter;

public abstract class ResourceSupport {
    @Getter
    @Setter
    @JsonInclude(value = JsonInclude.Include.NON_EMPTY)
    private List<Link> links = new ArrayList<>();

    @JsonIgnore
    public Link getLink(String rel) {
        return links.stream().filter(l -> l.getRel().equals(rel)).findFirst().orElse(null);
    }
}
