package com.cleo.prototype.entities.common;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.ArrayList;
import java.util.List;

import lombok.Getter;
import lombok.Setter;

public abstract class ResourceSupport {
    @Getter
    @Setter
    private List<Link> links = new ArrayList<>();

    @JsonIgnore
    public Link getLink(String rel) {
        return links.stream().filter(l -> l.getRel().equals(rel)).findFirst().orElse(null);
    }
}
