package com.backbase.oss.boat.transformers;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.parameters.RequestBody;
import io.swagger.v3.oas.models.responses.ApiResponse;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.v3.core.util.Json;

/**
 * OpenAPI 3.1.x variant of DeduplicateSchemasTransformer.
 *
 * <p>This transformer merges {@code components/schemas} entries that are structurally identical
 * but registered under different names. Unlike the base implementation, this version avoids
 * JSON serialization round-trips that cause type information loss in OpenAPI 3.1.x processing.
 *
 * <p>For finding duplicates, it still uses JSON serialization for comparison (to ensure
 * structural equality), but rewrites references using direct object tree traversal to
 * preserve all schema properties including type information.
 */
@SuppressWarnings("rawtypes")
@Slf4j
public class DeduplicateSchemasTransformerV31 implements Transformer {

    private static final String SCHEMA_REF_PREFIX = "#/components/schemas/";

    private static final Comparator<String> CANONICAL_NAME_ORDER = Comparator
        .<String>comparingInt(name -> startsWithUpperCase(name) ? 0 : 1)
        .thenComparingInt(String::length)
        .thenComparing(Comparator.naturalOrder());

    private static boolean startsWithUpperCase(String name) {
        return !name.isEmpty() && Character.isUpperCase(name.charAt(0));
    }

    @Override
    public OpenAPI transform(OpenAPI openAPI, Map<String, Object> options) {
        if (openAPI.getComponents() == null || openAPI.getComponents().getSchemas() == null) {
            return openAPI;
        }

        Map<String, Schema> schemas = openAPI.getComponents().getSchemas();
        Map<String, String> renames = findDuplicateRenames(schemas);

        if (renames.isEmpty()) {
            log.debug("No duplicate schemas found.");
            return openAPI;
        }

        renames.forEach((duplicate, canonical) ->
            log.info("Merging duplicate schema '{}' into '{}'.", duplicate, canonical));

        rewriteReferences(openAPI, renames);
        // Remove duplicate schemas after rewriting all references
        renames.keySet().forEach(openAPI.getComponents().getSchemas()::remove);

        return openAPI;
    }

    /**
     * Groups schemas by structural equality (their serialized JSON representation) and, for every group with
     * more than one member, maps every non-canonical member's name onto the canonical one.
     */
    private Map<String, String> findDuplicateRenames(Map<String, Schema> schemas) {
        Map<JsonNode, List<String>> byContent = new LinkedHashMap<>();

        schemas.forEach((name, schema) -> {
            JsonNode node = Json.mapper().valueToTree(schema);
            byContent.computeIfAbsent(node, key -> new ArrayList<>()).add(name);
        });

        Map<String, String> renames = new LinkedHashMap<>();
        for (List<String> names : byContent.values()) {
            if (names.size() < 2) {
                continue;
            }
            String canonical = names.stream()
                .min(CANONICAL_NAME_ORDER)
                .orElseThrow();
            names.stream()
                .filter(name -> !name.equals(canonical))
                .forEach(name -> renames.put(name, canonical));
        }
        return renames;
    }

    private void rewriteReferences(OpenAPI openAPI, Map<String, String> renames) {
        if (openAPI.getPaths() != null) {
            openAPI.getPaths().forEach((pathName, pathItem) -> {
                if (pathItem != null) {
                    pathItem.readOperations().forEach(operation -> {
                        if (operation.getParameters() != null) {
                            operation.getParameters().forEach(param -> rewriteSchemaRef(param.getSchema(), renames));
                        }
                        if (operation.getRequestBody() != null) {
                            rewriteRequestBodyRef(operation.getRequestBody(), renames);
                        }
                        if (operation.getResponses() != null) {
                            operation.getResponses().forEach((statusCode, response) ->
                                rewriteResponseRef(response, renames));
                        }
                    });
                }
            });
        }

        if (openAPI.getComponents() != null) {
            rewriteComponentsRefs(openAPI.getComponents(), renames);
        }
    }

    private void rewriteComponentsRefs(Components components, Map<String, String> renames) {
        if (components.getSchemas() != null) {
            components.getSchemas().forEach((name, schema) -> rewriteSchemaRef(schema, renames));
        }
        if (components.getRequestBodies() != null) {
            components.getRequestBodies().forEach((name, rb) -> rewriteRequestBodyRef(rb, renames));
        }
        if (components.getResponses() != null) {
            components.getResponses().forEach((name, response) -> rewriteResponseRef(response, renames));
        }
        if (components.getParameters() != null) {
            components.getParameters().forEach((name, param) -> rewriteSchemaRef(param.getSchema(), renames));
        }
    }

    private void rewriteSchemaRef(Schema schema, Map<String, String> renames) {
        if (schema == null) {
            return;
        }
        if (schema.get$ref() != null && schema.get$ref().startsWith(SCHEMA_REF_PREFIX)) {
            String schemaName = schema.get$ref().substring(SCHEMA_REF_PREFIX.length());
            String canonical = renames.get(schemaName);
            if (canonical != null) {
                schema.set$ref(SCHEMA_REF_PREFIX + canonical);
            }
        }
        if (schema.getProperties() != null) {
            schema.getProperties().values().forEach(prop -> rewriteSchemaRef((Schema) prop, renames));
        }
        if (schema.getItems() != null) {
            rewriteSchemaRef((Schema) schema.getItems(), renames);
        }
        if (schema.getAllOf() != null) {
            schema.getAllOf().forEach(s -> rewriteSchemaRef((Schema) s, renames));
        }
        if (schema.getOneOf() != null) {
            schema.getOneOf().forEach(s -> rewriteSchemaRef((Schema) s, renames));
        }
        if (schema.getAnyOf() != null) {
            schema.getAnyOf().forEach(s -> rewriteSchemaRef((Schema) s, renames));
        }
        if (schema.getNot() != null) {
            rewriteSchemaRef(schema.getNot(), renames);
        }
        Object additionalProps = schema.getAdditionalProperties();
        if (additionalProps instanceof Schema) {
            rewriteSchemaRef((Schema) additionalProps, renames);
        }
    }

    private void rewriteRequestBodyRef(RequestBody rb, Map<String, String> renames) {
        if (rb != null && rb.getContent() != null) {
            rb.getContent().forEach((mediaType, content) -> {
                if (content.getSchema() != null) {
                    rewriteSchemaRef(content.getSchema(), renames);
                }
            });
        }
    }

    private void rewriteResponseRef(ApiResponse response, Map<String, String> renames) {
        if (response != null && response.getContent() != null) {
            response.getContent().forEach((mediaType, content) -> {
                if (content.getSchema() != null) {
                    rewriteSchemaRef(content.getSchema(), renames);
                }
            });
        }
    }
}
