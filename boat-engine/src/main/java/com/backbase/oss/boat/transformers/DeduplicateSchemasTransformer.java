package com.backbase.oss.boat.transformers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import io.swagger.v3.core.util.Json;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Paths;
import io.swagger.v3.oas.models.media.Schema;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;

/**
 * Merges {@code components/schemas} entries that are structurally identical but registered under different
 * names.
 *
 * <p>This situation arises when the same external schema file is reachable through more than one {@code $ref}
 * string (e.g. once via its canonical component name, once via a direct ref straight to the model file). The
 * bundler resolver has no way of knowing both refs point at the same content, so it registers both, producing
 * two field-for-field identical schemas that show up as duplicate models/classes/doc pages downstream.
 *
 * <p>Of every group of duplicates, this transformer keeps a single canonical entry and rewrites every
 * {@code $ref} in {@code paths} and {@code components} that pointed at a discarded duplicate so it points at
 * the canonical entry instead. The discarded duplicate(s) are then removed from {@code components/schemas}.
 *
 * <p>The canonical name is picked with a heuristic: a name starting with an uppercase letter is preferred
 * (schema names are conventionally PascalCase, whereas a name synthesized from a {@code $ref} tends to keep
 * the original file's casing, e.g. {@code arrangement} from {@code ./arrangement.yaml}); ties are then broken
 * by shortest name, then alphabetically.
 *
 * <p><b>This is a breaking change for generated clients.</b> Whichever duplicate name is dropped disappears
 * from the generated Java/TypeScript/Swift/Kotlin models, and any code referencing that type name will fail to
 * compile. Consumers that need time to migrate can disable this behaviour (see the {@code deduplicateSchemas}
 * plugin option on {@code boat:bundle}/{@code boat:generate}).
 */
@SuppressWarnings("rawtypes")
@Slf4j
public class DeduplicateSchemasTransformer implements Transformer {

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
        // rewriteReferences() replaces components with a freshly deserialized instance, so the removal
        // must happen against the new schemas map, not the one captured before the rewrite.
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
        JsonNode pathsNode = Json.mapper().valueToTree(openAPI.getPaths());
        rewriteRefs(pathsNode, renames);
        openAPI.setPaths(Json.mapper().convertValue(pathsNode, Paths.class));

        JsonNode componentsNode = Json.mapper().valueToTree(openAPI.getComponents());
        rewriteRefs(componentsNode, renames);
        openAPI.setComponents(Json.mapper().convertValue(componentsNode, Components.class));
    }

    private void rewriteRefs(JsonNode node, Map<String, String> renames) {
        if (node instanceof ObjectNode) {
            ObjectNode objectNode = (ObjectNode) node;
            JsonNode ref = objectNode.get("$ref");
            if (ref != null && ref.isTextual()) {
                String refValue = ref.textValue();
                if (refValue.startsWith(SCHEMA_REF_PREFIX)) {
                    String canonical = renames.get(refValue.substring(SCHEMA_REF_PREFIX.length()));
                    if (canonical != null) {
                        objectNode.set("$ref", TextNode.valueOf(SCHEMA_REF_PREFIX + canonical));
                    }
                }
            }
            objectNode.elements().forEachRemaining(child -> rewriteRefs(child, renames));
        } else if (node.isArray()) {
            node.elements().forEachRemaining(child -> rewriteRefs(child, renames));
        }
    }
}
