package com.backbase.oss.boat.transformers;

import java.io.IOException;

public class ExplodeException extends RuntimeException {

    public ExplodeException(IOException e) {
        super(e);
    }
}
