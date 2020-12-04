package com.backbase.oss.boat.loader;


public class OpenAPILoaderException extends Exception {

    public OpenAPILoaderException(String message) {
        super(message);
    }

    public OpenAPILoaderException(String message, Throwable throwable) {
        super(message, throwable);
    }
}
