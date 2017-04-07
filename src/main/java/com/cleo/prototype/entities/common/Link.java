package com.cleo.prototype.entities.common;

import net.minidev.json.JSONObject;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import lombok.Getter;

@Getter
public class Link {
    private String rel;
    private String href;

    public Link() {
    }

    public Link(String rel, String href) {
        this.rel = rel;
        this.href = href;
    }

    public static Map<String, Link> parseLinks(JSONObject jsonObject) {
        List<Map<String, String>> links = (List<Map<String, String>>) jsonObject.get("links");
        HashMap<String, Link> map = new HashMap<>();
        links.forEach(m -> {
            map.put(m.get("rel"), new Link(m.get("rel"), m.get("href")));
        });
        return map;
    }

    public static Link getLink(JSONObject jsonObject, String rel) {
        Map<String, Link> links = parseLinks(jsonObject);
        return links.get(rel);
    }
}
