package com.cleo.prototype.agent;

import com.cleo.prototype.entities.browse.Container;
import com.cleo.prototype.entities.browse.ContainerItem;
import com.cleo.prototype.entities.browse.ResourceBrowseRequest;
import com.cleo.prototype.entities.browse.ResourceBrowseResponse;
import com.cleo.prototype.entities.common.AgentException;

import org.apache.commons.io.FilenameUtils;

import java.io.File;
import java.net.URI;
import java.util.Date;

public class ResourceBrowseHandler {
    private ResourceBrowseHandler() {
    }

    public static ResourceBrowseResponse build(ResourceBrowseRequest browseRequest) throws AgentException {
        File file = null;
        String path = browseRequest.getPath().replace(" ", "%20");
        try {
            file = new File(URI.create(path));
        } catch (Exception e) {
            final String message = "Cannot access ${type}: '${resource}' because it does not exist.";
            throw new AgentException("ERROR", "BAD_FILE_PATH", message)
                    .addArgs("type", "directory")
                    .addArgs("resource", browseRequest.getPath());

        }
        if (!file.exists()) {
            final String message = "Cannot access ${type}: '${resource}' because it does not exist.";
            throw new AgentException("ERROR", "RESOURCE_NOT_EXISTS", message)
                    .addArgs("type", "directory")
                    .addArgs("resource", browseRequest.getPath());
        }

        if (!file.canRead()) {
            final String message = "Cannot access ${type}: '${resource}' because directory cannot be read.";
            throw new AgentException("ERROR", "RESOURCE_NOT_READABLE", message)
                    .addArgs("type", "directory")
                    .addArgs("resource", browseRequest.getPath());
        }

        ResourceBrowseResponse response = new ResourceBrowseResponse();
        response.setPath(browseRequest.getPath());
        response.setListItems(browseRequest.isListItems());
        response.setDepth(browseRequest.getDepth());

        Container root = new Container();
        root.setFullPath(browseRequest.getPath());
        String name = file.getName();
        if (name == null || name.length() == 0) {
            name = file.getPath();
        }
        root.setName(name);
        root.setModified(new Date(file.lastModified()));
        response.setListing(root);
        addChildren(root, browseRequest.getDepth(), browseRequest.isListItems());

        return response;
    }

    private static void addChildren(Container root, int depth, boolean listItems) {
        if (depth <= 0) {
            return;
        }

        String path = root.getFullPath().replace(" ", "%20");
        File file = new File(URI.create(path));
        File[] children = file.listFiles();
        if (children == null) {
            return;
        }

        for (File child : children) {
            if (child.isHidden() || !child.canRead() || !child.canWrite()) {
                continue;
            }

            if (child.isDirectory()) {
                Container container = new Container();
                container.setFullPath(child.toURI().toString());
                container.setName(child.getName());
                container.setModified(new Date(child.lastModified()));
                root.getContainers().add(container);
                addChildren(container, depth - 1, listItems);
            } else if (listItems) {
                ContainerItem item = new ContainerItem();
                item.setName(child.getName());
                item.setModified(new Date(child.lastModified()));
                item.setSize(child.length());
                item.setExtension(FilenameUtils.getExtension(child.getName()));
                root.getContainerItems().add(item);
            }
        }
    }
}
