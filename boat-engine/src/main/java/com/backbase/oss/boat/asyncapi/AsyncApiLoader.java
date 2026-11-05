package com.backbase.oss.boat.asyncapi;

import com.backbase.oss.boat.asyncapi.RefResolver.Target;
import com.backbase.oss.boat.asyncapi.model.AsyncApiDocument;
import com.backbase.oss.boat.asyncapi.model.Channel;
import com.backbase.oss.boat.asyncapi.model.ChannelParameter;
import com.backbase.oss.boat.asyncapi.model.Message;
import com.backbase.oss.boat.asyncapi.model.Operation;
import com.backbase.oss.boat.asyncapi.model.Operation.Action;
import com.backbase.oss.boat.asyncapi.model.Operation.Reply;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;

/**
 * Loads AsyncAPI 3.0 and 3.1 documents into an {@link AsyncApiDocument}.
 *
 * <p>Structural references (channels, operations, messages, parameters, traits, replies) are resolved, also when they
 * point at external files. Schema references are kept as-is, but rebased to the root document when the schema was
 * found in an external file, so they can be resolved later by the OpenAPI bundler.
 */
@Slf4j
public final class AsyncApiLoader {

    /**
     * AsyncAPI versions that can be loaded. A document is read as the version with the same major and minor version.
     */
    public static final List<String> SUPPORTED_VERSIONS = Collections.unmodifiableList(Arrays.asList("3.0.0", "3.1.0"));

    private static final String REF = "$ref";
    private static final String MESSAGES = "messages";
    private static final String CHANNEL = "channel";
    private static final String PAYLOAD = "payload";
    private static final String HEADERS = "headers";
    private static final String SCHEMA_FORMAT = "schemaFormat";
    private static final String SCHEMA = "schema";
    private static final String TITLE = "title";
    private static final String DESCRIPTION = "description";
    private static final String SUMMARY = "summary";
    private static final String ADDRESS = "address";
    private static final String BINDINGS = "bindings";
    private static final String LOCATION = "location";
    private static final String CORRELATION_ID = "correlationId";

    private static final Pattern CHANNEL_POINTER = Pattern.compile("^/channels/([^/]+)$");
    private static final Pattern CHANNEL_MESSAGE_POINTER = Pattern.compile("^/channels/([^/]+)/messages/([^/]+)$");
    private static final Pattern COMPONENT_MESSAGE_POINTER = Pattern.compile("^/components/messages/([^/]+)$");

    private AsyncApiLoader() {
        throw new AssertionError("Private constructor");
    }

    public static AsyncApiDocument load(File file) throws AsyncApiLoaderException {
        if (!file.exists()) {
            throw new AsyncApiLoaderException("Could not load AsyncAPI from file: " + file.getAbsolutePath()
                + ". File does not exist!");
        }
        return load(file.toURI());
    }

    public static AsyncApiDocument load(URI uri) throws AsyncApiLoaderException {
        log.debug("Reading AsyncAPI from: {}", uri);
        JsonNode raw;
        try {
            raw = RefResolver.read(uri);
        } catch (IOException e) {
            throw new AsyncApiLoaderException("Could not read AsyncAPI from: " + uri, e);
        }
        return parse(raw, uri);
    }

    public static AsyncApiDocument parse(JsonNode raw, URI location) throws AsyncApiLoaderException {
        return new Builder(raw, location).build();
    }

    /**
     * Checks the {@code asyncapi} field of the document.
     *
     * @return the {@link #SUPPORTED_VERSIONS supported version} the document is read as
     */
    static String checkVersion(JsonNode raw) throws AsyncApiLoaderException {
        if (raw == null || !raw.isObject()) {
            throw new AsyncApiLoaderException("AsyncAPI document must be a YAML or JSON object");
        }
        JsonNode version = raw.get("asyncapi");
        if (version == null) {
            throw new AsyncApiLoaderException("Not an AsyncAPI document: missing 'asyncapi' version field");
        }
        String value = version.asText();
        if (value.startsWith("2.")) {
            throw new AsyncApiLoaderException("AsyncAPI " + value + " is not supported. Migrate the document to "
                + "AsyncAPI 3 (https://www.asyncapi.com/docs/migration/migrating-to-v3)");
        }
        for (String supported : SUPPORTED_VERSIONS) {
            if (value.startsWith(minorVersion(supported) + ".")) {
                return supported;
            }
        }
        throw new AsyncApiLoaderException("AsyncAPI " + value + " is not supported, expected "
            + SUPPORTED_VERSIONS.stream().map(supported -> minorVersion(supported) + ".x")
            .collect(Collectors.joining(" or ")));
    }

    private static String minorVersion(String version) {
        return version.substring(0, version.lastIndexOf('.'));
    }

