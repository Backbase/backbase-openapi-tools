package com.backbase.oss.boat.asyncapi;

import com.backbase.oss.boat.asyncapi.AsyncApiProjection.MessageSchemas;
import com.backbase.oss.boat.asyncapi.SchemaFormats.Draft;
import com.backbase.oss.boat.asyncapi.model.AsyncApiDocument;
import com.backbase.oss.boat.asyncapi.model.Channel;
import com.backbase.oss.boat.asyncapi.model.ChannelParameter;
import com.backbase.oss.boat.asyncapi.model.Message;
import com.backbase.oss.boat.asyncapi.model.Operation;
import com.backbase.oss.boat.asyncapi.model.Operation.Reply;
import com.backbase.oss.boat.transformers.Bundler;
import com.backbase.oss.boat.transformers.DeduplicateSchemasTransformer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.parser.OpenAPIV3Parser;
import io.swagger.v3.parser.core.models.ParseOptions;
import io.swagger.v3.parser.core.models.SwaggerParseResult;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;

/**
 * Projects an AsyncAPI 3.0 or 3.1 document onto an OpenAPI document, so the existing OpenAPI tooling (bundler, schema
 * de-duplication, code generators) can process its schemas.
 *
 * <ul>
 *   <li>{@code components.schemas} are copied as-is: {@code #/components/schemas/X} means the same in both specs. A
 *   component declared as Multi Format Schema Object contributes its {@code schema}.</li>
 *   <li>A message payload that references a component schema uses it directly. Inline payloads and headers become
 *   {@code {MessageName}Payload} and {@code {MessageName}Headers} component schemas.</li>
 *   <li>Every operation becomes a synthetic {@code POST /{operationId}} whose request body is the message payload,
 *   annotated with the {@code x-boat-async-*} extensions from {@link AsyncApiExtensions}. An operation with several
 *   messages becomes one synthetic operation per message, {@code {operationId}{MessageName}}.</li>
 * </ul>
 *
 * <p>The projection is then resolved with the {@link Bundler} against the AsyncAPI file location, so relative
 * external references resolve exactly like they do for an OpenAPI document next to it, and de-duplicated with the
 * {@link DeduplicateSchemasTransformer}.
 */
@Slf4j
@Getter
@Setter
@Accessors(chain = true)
public class AsyncApiOpenApiProjector {

    public static final String DEFAULT_OPENAPI_VERSION = "3.0.3";

    private static final String REF = "$ref";
    private static final String SCHEMA_REF_PREFIX = "#/components/schemas/";
    private static final String DEFAULT_CONTENT_TYPE = "application/json";
    private static final String DESCRIPTION = "description";
    private static final String CONTENT = "content";
    private static final String SCHEMA = "schema";
    private static final String NAME = "name";
    private static final String HEADERS = "headers";
    private static final String WHICH_OPENAPI = ", which OpenAPI ";
    private static final String PATH_SEPARATOR = "/";

    private static final JsonNodeFactory NODES = JsonNodeFactory.instance;

    /**
     * OpenAPI version of the projection. Use the version the shared schema library is written for.
     */
    private String openApiVersion = DEFAULT_OPENAPI_VERSION;

    /**
     * Resolve external references with the {@link Bundler}.
     */
    private boolean bundle = true;

    /**
     * Merge structurally identical schemas, see {@link DeduplicateSchemasTransformer}.
     */
    private boolean deduplicateSchemas = true;

    public AsyncApiProjection project(AsyncApiDocument document) throws AsyncApiLoaderException {
        if (!openApiVersion.startsWith("3.0.") && !openApiVersion.startsWith("3.1.")) {
            throw new AsyncApiLoaderException("Unsupported projection OpenAPI version " + openApiVersion
                + ", expected 3.0.x or 3.1.x");
        }
        return new Projection(document).run();
    }

    private final class Projection {

        private final AsyncApiDocument document;
        private final List<String> warnings = new ArrayList<>();
        private final ObjectNode root = NODES.objectNode();
        private final ObjectNode schemas = NODES.objectNode();
        private final Map<Message, MessageSchemas> messageSchemas = new IdentityHashMap<>();
        private final Map<JsonNode, String> registeredSchemas = new IdentityHashMap<>();
        private final Set<Message> usedMessages = Collections.newSetFromMap(new IdentityHashMap<>());
        private final Set<String> unsupportedComponents = new HashSet<>();
        private final RefResolver resolver;

