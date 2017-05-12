package com.cleo.prototype.entities.common;

import net.minidev.json.JSONObject;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Getter
public class LinkUtil {

    public static Map<String, String> parseLinks(JSONObject jsonObject) {
        List<Map<String, String>> links = (List<Map<String, String>>) jsonObject.get("links");
        HashMap<String, String> map = new HashMap<>();
        links.forEach(m -> {
            map.put(m.get("rel"), m.get("href"));
        });
        return map;
    }

    public static String getLink(JSONObject jsonObject, String rel) {
        Map<String, String> links = parseLinks(jsonObject);
        return links.get(rel);
    }
}