    private static final class Builder {

        private final JsonNode raw;
        private final URI root;
        private final RefResolver resolver;
        private final AsyncApiDocument document = new AsyncApiDocument();

        private Builder(JsonNode raw, URI location) {
            this.raw = raw;
            this.resolver = new RefResolver(location, raw);
            this.root = resolver.getRootUri();
        }

        private AsyncApiDocument build() throws AsyncApiLoaderException {
            checkVersion(raw);
            document.setRaw(raw);
            document.setLocation(root);
            document.setAsyncapi(raw.get("asyncapi").asText());
            document.setId(text(raw, "id"));
            document.setDefaultContentType(text(raw, "defaultContentType"));

            JsonNode info = raw.path("info");
            document.getInfo().setTitle(text(info, TITLE));
            document.getInfo().setVersion(text(info, "version"));
            document.getInfo().setDescription(text(info, DESCRIPTION));

            parseServers();

            JsonNode components = raw.path("components");
            for (Map.Entry<String, JsonNode> entry : fields(components.path("schemas"))) {
                JsonNode schema = entry.getValue();
                if (isMultiFormatSchema(schema)) {
                    document.getComponentSchemaFormats().put(entry.getKey(), schema.get(SCHEMA_FORMAT).asText());
                    schema = schema.get(SCHEMA);
                }
                document.getComponentSchemas().put(entry.getKey(), schema);
            }
            for (Map.Entry<String, JsonNode> entry : fields(components.path(MESSAGES))) {
                String pointer = "/components/messages/" + escape(entry.getKey());
                document.getComponentMessages().put(entry.getKey(),
                    parseMessage(entry.getValue(), root, entry.getKey(), pointer));
            }

            for (Map.Entry<String, JsonNode> entry : fields(raw.path("channels"))) {
                document.getChannels().put(entry.getKey(), parseChannel(entry.getValue(), root, entry.getKey()));
            }

            for (Map.Entry<String, JsonNode> entry : fields(raw.path("operations"))) {
                document.getOperations().put(entry.getKey(), parseOperation(entry.getKey(), entry.getValue()));
            }
            return document;
        }

        private void parseServers() throws AsyncApiLoaderException {
            for (Map.Entry<String, JsonNode> entry : fields(raw.path("servers"))) {
                JsonNode node = resolver.resolve(entry.getValue(), root).getNode();
                AsyncApiDocument.Server server = new AsyncApiDocument.Server();
                server.setKey(entry.getKey());
                server.setHost(text(node, "host"));
                server.setProtocol(text(node, "protocol"));
                server.setProtocolVersion(text(node, "protocolVersion"));
                server.setPathname(text(node, "pathname"));
                server.setDescription(text(node, DESCRIPTION));
                document.getServers().put(entry.getKey(), server);
            }
        }

        private Channel parseChannel(JsonNode node, URI base, String key) throws AsyncApiLoaderException {
            Target target = resolver.resolve(node, base);
            JsonNode channelNode = target.getNode();

            Channel channel = new Channel();
            channel.setKey(key);
            channel.setAddress(text(channelNode, ADDRESS));
            channel.setTitle(text(channelNode, TITLE));
            channel.setSummary(text(channelNode, SUMMARY));
            channel.setDescription(text(channelNode, DESCRIPTION));
            channel.setBindings(channelNode.get(BINDINGS));

            for (Map.Entry<String, JsonNode> entry : fields(channelNode.path("parameters"))) {
                channel.getParameters().put(entry.getKey(),
                    parseParameter(entry.getKey(), entry.getValue(), target.getBaseUri()));
            }
            for (Map.Entry<String, JsonNode> entry : fields(channelNode.path(MESSAGES))) {
                String pointer = "/channels/" + escape(key) + "/messages/" + escape(entry.getKey());
                Message component = componentMessage(entry.getValue(), target.getBaseUri());
                channel.getMessages().put(entry.getKey(), component != null
                    ? component.copy(entry.getKey(), pointer)
                    : parseMessage(entry.getValue(), target.getBaseUri(), entry.getKey(), pointer));
            }
            return channel;
        }

        /**
         * The already parsed component message {@code node} directly references, so channel messages share its
         * schema nodes and register them only once.
         */
        private Message componentMessage(JsonNode node, URI base) throws AsyncApiLoaderException {
            if (!RefResolver.isRef(node)) {
                return null;
            }
            Target first = resolver.resolveRef(node.get(REF).asText(), base);
            if (!resolver.isRoot(first.getBaseUri())) {
                return null;
            }
            Matcher matcher = COMPONENT_MESSAGE_POINTER.matcher(first.getPointer());
            return matcher.matches() ? document.getComponentMessages().get(unescape(matcher.group(1))) : null;
        }

