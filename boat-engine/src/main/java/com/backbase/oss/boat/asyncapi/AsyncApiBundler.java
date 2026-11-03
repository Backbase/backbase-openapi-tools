package com.backbase.oss.boat.asyncapi;

import com.backbase.oss.boat.asyncapi.AsyncApiProjection.MessageSchemas;
import com.backbase.oss.boat.asyncapi.RefResolver.Target;
import com.backbase.oss.boat.asyncapi.model.AsyncApiDocument;
import com.backbase.oss.boat.serializer.SerializerUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;
import io.swagger.v3.oas.models.OpenAPI;
import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;

/**
 * Bundles an AsyncAPI 3.0 document and everything it references into a single self-contained document.
 *
 * <p>Schemas are bundled through the OpenAPI projection, with the same {@link com.backbase.oss.boat.transformers.Bundler}
 * and {@link com.backbase.oss.boat.transformers.DeduplicateSchemasTransformer} as {@code boat:bundle} uses for
 * OpenAPI, so shared schema files end up under the same component names in both. The resolved
 * {@code components.schemas} are written back and message payloads and headers point at them; inline payloads and
 * headers become {@code {MessageName}Payload} and {@code {MessageName}Headers} components. Other external references
 * (channels, messages, parameters, ...) are inlined.
 */
@Slf4j
@Getter
@Setter
@Accessors(chain = true)
public class AsyncApiBundler {

    private static final String REF = "$ref";
    private static final ObjectMapper YAML = new ObjectMapper(new YAMLFactory()
        .disable(YAMLGenerator.Feature.WRITE_DOC_START_MARKER)
        .enable(YAMLGenerator.Feature.MINIMIZE_QUOTES));
    private static final int MAX_DEPTH = 50;

    private boolean deduplicateSchemas = true;
    private String openApiVersion = AsyncApiOpenApiProjector.DEFAULT_OPENAPI_VERSION;

    /**
     * When set, replaces {@code info.version}.
     */
    private String version;

    /**
     * Vendor extensions to remove, with or without {@code x-} prefix.
     */
    private Collection<String> removeExtensions = Collections.emptyList();

    private List<String> warnings = new ArrayList<>();

    public ObjectNode bundle(File file) throws AsyncApiLoaderException {
        return bundle(AsyncApiLoader.load(file));
    }

    public ObjectNode bundle(AsyncApiDocument document) throws AsyncApiLoaderException {
        AsyncApiProjection projection = new AsyncApiOpenApiProjector()
            .setOpenApiVersion(openApiVersion)
            .setDeduplicateSchemas(deduplicateSchemas)
            .project(document);
        warnings = new ArrayList<>(projection.getWarnings());

        ObjectNode output = (ObjectNode) document.getRaw().deepCopy();
        ObjectNode components = objectField(output, "components");
        JsonNode sourceSchemas = components.remove("schemas");
        RefResolver resolver = new RefResolver(document.getLocation(), document.getRaw());
        inlineExternalRefs(output, resolver, document.getLocation(), 0);

        writeSchemas(components, projection.getOpenAPI(), document, sourceSchemas, resolver);
        projection.getMessageSchemas().forEach((pointer, schemas) -> writeMessageSchemas(output, pointer, schemas));

        if (version != null && !version.isEmpty()) {
            objectField(output, "info").put("version", version);
        }
        if (removeExtensions != null && !removeExtensions.isEmpty()) {
            removeExtensions(output, extensionNames(removeExtensions));
        }
        return output;
    }

    public static String toYaml(JsonNode document) {
        try {
            return YAML.writeValueAsString(document);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Cannot serialize AsyncAPI document", e);
        }
    }