        private Projection(AsyncApiDocument document) {
            this.document = document;
            this.resolver = new RefResolver(document.getLocation(), document.getRaw());
        }

        private AsyncApiProjection run() throws AsyncApiLoaderException {
            document.getOperations().values().forEach(operation -> {
                usedMessages.addAll(operation.getMessages());
                if (operation.getReply() != null) {
                    usedMessages.addAll(operation.getReply().getMessages());
                }
            });

            root.put("openapi", openApiVersion);
            root.set("info", info());
            root.set(AsyncApiExtensions.ASYNCAPI, asyncApiInfo());

            document.getComponentSchemas().forEach((name, schema) -> {
                String owner = "Schema '" + name + "'";
                String format = document.getComponentSchemaFormats().get(name);
                if (!SchemaFormats.isSupported(format)) {
                    unsupportedComponents.add(name);
                    warnings.add(owner + " uses unsupported schema format '" + format + "' and is skipped");
                    return;
                }
                schemas.set(name, schema.deepCopy());
                checkKeywords(owner, schema, format);
            });
            for (Message message : allMessages()) {
                registerMessageSchemas(message);
            }

            ObjectNode paths = NODES.objectNode();
            ArrayNode tags = NODES.arrayNode();
            Set<String> tagNames = new LinkedHashSet<>();
            for (Operation operation : document.getOperations().values()) {
                addOperation(operation, paths, tagNames);
            }
            tagNames.forEach(tag -> tags.addObject().put(NAME, tag));
            if (!tags.isEmpty()) {
                root.set("tags", tags);
            }
            root.set("paths", paths);

            ObjectNode components = root.putObject("components");
            components.set("schemas", schemas);
            components.set(AsyncApiExtensions.MESSAGES, messageSchemaExtension());

            OpenAPI openAPI = parse();
            if (bundle) {
                new Bundler(document.getLocation().toString()).transform(openAPI);
                if (deduplicateSchemas) {
                    new DeduplicateSchemasTransformer().transform(openAPI);
                }
            }
            Map<String, MessageSchemas> schemasByPointer = readMessageSchemas(openAPI);
            warnings.forEach(warning -> log.warn("{}: {}", document.getLocation(), warning));
            return new AsyncApiProjection(openAPI, warnings, schemasByPointer);
        }

        private ObjectNode info() {
            ObjectNode info = NODES.objectNode();
            AsyncApiDocument.Info source = document.getInfo();
            info.put("title", source.getTitle() != null ? source.getTitle() : "AsyncAPI");
            if (source.getDescription() != null) {
                info.put(DESCRIPTION, source.getDescription());
            }
            info.put("version", source.getVersion() != null ? source.getVersion() : "1.0.0");
            return info;
        }

        private ObjectNode asyncApiInfo() {
            ObjectNode info = NODES.objectNode();
            info.put("version", document.getAsyncapi());
            putIfNotNull(info, "id", document.getId());
            putIfNotNull(info, "defaultContentType", document.getDefaultContentType());
            ArrayNode servers = info.putArray("servers");
            document.getServers().values().forEach(server -> {
                ObjectNode node = servers.addObject();
                node.put(NAME, server.getKey());
                putIfNotNull(node, "host", server.getHost());
                putIfNotNull(node, "protocol", server.getProtocol());
                putIfNotNull(node, "pathname", server.getPathname());
            });
            return info;
        }

        private Iterable<Message> allMessages() {
            Set<Message> messages = Collections.newSetFromMap(new IdentityHashMap<>());
            List<Message> ordered = new ArrayList<>();
            document.getComponentMessages().values().forEach(message -> addOnce(messages, ordered, message));
            document.getChannels().values().forEach(channel ->
                channel.getMessages().values().forEach(message -> addOnce(messages, ordered, message)));
            usedMessages.forEach(message -> addOnce(messages, ordered, message));
            return ordered;
        }

