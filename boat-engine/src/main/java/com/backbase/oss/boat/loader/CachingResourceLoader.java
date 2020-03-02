package com.backbase.oss.boat.loader;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import org.apache.commons.io.IOUtils;
import org.raml.v2.api.loader.DefaultResourceLoader;
import org.raml.v2.api.loader.ResourceLoaderExtended;
import org.raml.v2.api.loader.ResourceUriCallback;

/**
 * There are 3 purposes to this, to cache the resource content, to normalise all $ref URIs in the
 * JSON and to provide reverse lookup from content to resource location.
 */
public class CachingResourceLoader implements ResourceLoaderExtended {

    private Map<String, ContentItem> nameToContent = new HashMap<>();
    private Map<String, ContentItem> contentToName = new HashMap<>();
    private ResourceLoaderExtended resourceLoader;
    private ObjectMapper jsonMapper = new ObjectMapper();

    public CachingResourceLoader() {
        this(new DefaultResourceLoader());
    }

    public CachingResourceLoader(ResourceLoaderExtended resourceLoader) {
        this.resourceLoader = resourceLoader;
    }

    private InputStream cacheContent(URI foundUri, String resourceName, InputStream inputStream) {
        if (inputStream == null) {
            return null;
        }
        try (InputStream in = inputStream) {
            String content = IOUtils.toString(in, StandardCharsets.UTF_8.name());

            content = normaliseJsonRefs(foundUri, resourceName, content);

            ContentItem contentItem = new ContentItem(foundUri, content, resourceName);
            nameToContent.put(resourceName, contentItem);
            contentToName.put(content, contentItem);
            return IOUtils.toInputStream(content, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RamlLoaderException("Failed to cache content", e);
        }
    }

    @Nullable
    @Override
    public InputStream fetchResource(String resourceName) {
        return fetchResource(resourceName, null);
    }

    @Override
    public InputStream fetchResource(String resourceName, final ResourceUriCallback callback) {
        if (nameToContent.containsKey(resourceName)) {
            ContentItem contentItem = nameToContent.get(resourceName);

            if (callback != null) {
                callback.onResourceFound(contentItem.getFoundUri());
            }

            return IOUtils.toInputStream(contentItem.getContent(), StandardCharsets.UTF_8);
        }

        CachingResourceUriCallback myCallback = new CachingResourceUriCallback(callback);
        InputStream inputStream = resourceLoader.fetchResource(resourceName, myCallback);

        return cacheContent(myCallback.getResourceUri(), resourceName, inputStream);
    }

    /**
     * Reverse lookup resource name based on the schema content.
     */
    public String getResourceName(String content) {
        ContentItem contentItem = contentToName.get(content);
        if (contentItem == null) {
            return null;
        }
        return contentItem.getResourceName();
    }

    /**
     * Make all $ref properties in the JSON absolute and normalised.
     *
     * @param foundUri     the URI for the resource
     * @param resourceName path the JSON
     * @param content      the unprocessed content
     * @return processed content with normalised JSON refs
     */
    public String normaliseJsonRefs(URI foundUri, String resourceName, String content) {
        if (!resourceName.endsWith(".json")) {
            return content;
        }
        try {
            JsonNode schema = jsonMapper.readTree(content);
            List<JsonNode> refs = schema.findParents("$ref");
            for (JsonNode ref : refs) {
                String uriString = ref.findValue("$ref").asText();
                URI refUri = URI.create(uriString);
                if (!refUri.isAbsolute()) {
                    // Update the URI only if it needs updating
                    URI resolvedUri = foundUri.resolve(refUri).normalize();
                    ((ObjectNode) ref).put("$ref", resolvedUri.toString());
                }
            }
            return schema.toString();
        } catch (IOException e) {
            throw new RamlLoaderException("Failed to normalize json references", e);
        }
    }
}
