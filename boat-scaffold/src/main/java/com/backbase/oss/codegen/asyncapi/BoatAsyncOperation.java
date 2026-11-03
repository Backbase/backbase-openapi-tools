package com.backbase.oss.codegen.asyncapi;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.openapitools.codegen.CodegenOperation;

/**
 * A {@link CodegenOperation} for an AsyncAPI operation, carrying the role and channel details read from the
 * {@code x-boat-async-*} extensions of the projected OpenAPI operation.
 */
@SuppressWarnings("java:S1104")
public class BoatAsyncOperation extends CodegenOperation {

    public boolean isSend;
    public boolean isReceive;
    public boolean hasReply;

    public String asyncOperationId;
    public String messageName;
    public String contentType;
    public String payloadType;

    /**
     * Generated headers class, or {@code null} when the message declares no headers.
     */
    public String headersType;
    public List<String> headerNames = new ArrayList<>();
    public String correlationIdHeader;

    public String channelName;
    public String channelAddress;
    public boolean channelParameterized;
    public List<ChannelParam> channelParams = new ArrayList<>();

    /**
     * Destination for the binding: the channel address, with parameters replaced by {@code *} for receive operations.
     */
    public String destination;

    /**
     * Java expression resolving a parameterized channel address from the publisher method parameters.
     */
    public String destinationExpression;

    public String bindingName;

    public String replyType;
    public String replyHeadersType;
    public List<String> replyHeaderNames = new ArrayList<>();
    public String replyChannelName;
    public String replyDestination;
    public String replyAddressHeader;
    public String replyCorrelationIdHeader;
    public String replyBindingName;
    public String replyHandlerName;

    public BoatAsyncOperation(CodegenOperation source) {
        super();
        copyFields(source);
    }

    /**
     * The handler declares a method for this operation: it receives messages, or it sends and expects a reply.
     */
    public boolean isHandled() {
        return isReceive || (isSend && hasReply);
    }

    @SuppressWarnings({"unchecked", "java:S3011"})
    private void copyFields(CodegenOperation source) {
        for (Field field : CodegenOperation.class.getDeclaredFields()) {
            if (Modifier.isStatic(field.getModifiers())) {
                continue;
            }
            try {
                field.setAccessible(true);
                Object value = field.get(source);
                if (Modifier.isFinal(field.getModifiers())) {
                    Object target = field.get(this);
                    if (target instanceof Collection && value instanceof Collection) {
                        ((Collection<Object>) target).addAll((Collection<Object>) value);
                    } else if (target instanceof Map && value instanceof Map) {
                        ((Map<Object, Object>) target).putAll((Map<Object, Object>) value);
                    }
                } else {
                    field.set(this, value);
                }
            } catch (IllegalAccessException e) {
                throw new IllegalStateException("Cannot copy CodegenOperation." + field.getName(), e);
            }
        }
    }

    @Getter
    @AllArgsConstructor
    public static class ChannelParam {

        private final String name;
        private final String paramName;
        private final String description;
    }
}