        private void registerMessageSchemas(Message message) throws AsyncApiLoaderException {
            String owner = "Message '" + message.getName() + "'";
            String unsupported = unsupportedFormat(message);
            if (unsupported != null) {
                if (usedMessages.contains(message)) {
                    throw new AsyncApiLoaderException(owner + " uses schema format '" + unsupported
                        + "', which is not supported. Supported formats: " + SchemaFormats.SUPPORTED);
                }
                warnings.add(owner + " uses unsupported schema format '" + unsupported + "' and is skipped");
                return;
            }
            String typeName = AsyncApiNames.toTypeName(message.getName());
            String payloadRef = schemaRef(message.getPayload(), message.getPayloadSchemaFormat(),
                typeName + "Payload", owner + " payload");
            String headersRef = schemaRef(message.getHeaders(), message.getHeadersSchemaFormat(),
                typeName + "Headers", owner + " headers");
            messageSchemas.put(message, new MessageSchemas(payloadRef, headersRef));
        }

        /**
         * The first unsupported schema format of the payload or headers, including the format of a component schema
         * they reference; {@code null} when all are supported.
         */
        private String unsupportedFormat(Message message) {
            for (String format : new String[]{message.getPayloadSchemaFormat(), message.getHeadersSchemaFormat()}) {
                if (!SchemaFormats.isSupported(format)) {
                    return format;
                }
            }
            for (JsonNode schema : new JsonNode[]{message.getPayload(), message.getHeaders()}) {
                if (schema != null && isComponentSchemaRef(schema)) {
                    String name = RefResolver.lastSegment(schema.get(REF).asText());
                    if (unsupportedComponents.contains(name)) {
                        return document.getComponentSchemaFormats().get(name);
                    }
                }
            }
            return null;
        }

        /**
         * Returns a {@code #/components/schemas/...} ref for the schema, registering it as component schema
         * {@code preferredName} unless it already is a plain reference to a component schema.
         */
        private String schemaRef(JsonNode schema, String schemaFormat, String preferredName, String owner) {
            if (schema == null || schema.isNull()) {
                return null;
            }
            if (isComponentSchemaRef(schema)) {
                return schema.get(REF).asText();
            }
            String registered = registeredSchemas.get(schema);
            if (registered != null) {
                return registered;
            }
            String name = preferredName;
            int counter = 2;
            while (schemas.has(name)) {
                name = preferredName + counter++;
            }
            schemas.set(name, schema.deepCopy());
            checkKeywords(owner, schema, schemaFormat);
            String ref = SCHEMA_REF_PREFIX + name;
            registeredSchemas.put(schema, ref);
            return ref;
        }

        private void addOperation(Operation operation, ObjectNode paths, Set<String> tagNames) {
            List<Message> messages = operation.getMessages();
            if (messages.isEmpty()) {
                warnings.add("Operation '" + operation.getKey() + "' has no messages and is skipped");
                return;
            }
            List<String> tags = operation.getTags().isEmpty() && operation.getChannel() != null
                ? Collections.singletonList(operation.getChannel().getKey())
                : operation.getTags();
            tagNames.addAll(tags);

            for (Message message : messages) {
                String operationId = messages.size() == 1 ? operation.getKey()
                    : operation.getKey() + AsyncApiNames.toTypeName(message.getName());
                String path = PATH_SEPARATOR + operationId;
                if (paths.has(path)) {
                    warnings.add("Duplicate operation id '" + operationId + "', skipping message '"
                        + message.getName() + "'");
                    continue;
                }
                paths.putObject(path).set("post", operationPost(operation, message, operationId, tags));
            }
        }

        private ObjectNode operationPost(Operation operation, Message message, String operationId,
            List<String> tags) {
            ObjectNode post = NODES.objectNode();
            post.put("operationId", operationId);
            ArrayNode tagsNode = post.putArray("tags");
            tags.forEach(tagsNode::add);
            putIfNotNull(post, "summary", operation.getSummary() != null ? operation.getSummary()
                : operation.getTitle());
            putIfNotNull(post, DESCRIPTION, operation.getDescription());

            post.put(AsyncApiExtensions.ACTION, operation.getAction().getValue());
            post.put(AsyncApiExtensions.OPERATION, operation.getKey());
            if (operation.getChannel() != null) {
                post.set(AsyncApiExtensions.CHANNEL, channelNode(operation.getChannel()));
            }
            post.set(AsyncApiExtensions.MESSAGE, messageNode(message));
            addRequestBody(post, message);
            addResponses(post, operation);
            return post;
        }