        private ChannelParameter parseParameter(String name, JsonNode node, URI base) throws AsyncApiLoaderException {
            JsonNode parameterNode = resolver.resolve(node, base).getNode();
            ChannelParameter parameter = new ChannelParameter();
            parameter.setName(name);
            parameter.setDescription(text(parameterNode, DESCRIPTION));
            parameter.setDefaultValue(text(parameterNode, "default"));
            parameter.setLocation(text(parameterNode, LOCATION));
            parameterNode.path("enum").forEach(value -> parameter.getEnumValues().add(value.asText()));
            parameterNode.path("examples").forEach(value -> parameter.getExamples().add(value.asText()));
            return parameter;
        }

        private Message parseMessage(JsonNode node, URI base, String key, String pointer)
            throws AsyncApiLoaderException {
            Target target = resolver.resolve(node, base);
            ObjectNode messageNode = applyTraits(target);

            Message message = new Message();
            message.setKey(key);
            message.setPointer(pointer);
            String name = text(messageNode, "name");
            if (name == null) {
                name = target.getName() != null ? target.getName() : key;
            }
            message.setName(name);
            message.setTitle(text(messageNode, TITLE));
            message.setSummary(text(messageNode, SUMMARY));
            message.setDescription(text(messageNode, DESCRIPTION));
            message.setContentType(text(messageNode, "contentType"));
            message.setBindings(messageNode.get(BINDINGS));

            JsonNode payload = messageNode.get(PAYLOAD);
            if (isMultiFormatSchema(payload)) {
                message.setPayloadSchemaFormat(payload.get(SCHEMA_FORMAT).asText());
                payload = payload.get(SCHEMA);
            }
            message.setPayload(payload);

            JsonNode headers = messageNode.get(HEADERS);
            if (isMultiFormatSchema(headers)) {
                message.setHeadersSchemaFormat(headers.get(SCHEMA_FORMAT).asText());
                headers = headers.get(SCHEMA);
            }
            message.setHeaders(headers);

            JsonNode correlationIdNode = messageNode.get(CORRELATION_ID);
            if (correlationIdNode != null) {
                JsonNode resolved = resolver.resolve(correlationIdNode, root).getNode();
                Message.CorrelationId correlationId = new Message.CorrelationId();
                correlationId.setLocation(text(resolved, LOCATION));
                correlationId.setDescription(text(resolved, DESCRIPTION));
                message.setCorrelationId(correlationId);
            }
            return message;
        }

        private Operation parseOperation(String key, JsonNode node) throws AsyncApiLoaderException {
            Target target = resolver.resolve(node, root);
            ObjectNode operationNode = applyTraits(target);
            URI base = target.getBaseUri();

            Operation operation = new Operation();
            operation.setKey(key);
            try {
                operation.setAction(Action.fromValue(text(operationNode, "action")));
            } catch (IllegalArgumentException e) {
                throw new AsyncApiLoaderException("Operation '" + key + "': " + e.getMessage());
            }
            operation.setTitle(text(operationNode, TITLE));
            operation.setSummary(text(operationNode, SUMMARY));
            operation.setDescription(text(operationNode, DESCRIPTION));
            operation.setBindings(operationNode.get(BINDINGS));
            for (JsonNode tag : operationNode.path("tags")) {
                String tagName = text(resolver.resolve(tag, base).getNode(), "name");
                if (tagName != null) {
                    operation.getTags().add(tagName);
                }
            }

            Channel channel = resolveChannelRef(operationNode.get(CHANNEL), base, "Operation '" + key + "'");
            operation.setChannel(channel);
            operation.setMessages(resolveMessages(operationNode.get(MESSAGES), base, channel));

            JsonNode replyNode = operationNode.get("reply");
            if (replyNode != null) {
                operation.setReply(parseReply(replyNode, base, key));
            }
            return operation;
        }

        private Reply parseReply(JsonNode node, URI base, String operationKey) throws AsyncApiLoaderException {
            Target target = resolver.resolve(node, base);
            JsonNode replyNode = target.getNode();
            Reply reply = new Reply();
            if (replyNode.has(CHANNEL)) {
                reply.setChannel(resolveChannelRef(replyNode.get(CHANNEL), target.getBaseUri(),
                    "Reply of operation '" + operationKey + "'"));
            }
            if (replyNode.has(ADDRESS)) {
                JsonNode address = resolver.resolve(replyNode.get(ADDRESS), target.getBaseUri()).getNode();
                reply.setAddressLocation(text(address, LOCATION));
            }
            reply.setMessages(resolveMessages(replyNode.get(MESSAGES), target.getBaseUri(), reply.getChannel()));
            return reply;
        }

