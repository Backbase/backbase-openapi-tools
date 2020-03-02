package com.backbase.oss.boat.loader;

import java.net.URI;
import org.raml.v2.api.loader.ResourceUriCallback;

/**
 * Callback which stores the found resourceURI and calls another callback in a chain.
 */
class CachingResourceUriCallback implements ResourceUriCallback {

    private URI resourceUri;
    private ResourceUriCallback callback;

    /**
     * Constructor with callback.
     *
     * @param callback may be null
     */
    public CachingResourceUriCallback(ResourceUriCallback callback) {
        this.callback = callback;
    }

    @Override
    public void onResourceFound(URI resourceUri) {
        this.resourceUri = resourceUri;
        if (callback != null) {
            callback.onResourceFound(resourceUri);
        }
    }

    public URI getResourceUri() {
        return resourceUri;
    }
}
