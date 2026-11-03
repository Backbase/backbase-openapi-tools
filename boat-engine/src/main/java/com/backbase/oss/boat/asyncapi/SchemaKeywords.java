package com.backbase.oss.boat.asyncapi;

import com.backbase.oss.boat.asyncapi.RefResolver.Target;
import com.fasterxml.jackson.databind.JsonNode;
import java.net.URI;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;

/**
 * Finds JSON Schema keywords that the OpenAPI version of a projection cannot express.
 *
 * <p>Only schema positions are inspected, so property names such as {@code id} or {@code const} are not taken for
 * keywords. External references are followed; references to the component schemas of the root document are not,
 * because those are checked on their own.
 */
final class SchemaKeywords {

    private static final String REF = "$ref";
    private static final String COMPONENT_SCHEMAS = "/components/schemas/";
    private static final String EXCLUSIVE_MINIMUM = "exclusiveMinimum";
    private static final String EXCLUSIVE_MAXIMUM = "exclusiveMaximum";

    private static final Set<String> NON_OAS30_KEYWORDS = Collections.unmodifiableSet(new LinkedHashSet<>(Arrays.asList(
        "const", "if", "then", "else", "$defs", "prefixItems", "contains", "dependentSchemas", "dependentRequired",
        "unevaluatedProperties", "unevaluatedItems", "patternProperties", "propertyNames", "$id", "$anchor")));

    private static final Set<String> SUBSCHEMA_MAPS = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
        "properties", "patternProperties", "definitions", "$defs", "dependentSchemas", "dependencies")));

    private static final Set<String> SUBSCHEMAS = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
        "items", "prefixItems", "allOf", "anyOf", "oneOf", "not", "if", "then", "else", "contains", "propertyNames",
        "additionalProperties", "additionalItems", "unevaluatedProperties", "unevaluatedItems", "contentSchema")));

    private SchemaKeywords() {
    }

    /**
     * Keywords OpenAPI 3.0 cannot express, such as {@code const} or a numeric {@code exclusiveMinimum}.
     */
    static Set<String> notInOpenApi30(JsonNode schema, URI base, RefResolver resolver) {
        Set<String> found = new LinkedHashSet<>();
        visit(schema, base, resolver, (keyword, value) -> {
            if (NON_OAS30_KEYWORDS.contains(keyword)) {
                found.add(keyword);
            } else if ("type".equals(keyword) && value.isArray()) {
                found.add("type: [...]");
            } else if (isExclusiveBound(keyword) && value.isNumber()) {
                found.add(keyword + ": <number>");
            }
        });
        return found;
    }

    /**
     * Draft-04 keywords that mean something else in OpenAPI 3.1: boolean {@code exclusiveMinimum} and
     * {@code exclusiveMaximum}, and {@code id}.
     */
    static Set<String> draft04Only(JsonNode schema, URI base, RefResolver resolver) {
        Set<String> found = new LinkedHashSet<>();
        visit(schema, base, resolver, (keyword, value) -> {
            if (isExclusiveBound(keyword) && value.isBoolean()) {
                found.add(keyword + ": <boolean>");
            } else if ("id".equals(keyword) && value.isTextual()) {
                found.add("id");
            }
        });
        return found;
    }

    private static boolean isExclusiveBound(String keyword) {
        return EXCLUSIVE_MINIMUM.equals(keyword) || EXCLUSIVE_MAXIMUM.equals(keyword);
    }

    private static void visit(JsonNode schema, URI base, RefResolver resolver, BiConsumer<String, JsonNode> keywords) {
        new Walker(resolver, keywords).schema(schema, base);
    }

    private static final class Walker {

        private final RefResolver resolver;
        private final BiConsumer<String, JsonNode> keywords;
        private final Set<String> visitedRefs = new HashSet<>();

        private Walker(RefResolver resolver, BiConsumer<String, JsonNode> keywords) {
            this.resolver = resolver;
            this.keywords = keywords;
        }

        private void schema(JsonNode node, URI base) {
            if (node == null || !node.isObject()) {
                return;
            }
            Iterator<Map.Entry<String, JsonNode>> fields = node.fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> field = fields.next();
                String keyword = field.getKey();
                JsonNode value = field.getValue();
                keywords.accept(keyword, value);
                if (REF.equals(keyword) && value.isTextual()) {
                    ref(value.asText(), base);
                } else if (SUBSCHEMA_MAPS.contains(keyword) && value.isObject()) {
                    value.elements().forEachRemaining(child -> schema(child, base));
                } else if (SUBSCHEMAS.contains(keyword)) {
                    if (value.isArray()) {
                        value.forEach(child -> schema(child, base));
                    } else {
                        schema(value, base);
                    }
                }
            }
        }

        private void ref(String ref, URI base) {
            Target target;
            try {
                target = resolver.resolveRef(ref, base);
            } catch (AsyncApiLoaderException e) {
                // Unresolvable references are reported by the bundler.
                return;
            }
            if (resolver.isRoot(target.getBaseUri()) && target.getPointer() != null
                && target.getPointer().startsWith(COMPONENT_SCHEMAS)) {
                return;
            }
            if (visitedRefs.add(target.getBaseUri() + "#" + target.getPointer())) {
                schema(target.getNode(), target.getBaseUri());
            }
        }
    }
}
