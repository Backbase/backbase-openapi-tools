package com.backbase.oss.boat.transformers;

public class TransformerException extends RuntimeException {
    public TransformerException(String message) {
        super(message);
    }

    public TransformerException(String message, Throwable cause) {
        super(message, cause);
    }
}
