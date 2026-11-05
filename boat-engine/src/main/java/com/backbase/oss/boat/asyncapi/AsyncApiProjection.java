package com.backbase.oss.boat.asyncapi;

import io.swagger.v3.oas.models.OpenAPI;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import lombok.Getter;
import lombok.Value;

/**
 * Result of projecting an AsyncAPI document onto OpenAPI.
 */
@Getter
public class AsyncApiProjection {

    private final OpenAPI openAPI;
    private final List<String> warnings;

    /**
     * Payload and headers schema refs of every message, keyed by the message's JSON pointer in the AsyncAPI
     * document. Refs point into the projected {@code components.schemas}.
     */
    private final Map<String, MessageSchemas> messageSchemas;

    AsyncApiProjection(OpenAPI openAPI, List<String> warnings, Map<String, MessageSchemas> messageSchemas) {
        this.openAPI = openAPI;
        this.warnings = Collections.unmodifiableList(warnings);
        this.messageSchemas = Collections.unmodifiableMap(messageSchemas);
    }

    @Value
    public static class MessageSchemas {
        String payloadRef;
        String headersRef;
    }
}
