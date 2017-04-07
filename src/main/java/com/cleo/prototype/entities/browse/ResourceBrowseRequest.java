package com.cleo.prototype.entities.browse;


import com.cleo.prototype.entities.common.ResourceSupport;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ResourceBrowseRequest extends ResourceSupport {
    private String path;
    private boolean listItems;
    private int depth;
}