        private void addRequestBody(ObjectNode post, Message message) {
            MessageSchemas schemasOfMessage = messageSchemas.get(message);
            if (schemasOfMessage == null || schemasOfMessage.getPayloadRef() == null) {
                return;
            }
            ObjectNode requestBody = post.putObject("requestBody");
            requestBody.put("required", true);
            requestBody.putObject(CONTENT).putObject(contentType(message)).putObject(SCHEMA)
                .put(REF, schemasOfMessage.getPayloadRef());
        }

        private void addResponses(ObjectNode post, Operation operation) {
            ObjectNode responses = post.putObject("responses");
            Reply reply = operation.getReply();
            if (reply != null) {
                post.set(AsyncApiExtensions.REPLY, replyNode(operation, reply, responses));
            }
            if (responses.isEmpty()) {
                responses.putObject("202").put(DESCRIPTION, "Accepted");
            }
        }

        private ObjectNode replyNode(Operation operation, Reply reply, ObjectNode responses) {
            ObjectNode replyNode = NODES.objectNode();
            if (reply.getChannel() != null) {
                replyNode.set("channel", channelNode(reply.getChannel()));
            }
            putIfNotNull(replyNode, "addressLocation", reply.getAddressLocation());
            if (reply.getMessages().isEmpty()) {
                return replyNode;
            }
            if (reply.getMessages().size() > 1) {
                warnings.add("Reply of operation '" + operation.getKey() + "' has several messages, only '"
                    + reply.getMessages().get(0).getName() + "' is used");
            }
            Message replyMessage = reply.getMessages().get(0);
            replyNode.set("message", messageNode(replyMessage));
            MessageSchemas replySchemas = messageSchemas.get(replyMessage);
            if (replySchemas != null && replySchemas.getPayloadRef() != null) {
                ObjectNode ok = responses.putObject("200");
                ok.put(DESCRIPTION, "Reply");
                ok.putObject(CONTENT).putObject(contentType(replyMessage)).putObject(SCHEMA)
                    .put(REF, replySchemas.getPayloadRef());
            }
            return replyNode;
        }

        private ObjectNode channelNode(Channel channel) {
            ObjectNode node = NODES.objectNode();
            node.put(NAME, channel.getKey());
            if (channel.getAddress() != null) {
                node.put("address", channel.getAddress());
            } else {
                node.putNull("address");
            }
            node.put("parameterized", channel.isParameterized());
            putIfNotNull(node, DESCRIPTION, channel.getDescription());
            ArrayNode parameters = node.putArray("parameters");
            for (ChannelParameter parameter : channel.getParameters().values()) {
                ObjectNode parameterNode = parameters.addObject();
                parameterNode.put(NAME, parameter.getName());
                putIfNotNull(parameterNode, DESCRIPTION, parameter.getDescription());
                putIfNotNull(parameterNode, "default", parameter.getDefaultValue());
                putIfNotNull(parameterNode, "location", parameter.getLocation());
                ArrayNode enumValues = parameterNode.putArray("enum");
                parameter.getEnumValues().forEach(enumValues::add);
            }
            return node;
        }

        private ObjectNode messageNode(Message message) {
            ObjectNode node = NODES.objectNode();
            node.put(NAME, message.getName());
            putIfNotNull(node, "title", message.getTitle());
            putIfNotNull(node, "summary", message.getSummary());
            node.put("contentType", contentType(message));
            MessageSchemas schemasOfMessage = messageSchemas.get(message);
            if (schemasOfMessage != null && schemasOfMessage.getHeadersRef() != null) {
                node.putObject(HEADERS).put(REF, schemasOfMessage.getHeadersRef());
            }
            if (message.getCorrelationId() != null) {
                ObjectNode correlationId = node.putObject("correlationId");
                putIfNotNull(correlationId, "location", message.getCorrelationId().getLocation());
                putIfNotNull(correlationId, DESCRIPTION, message.getCorrelationId().getDescription());
            }
            return node;
        }

        private String contentType(Message message) {
            if (message.getContentType() != null) {
                return message.getContentType();
            }
            return document.getDefaultContentType() != null ? document.getDefaultContentType()
                : DEFAULT_CONTENT_TYPE;
        }

        private ObjectNode messageSchemaExtension() {
            ObjectNode extension = NODES.objectNode();
            messageSchemas.forEach((message, refs) -> {
                if (message.getPointer() == null) {
                    return;
                }
                ObjectNode node = extension.putObject(message.getPointer());
                if (refs.getPayloadRef() != null) {
                    node.putObject("payload").put(REF, refs.getPayloadRef());
                }
                if (refs.getHeadersRef() != null) {
                    node.putObject(HEADERS).put(REF, refs.getHeadersRef());
                }
            });
            return extension;
        }

