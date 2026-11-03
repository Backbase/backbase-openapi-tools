package com.backbase.oss.boat.asyncapi;

import com.backbase.oss.boat.asyncapi.model.AsyncApiDocument;
import com.backbase.oss.boat.asyncapi.model.Channel;
import com.backbase.oss.boat.asyncapi.model.Message;
import com.backbase.oss.boat.asyncapi.model.Operation;
import com.backbase.oss.boat.asyncapi.model.Operation.Reply;
import com.fasterxml.jackson.databind.JsonNode;
import com.networknt.schema.JsonMetaSchema;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.NonValidationKeyword;
import com.networknt.schema.SpecVersion.VersionFlag;
import com.networknt.schema.ValidationMessage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;

/**
 * Validates AsyncAPI 3.0 documents against the official JSON Schema and checks the references between operations,
 * channels, messages and replies.
 *
 * <p>The JSON Schema is bundled with all definitions inlined and remote {@code $ref} loading is disabled, so
 * validation works offline.
 */
@Slf4j
public class AsyncApiValidator {

    private static final String SCHEMA_RESOURCE = "/asyncapi/asyncapi-3.0.0-schema.json";
    private static final Pattern ADDRESS_PARAMETER = Pattern.compile("\\{([^}]+)}");
    private static final Pattern RUNTIME_EXPRESSION = Pattern.compile("^\\$message\\.(header|payload)(#(/.*)?)?$");

    private static JsonSchema schema;

    public AsyncApiValidationResult validate(File file) {
        return validate(file.toURI());
    }

    public AsyncApiValidationResult validate(URI location) {
        AsyncApiValidationResult result = new AsyncApiValidationResult();
        JsonNode raw;
        try {
            raw = RefResolver.read(location);
        } catch (IOException e) {
            result.error("Cannot read " + location + ": " + e.getMessage());
            return result;
        }
        return validate(raw, location, result);
    }

    public AsyncApiValidationResult validate(JsonNode raw, URI location) {
        return validate(raw, location, new AsyncApiValidationResult());
    }

    private AsyncApiValidationResult validate(JsonNode raw, URI location, AsyncApiValidationResult result) {
        try {
            AsyncApiLoader.checkVersion(raw);
        } catch (AsyncApiLoaderException e) {
            result.error(e.getMessage());
            return result;
        }
        result.setTitle(raw.path("info").path("title").asText(null));

        getSchema().validate(raw).stream()
            .sorted(Comparator.comparing(message -> message.getInstanceLocation().toString()))
            .map(AsyncApiValidator::format)
            .collect(Collectors.toCollection(LinkedHashSet::new))
            .forEach(result::error);

        AsyncApiDocument document;
        try {
            document = AsyncApiLoader.parse(raw, location);
        } catch (AsyncApiLoaderException e) {
            result.error(e.getMessage());
            return result;
        }
        checkReferences(document, result);
        return result;
    }

    private static void checkReferences(AsyncApiDocument document, AsyncApiValidationResult result) {
        for (Channel channel : document.getChannels().values()) {
            checkAddressParameters(channel, result);
        }
        for (Operation operation : document.getOperations().values()) {
            String owner = "Operation '" + operation.getKey() + "'";
            Channel channel = operation.getChannel();
            if (!isDeclared(document, channel)) {
                result.error(owner + " must reference a channel declared under 'channels' (#/channels/...)");
            } else {
                checkMessagesBelongToChannel(owner, operation.getMessages(), channel, result);
            }
            if (operation.getMessages().isEmpty()) {
                result.warning(owner + " has no messages, no code is generated for it");
            }
            operation.getMessages().forEach(message -> checkSchemaFormat(owner, message, result));
            if (operation.getReply() != null) {
                checkReply(document, owner, operation.getReply(), result);
            }
        }
        document.getComponentSchemaFormats().forEach((name, format) -> {
            if (!SchemaFormats.isSupported(format)) {
                result.warning("Schema '" + name + "' uses schema format '" + format
                    + "', which is not supported for code generation");
            }
        });
    }