    /**
     * Replaces structural references to external files with their content. Schema locations are skipped: they are
     * replaced by the bundled schemas afterwards.
     */
    private void inlineExternalRefs(JsonNode node, RefResolver resolver, URI base, int depth)
        throws AsyncApiLoaderException {
        if (depth > MAX_DEPTH) {
            throw new AsyncApiLoaderException("External references nested deeper than " + MAX_DEPTH + " levels");
        }
        if (node instanceof ObjectNode) {
            ObjectNode object = (ObjectNode) node;
            List<String> fieldNames = new ArrayList<>();
            object.fieldNames().forEachRemaining(fieldNames::add);
            for (String field : fieldNames) {
                if ("payload".equals(field) || "headers".equals(field)) {
                    continue;
                }
                JsonNode value = object.get(field);
                if (isExternalRef(value)) {
                    object.set(field, inline(value, resolver, base, depth));
                } else {
                    inlineExternalRefs(value, resolver, base, depth + 1);
                }
            }
        } else if (node instanceof ArrayNode) {
            ArrayNode array = (ArrayNode) node;
            for (int i = 0; i < array.size(); i++) {
                if (isExternalRef(array.get(i))) {
                    array.set(i, inline(array.get(i), resolver, base, depth));
                } else {
                    inlineExternalRefs(array.get(i), resolver, base, depth + 1);
                }
            }
        }
    }

    private JsonNode inline(JsonNode ref, RefResolver resolver, URI base, int depth) throws AsyncApiLoaderException {
        Target target = resolver.resolveRef(ref.get(REF).asText(), base);
        JsonNode content = resolver.rebase(target.getNode(), target.getBaseUri());
        if (isExternalRef(content)) {
            return inline(content, resolver, base, depth + 1);
        }
        inlineExternalRefs(content, resolver, base, depth + 1);
        return content;
    }

    /**
     * Writes the bundled schemas. Components declared as Multi Format Schema Object keep their
     * {@code schemaFormat}; those in a format the projection skips are copied from the source.
     */
    private void writeSchemas(ObjectNode components, OpenAPI openAPI, AsyncApiDocument document,
        JsonNode sourceSchemas, RefResolver resolver) throws AsyncApiLoaderException {
        ObjectNode schemas = components.objectNode();
        if (openAPI.getComponents() != null && openAPI.getComponents().getSchemas() != null) {
            schemas = SerializerUtils.jsonMapper(openAPI).valueToTree(openAPI.getComponents().getSchemas());
        }
        for (Map.Entry<String, String> entry : document.getComponentSchemaFormats().entrySet()) {
            String name = entry.getKey();
            if (!SchemaFormats.isSupported(entry.getValue())) {
                JsonNode source = sourceSchemas.get(name).deepCopy();
                inlineExternalRefs(source, resolver, document.getLocation(), 0);
                schemas.set(name, source);
            } else if (schemas.has(name)) {
                ObjectNode wrapper = schemas.objectNode();
                wrapper.put("schemaFormat", entry.getValue());
                wrapper.set("schema", schemas.get(name));
                schemas.set(name, wrapper);
            }
        }
        if (!schemas.isEmpty()) {
            components.set("schemas", schemas);
        }
    }

    private static ObjectNode objectField(ObjectNode parent, String field) {
        JsonNode value = parent.get(field);
        return value instanceof ObjectNode ? (ObjectNode) value : parent.putObject(field);
    }

    private static void writeMessageSchemas(ObjectNode output, String pointer, MessageSchemas schemas) {
        JsonNode message = output.at(pointer);
        if (!(message instanceof ObjectNode) || message.has(REF)) {
            return;
        }
        ObjectNode messageNode = (ObjectNode) message;
        setSchemaRef(messageNode, "payload", schemas.getPayloadRef());
        setSchemaRef(messageNode, "headers", schemas.getHeadersRef());
    }

    private static void setSchemaRef(ObjectNode message, String field, String ref) {
        if (ref == null) {
            return;
        }
        JsonNode existing = message.get(field);
        ObjectNode refNode = message.objectNode().put(REF, ref);
        if (existing != null && existing.isObject() && existing.has("schemaFormat") && existing.has("schema")) {
            ((ObjectNode) existing).set("schema", refNode);
        } else {
            message.set(field, refNode);
        }
    }

    private static boolean isExternalRef(JsonNode node) {
        return RefResolver.isRef(node) && !node.get(REF).asText().startsWith("#");
    }

    private static Set<String> extensionNames(Collection<String> names) {
        Set<String> result = new HashSet<>();
        for (String name : names) {
            result.add(name);
            if (!name.startsWith("x-")) {
                result.add("x-" + name);
            }
        }
        return result;
    }

    private static void removeExtensions(JsonNode node, Set<String> names) {
        if (node instanceof ObjectNode) {
            ((ObjectNode) node).remove(names);
        }
        Iterator<JsonNode> children = node.elements();
        while (children.hasNext()) {
            removeExtensions(children.next(), names);
        }
    }
}
