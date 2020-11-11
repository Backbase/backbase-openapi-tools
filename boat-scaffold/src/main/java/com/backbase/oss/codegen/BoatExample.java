package com.backbase.oss.codegen;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.models.examples.Example;
import lombok.Data;

@Data
public class BoatExample {

    private static ObjectMapper objectMapper = new ObjectMapper();

    static {
        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
    }

    private String name;
    private String contentType;
    private Example example;

    public BoatExample(String key, String contentType, Example value) {
        this.name = key;
        this.contentType = contentType;
        this.example = value;
    }

    public String getPrettyPrintValue() {
        String s = null;
        try {
            s = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(example.getValue());
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        return s;

    }


}
