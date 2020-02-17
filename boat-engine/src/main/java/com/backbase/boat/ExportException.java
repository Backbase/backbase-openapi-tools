package com.backbase.boat;

public class ExportException extends Exception {

    public ExportException(String message, Throwable cause) {
        super(message, cause);
    }

    public ExportException(String message) {
        super(message);
    }
}
