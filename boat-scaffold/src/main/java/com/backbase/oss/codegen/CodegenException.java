package com.backbase.oss.codegen;

public class CodegenException extends RuntimeException {
    public CodegenException(String message) {
        super(message);
    }

    public CodegenException(String message, Throwable cause) {
        super(message, cause);
    }
}