        private List<Message> resolveMessages(JsonNode messagesNode, URI base, Channel channel)
            throws AsyncApiLoaderException {
            List<Message> messages = new ArrayList<>();
            if (messagesNode == null || !messagesNode.isArray()) {
                if (channel != null) {
                    messages.addAll(channel.getMessages().values());
                }
                return messages;
            }
            int index = 0;
            for (JsonNode messageRef : messagesNode) {
                messages.add(resolveMessageRef(messageRef, base, index++));
            }
            return messages;
        }

        private Message resolveMessageRef(JsonNode node, URI base, int index) throws AsyncApiLoaderException {
            if (!RefResolver.isRef(node)) {
                return parseMessage(node, base, "message" + index, null);
            }
            Target first = resolver.resolveRef(node.get(REF).asText(), base);
            if (resolver.isRoot(first.getBaseUri())) {
                Matcher channelMessage = CHANNEL_MESSAGE_POINTER.matcher(first.getPointer());
                if (channelMessage.matches()) {
                    Channel channel = document.getChannels().get(unescape(channelMessage.group(1)));
                    Message message = channel == null ? null
                        : channel.getMessages().get(unescape(channelMessage.group(2)));
                    if (message != null) {
                        return message;
                    }
                }
                Matcher componentMessage = COMPONENT_MESSAGE_POINTER.matcher(first.getPointer());
                if (componentMessage.matches()) {
                    Message message = document.getComponentMessages().get(unescape(componentMessage.group(1)));
                    if (message != null) {
                        return message;
                    }
                }
            }
            return parseMessage(node, base, first.getName(), null);
        }

        private Channel resolveChannelRef(JsonNode node, URI base, String owner) throws AsyncApiLoaderException {
            if (!RefResolver.isRef(node)) {
                throw new AsyncApiLoaderException(owner + " must reference a channel using $ref");
            }
            Target first = resolver.resolveRef(node.get(REF).asText(), base);
            if (resolver.isRoot(first.getBaseUri())) {
                Matcher matcher = CHANNEL_POINTER.matcher(first.getPointer());
                if (matcher.matches()) {
                    Channel channel = document.getChannels().get(unescape(matcher.group(1)));
                    if (channel != null) {
                        return channel;
                    }
                }
            }
            return parseChannel(node, base, first.getName());
        }

        /**
         * Merges the {@code traits} of a message or operation into it (JSON Merge Patch, the object itself wins),
         * after rebasing schema references found in external files to the root document.
         */
        private ObjectNode applyTraits(Target target) throws AsyncApiLoaderException {
            if (!target.getNode().isObject()) {
                throw new AsyncApiLoaderException("Expected an object at " + target.getBaseUri() + "#"
                    + target.getPointer());
            }
            ObjectNode object = rebased(target);
            JsonNode traits = object.remove("traits");
            if (traits == null || !traits.isArray()) {
                return object;
            }
            ObjectNode merged = RefResolver.YAML_MAPPER.createObjectNode();
            for (JsonNode trait : traits) {
                mergePatch(merged, rebased(resolver.resolve(trait, target.getBaseUri())));
            }
            mergePatch(merged, object);
            return merged;
        }

        private ObjectNode rebased(Target target) {
            ObjectNode copy = (ObjectNode) target.getNode().deepCopy();
            for (String field : new String[]{PAYLOAD, HEADERS, CORRELATION_ID, BINDINGS}) {
                if (copy.has(field)) {
                    copy.set(field, resolver.rebase(copy.get(field), target.getBaseUri()));
                }
            }
            return copy;
        }

        private static void mergePatch(ObjectNode target, JsonNode patch) {
            Iterator<Map.Entry<String, JsonNode>> fields = patch.fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> field = fields.next();
                JsonNode existing = target.get(field.getKey());
                if (field.getValue().isNull()) {
                    target.remove(field.getKey());
                } else if (existing instanceof ObjectNode && field.getValue().isObject()) {
                    mergePatch((ObjectNode) existing, field.getValue());
                } else {
                    target.set(field.getKey(), field.getValue().deepCopy());
                }
            }
        }

        private static boolean isMultiFormatSchema(JsonNode node) {
            return node != null && node.isObject() && node.has(SCHEMA_FORMAT) && node.has(SCHEMA);
        }

        private static Iterable<Map.Entry<String, JsonNode>> fields(JsonNode node) {
            return node::fields;
        }

        private static String text(JsonNode node, String field) {
            JsonNode value = node == null ? null : node.get(field);
            return value == null || value.isNull() ? null : value.asText();
        }
    }

    static String escape(String segment) {
        return segment.replace("~", "~0").replace("/", "~1");
    }

    private static String unescape(String segment) {
        return segment.replace("~1", "/").replace("~0", "~");
    }
}
