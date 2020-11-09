package com.backbase.oss.codegen;

import io.swagger.v3.oas.models.examples.Example;

public class BoatExample {

    public String name;
    public String contentType;
    public Example example;

    public BoatExample(Example value) {
        this.example = value;
    }

    public BoatExample(String key, String ContentType, Example value) {
        this.name = key;
        this.example = value;

    }
}
