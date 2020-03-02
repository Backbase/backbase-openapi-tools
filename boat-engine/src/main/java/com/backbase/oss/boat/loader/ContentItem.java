package com.backbase.oss.boat.loader;

import java.net.URI;

/**
 * Record the URI location and content of loaded resources.
 */
class ContentItem {
    private final URI foundUri;
    private final String content;
    private final String resourceName;

    /**
     * Constructor with required field values.
     */
    public ContentItem(URI foundUri, String content, String resourceName) {
        this.foundUri = foundUri;
        this.content = content;
        this.resourceName = resourceName;
    }

    public URI getFoundUri() {
        return foundUri;
    }

    public String getContent() {
        return content;
    }

    public String getResourceName() {
        return resourceName;
    }
}
