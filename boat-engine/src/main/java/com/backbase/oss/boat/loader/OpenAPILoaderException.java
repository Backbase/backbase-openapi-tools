package com.backbase.oss.boat.loader;

import java.io.IOException;


public class OpenAPILoaderException extends Throwable {
    public OpenAPILoaderException(String s, IOException e) {
        super(s, e);
    }
}
