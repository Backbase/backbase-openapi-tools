package com.backbase.oss.boat.asyncapi.model;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;

@Data
public class Message {

    /**
     * Key of the message in the map it was declared in ({@code channels.x.messages} or {@code components.messages}).
     */
    private String key;

    /**
     * Name used for generated types: the {@code name} field, else the key of the referenced component message
     * (or file), else {@link #key}.
     */
    private String name;

    /**
     * JSON pointer of the message in the root document, e.g. {@code /channels/orders/messages/orderPlaced}.
     */
    private String pointer;

    private String title;
    private String summary;
    private String description;
    private String contentType;

    /**
     * Payload schema, with {@code $ref}s relative to the root document.
     */
    private JsonNode payload;
    private String payloadSchemaFormat;

    /**
     * Headers schema, with {@code $ref}s relative to the root document.
     */
    private JsonNode headers;
    private String headersSchemaFormat;

    private CorrelationId correlationId;
    private JsonNode bindings;

    /**
     * Copy declared under another key and pointer. Schema nodes are shared, not copied.
     */
    public Message copy(String newKey, String newPointer) {
        Message copy = new Message();
        copy.key = newKey;
        copy.pointer = newPointer;
        copy.name = name;
        copy.title = title;
        copy.summary = summary;
        copy.description = description;
        copy.contentType = contentType;
        copy.payload = payload;
        copy.payloadSchemaFormat = payloadSchemaFormat;
        copy.headers = headers;
        copy.headersSchemaFormat = headersSchemaFormat;
        copy.correlationId = correlationId;
        copy.bindings = bindings;
        return copy;
    }

    @Data
    public static class CorrelationId {
        private String location;
        private String description;
    }
}