    private static void checkReply(AsyncApiDocument document, String owner, Reply reply,
        AsyncApiValidationResult result) {
        String replyOwner = "Reply of " + owner.toLowerCase().charAt(0) + owner.substring(1);
        if (reply.getChannel() != null) {
            if (!isDeclared(document, reply.getChannel())) {
                result.error(replyOwner + " must reference a channel declared under 'channels' (#/channels/...)");
            } else {
                checkMessagesBelongToChannel(replyOwner, reply.getMessages(), reply.getChannel(), result);
            }
        }
        if (reply.getAddressLocation() != null
            && !RUNTIME_EXPRESSION.matcher(reply.getAddressLocation()).matches()) {
            result.error(replyOwner + " address location '" + reply.getAddressLocation()
                + "' must be a runtime expression like $message.header#/replyTo");
        }
        if (reply.getChannel() == null && reply.getAddressLocation() == null) {
            result.warning(replyOwner + " has neither a channel nor an address, the reply destination is unknown");
        }
    }

    private static void checkMessagesBelongToChannel(String owner, Iterable<Message> messages, Channel channel,
        AsyncApiValidationResult result) {
        for (Message message : messages) {
            if (!channel.getMessages().containsValue(message)) {
                result.error(owner + " message '" + message.getName() + "' must reference a message of channel '"
                    + channel.getKey() + "' (#/channels/" + channel.getKey() + "/messages/...)");
            }
        }
    }

    private static void checkAddressParameters(Channel channel, AsyncApiValidationResult result) {
        if (channel.getAddress() == null) {
            return;
        }
        Matcher matcher = ADDRESS_PARAMETER.matcher(channel.getAddress());
        while (matcher.find()) {
            if (!channel.getParameters().containsKey(matcher.group(1))) {
                result.error("Channel '" + channel.getKey() + "' address parameter '" + matcher.group(1)
                    + "' is not declared under 'parameters'");
            }
        }
    }

    private static void checkSchemaFormat(String owner, Message message, AsyncApiValidationResult result) {
        for (String format : new String[]{message.getPayloadSchemaFormat(), message.getHeadersSchemaFormat()}) {
            if (format != null && !SchemaFormats.isSupported(format)) {
                result.warning(owner + " message '" + message.getName() + "' uses schema format '" + format
                    + "', which is not supported for code generation");
            }
        }
    }

    private static boolean isDeclared(AsyncApiDocument document, Channel channel) {
        return channel != null && document.getChannels().get(channel.getKey()) == channel;
    }

    private static String format(ValidationMessage message) {
        String text = message.getMessage();
        String location = message.getInstanceLocation().toString();
        return text.startsWith(location) ? text : location + ": " + text;
    }

    private static synchronized JsonSchema getSchema() {
        if (schema == null) {
            JsonMetaSchema draft7 = JsonMetaSchema.builder(JsonMetaSchema.getV7())
                .keyword(new NonValidationKeyword("example"))
                .build();
            JsonSchemaFactory factory = JsonSchemaFactory.getInstance(VersionFlag.V7, builder -> builder
                .metaSchema(draft7)
                .schemaLoaders(loaders -> loaders.add(iri -> {
                    String value = iri.toString();
                    if (value.startsWith("http:") || value.startsWith("https:")) {
                        return () -> {
                            throw new IOException("Remote schema loading is disabled: " + value);
                        };
                    }
                    return null;
                })));
            try (InputStream in = AsyncApiValidator.class.getResourceAsStream(SCHEMA_RESOURCE)) {
                if (in == null) {
                    throw new IllegalStateException("Missing AsyncAPI schema resource " + SCHEMA_RESOURCE);
                }
                schema = factory.getSchema(in);
            } catch (IOException e) {
                throw new IllegalStateException("Cannot read AsyncAPI schema resource " + SCHEMA_RESOURCE, e);
            }
        }
        return schema;
    }
}
