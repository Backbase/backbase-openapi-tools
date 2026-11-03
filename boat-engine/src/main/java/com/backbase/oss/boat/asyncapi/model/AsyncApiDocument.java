package com.backbase.oss.boat.asyncapi.model;

import com.fasterxml.jackson.databind.JsonNode;
import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.Data;
import lombok.ToString;

/**
 * An AsyncAPI 3.0 document with its structural references resolved.
 *
 * <p>Schemas (component schemas, message payloads and headers) are kept as raw JSON so that their {@code $ref}s can
 * be resolved by the OpenAPI bundler.
 */
@Data
public class AsyncApiDocument {

    private String asyncapi;
    private String id;
    private Info info = new Info();
    private String defaultContentType;
    private Map<String, Server> servers = new LinkedHashMap<>();
    private Map<String, Channel> channels = new LinkedHashMap<>();
    private Map<String, Operation> operations = new LinkedHashMap<>();
    private Map<String, JsonNode> componentSchemas = new LinkedHashMap<>();

    /**
     * {@code schemaFormat} of the component schemas declared as Multi Format Schema Objects, by component name.
     * {@link #getComponentSchemas()} holds their unwrapped {@code schema}.
     */
    private Map<String, String> componentSchemaFormats = new LinkedHashMap<>();
    private Map<String, Message> componentMessages = new LinkedHashMap<>();

    /**
     * Location of the document; relative references are resolved against it.
     */
    private URI location;

    @ToString.Exclude
    private JsonNode raw;

    @Data
    public static class Info {
        private String title;
        private String version;
        private String description;
    }

    @Data
    public static class Server {
        private String key;
        private String host;
        private String protocol;
        private String protocolVersion;
        private String pathname;
        private String description;
    }
}
