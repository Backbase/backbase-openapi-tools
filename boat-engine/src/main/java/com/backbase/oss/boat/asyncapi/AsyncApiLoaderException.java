package com.backbase.oss.boat.asyncapi;

import java.util.Collections;
import java.util.List;
import lombok.Getter;

@Getter
public class AsyncApiLoaderException extends Exception {

    private final List<String> messages;

    public AsyncApiLoaderException(String message) {
        super(message);
        this.messages = Collections.singletonList(message);
    }

    public AsyncApiLoaderException(String message, Throwable cause) {
        super(message, cause);
        this.messages = Collections.singletonList(message);
    }

    public AsyncApiLoaderException(String message, List<String> messages) {
        super(message + ": " + String.join(", ", messages));
        this.messages = messages;
    }
}
