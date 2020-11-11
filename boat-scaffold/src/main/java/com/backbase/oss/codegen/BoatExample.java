package com.backbase.oss.codegen;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.models.examples.Example;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

@Data
@Slf4j
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
        if (example.getValue() instanceof JsonNode) {
            try {
                return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(example.getValue());
            } catch (JsonProcessingException e) {
                log.error("Failed to pretty print example: {}", example.getValue(), e);
                return example.getValue().toString();
            }
        } else {
            return example.getValue().toString();
        }
    }

}
