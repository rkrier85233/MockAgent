package com.cleo.prototype.entities.browse;


import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ResourceBrowseResponse {
    private String path;
    private boolean listItems;
    private int depth;
    private Container listing;
}
