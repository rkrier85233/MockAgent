package com.cleo.prototype.entities.dataflow;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Source {
    private String datastoreId;
    private String subPath;
    private ItemMatch itemMatch;
    private String pattern;

    public Source() {
    }

    @Builder
    private Source(String datastoreId, String subPath, ItemMatch itemMatch, String pattern) {
        this.datastoreId = datastoreId;
        this.subPath = subPath;
        this.itemMatch = itemMatch;
        this.pattern = pattern;
    }
}
