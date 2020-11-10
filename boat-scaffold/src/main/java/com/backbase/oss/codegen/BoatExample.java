package com.backbase.oss.codegen;

import io.swagger.v3.oas.models.examples.Example;
import lombok.Data;
import lombok.ToString;

@Data
public class BoatExample {

    private String name;
    private String contentType;
    private Example example;

    public BoatExample(String key, String contentType, Example value) {
        this.name = key;
        this.contentType = contentType;
        this.example = value;
    }


}
