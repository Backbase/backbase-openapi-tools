package com.backbase.oss.codegen.doc;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.core.util.Json;
import io.swagger.v3.oas.models.examples.Example;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

@Data
@Slf4j
public class BoatExample {

    private static ObjectMapper objectMapper = new ObjectMapper();

    static {
        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
    }

    private String key;
    private String name;
    private String contentType;
    private Example example;

    public BoatExample(String key, String contentType, Example value) {
        this.key = StringUtils.replace(key, " ", "-");
        this.name = key;
        this.contentType = contentType;
        this.example = value;
    }

    public String getPrettyPrintValue() {
        if (example.getValue() instanceof JsonNode) {
            return Json.pretty(example.getValue());
        } else {
            return example.getValue().toString();
        }
    }

}
