package com.backbase.oss.boat.loader;


import java.util.List;

public class OpenAPILoaderException extends Exception {

    private final List<String> parseMessages;

    public OpenAPILoaderException(String message, List<String> messages) {
        super(message);
        this.parseMessages = messages;
    }

    public OpenAPILoaderException(String message, Throwable throwable) {
        super(message, throwable);
        parseMessages = null;
    }

    public OpenAPILoaderException(String message) {
        super(message);
        parseMessages = null;
    }

    public List<String> getParseMessages() {
        return parseMessages;
    }
}