        private OpenAPI parse() throws AsyncApiLoaderException {
            ParseOptions options = new ParseOptions();
            options.setResolve(false);
            SwaggerParseResult result = new OpenAPIV3Parser().readContents(root.toString(), null, options);
            if (result.getOpenAPI() == null) {
                throw new AsyncApiLoaderException("Cannot project AsyncAPI " + document.getLocation()
                    + " onto OpenAPI", result.getMessages() == null ? Collections.emptyList() : result.getMessages());
            }
            if (result.getMessages() != null) {
                result.getMessages().forEach(message -> warnings.add("Projected OpenAPI: " + message));
            }
            return result.getOpenAPI();
        }

        /**
         * Reads the message schema refs back after bundling and de-duplication rewrote them, and removes the
         * internal extension from the projection.
         */
        @SuppressWarnings("unchecked")
        private Map<String, MessageSchemas> readMessageSchemas(OpenAPI openAPI) {
            Map<String, MessageSchemas> result = new LinkedHashMap<>();
            if (openAPI.getComponents() == null || openAPI.getComponents().getExtensions() == null) {
                return result;
            }
            Object extension = openAPI.getComponents().getExtensions().remove(AsyncApiExtensions.MESSAGES);
            if (!(extension instanceof Map)) {
                return result;
            }
            ((Map<String, Object>) extension).forEach((pointer, value) -> {
                Map<String, Object> refs = (Map<String, Object>) value;
                result.put(pointer, new MessageSchemas(refOf(refs.get("payload")), refOf(refs.get(HEADERS))));
            });
            if (openAPI.getComponents().getExtensions().isEmpty()) {
                openAPI.getComponents().setExtensions(null);
            }
            return result;
        }

        /**
         * Warns about what the projection cannot express: JSON Schema 2019-09 and later, or keywords OpenAPI 3.0
         * lacks, on a 3.0 projection; draft-04 keywords that OpenAPI 3.1 reads differently on a 3.1 projection.
         */
        private void checkKeywords(String owner, JsonNode schema, String schemaFormat) {
            Optional<Draft> draft = SchemaFormats.draft(schemaFormat);
            URI base = document.getLocation();
            if (openApiVersion.startsWith("3.0.")) {
                if (draft.filter(Draft::isOpenApi31).isPresent()) {
                    warnings.add(owner + " is JSON Schema " + draft.get().getVersion() + WHICH_OPENAPI
                        + openApiVersion + " only partly supports. Set the projection OpenAPI version to 3.1.0.");
                }
                Set<String> found = SchemaKeywords.notInOpenApi30(schema, base, resolver);
                if (!found.isEmpty()) {
                    warnings.add(owner + " uses " + found + WHICH_OPENAPI + openApiVersion + " cannot express; "
                        + "these keywords are ignored. Set the projection OpenAPI version to 3.1.0 to keep them.");
                }
            } else if (draft.filter(Draft.DRAFT_04::equals).isPresent()) {
                Set<String> found = SchemaKeywords.draft04Only(schema, base, resolver);
                if (!found.isEmpty()) {
                    warnings.add(owner + " is JSON Schema draft-04 and uses " + found + WHICH_OPENAPI
                        + openApiVersion + " reads differently. Set the projection OpenAPI version to 3.0.3.");
                }
            }
        }
    }

    private static boolean isComponentSchemaRef(JsonNode schema) {
        return schema.isObject() && schema.size() == 1 && schema.has(REF)
            && schema.get(REF).asText().startsWith(SCHEMA_REF_PREFIX);
    }

    @SuppressWarnings("unchecked")
    private static String refOf(Object value) {
        if (value instanceof Map) {
            Object ref = ((Map<String, Object>) value).get(REF);
            return ref == null ? null : ref.toString();
        }
        return null;
    }

    private static void addOnce(Set<Message> seen, List<Message> ordered, Message message) {
        if (seen.add(message)) {
            ordered.add(message);
        }
    }

    private static void putIfNotNull(ObjectNode node, String field, String value) {
        if (value != null) {
            node.put(field, value);
        }
    }
}
