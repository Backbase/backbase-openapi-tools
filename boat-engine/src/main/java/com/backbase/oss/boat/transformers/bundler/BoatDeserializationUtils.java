package com.backbase.oss.boat.transformers.bundler;

import io.swagger.v3.core.util.Json;
import io.swagger.v3.core.util.Yaml;
import io.swagger.v3.oas.models.examples.Example;

import java.util.Objects;

public class BoatDeserializationUtils {

    public static <T> T deserialize(Object contents, String fileOrHost, Class<T> expectedType) {
        T result;

        boolean isJson = false;

        if (contents instanceof String && isJson((String) contents)) {
            isJson = true;
        }

        if (expectedType.equals(Example.class)) {
            Example example = new Example();
            example.setValue(contents);
            return (T) example;
        }

        try {
            if (contents instanceof String) {
                if (isJson) {
                    result = Json.mapper().readValue((String) contents, expectedType);
                } else {
                    result = Yaml.mapper().readValue((String) contents, expectedType);
                }
            } else {
                result = Json.mapper().convertValue(contents, expectedType);
            }
        } catch (Exception e) {
            throw new RuntimeException("An exception was thrown while trying to deserialize the contents of " + fileOrHost + " into type " + expectedType, e);
        }

        return result;
    }

    private static boolean isJson(String contents) {
        return !Objects.isNull(contents) && contents.trim().startsWith("{");
    }

    private static boolean isXml(String contents) {
        return !Objects.isNull(contents) && contents.trim().startsWith("<");
    }

}
