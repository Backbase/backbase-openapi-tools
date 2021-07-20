package com.backbase.oss.codegen.doc;

import com.fasterxml.jackson.databind.JsonNode;
import com.samskivert.mustache.Mustache;
import io.swagger.v3.core.util.Json;
import io.swagger.v3.oas.models.examples.Example;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.HashMap;

@Data
@Slf4j
public class BoatExample {

    public static final Mustache.Lambda escapeJavascript = (fragment, writer) -> {
        String text = fragment.execute();
        StringEscapeUtils.escapeJavaScript(writer, text);
    };

    private String key;
    private String name;
    private String contentType;
    private Example example;
    private boolean isJson;

    public BoatExample(String key, String contentType, Example value, boolean isJson) {
        this.key = StringUtils.replace(key, " ", "-");
        this.name = key;
        this.contentType = contentType;
        this.isJson = isJson;
        this.example = value;
    }

    public String getPrettyPrintValue() {
        if (example.getValue() instanceof JsonNode
                || example.getValue() instanceof HashMap) {
            return Json.pretty(example.getValue());
        } else if (example.getValue() == null) {
            return "";
        } else {
            if (example.getValue() != null) {
                return example.getValue().toString();
            } else {
                return null;
            }
        }
    }
}
