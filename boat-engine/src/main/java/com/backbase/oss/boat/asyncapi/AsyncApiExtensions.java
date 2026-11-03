package com.backbase.oss.boat.asyncapi;

/**
 * Vendor extensions used to carry AsyncAPI semantics through the projected OpenAPI document.
 *
 * <p>Each AsyncAPI operation/message pair becomes a synthetic {@code POST /{operationId}}; these extensions on that
 * operation tell the code generator what it really is. They are internal to boat and not meant to be authored.
 */
public final class AsyncApiExtensions {

    /**
     * Root extension with document level AsyncAPI information.
     */
    public static final String ASYNCAPI = "x-boat-asyncapi";

    /**
     * {@code send} or {@code receive}.
     */
    public static final String ACTION = "x-boat-async-action";

    /**
     * Key of the AsyncAPI operation the synthetic operation was created from.
     */
    public static final String OPERATION = "x-boat-async-operation";

    /**
     * Channel: {@code name}, {@code address}, {@code parameterized} and {@code parameters}.
     */
    public static final String CHANNEL = "x-boat-async-channel";

    /**
     * Message: {@code name}, {@code title}, {@code summary}, {@code contentType}, {@code headers} ({@code $ref}) and
     * {@code correlationId}.
     */
    public static final String MESSAGE = "x-boat-async-message";

    /**
     * Reply: {@code channel}, {@code addressLocation} and {@code message}. The reply payload is the 200 response.
     */
    public static final String REPLY = "x-boat-async-reply";

    /**
     * Extension on {@code components} mapping message pointers to their payload and headers schema refs.
     */
    public static final String MESSAGES = "x-boat-async-messages";

    private AsyncApiExtensions() {
    }
}
