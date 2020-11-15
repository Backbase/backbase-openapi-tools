package com.backbase.oss.boat.transformers;

import static java.util.Collections.emptyMap;

import java.util.Map;

import io.swagger.v3.oas.models.OpenAPI;

public interface Transformer {

    default void transform(OpenAPI openAPI) {
        transform(openAPI, emptyMap());
    }

    void transform(OpenAPI openAPI, Map<String, Object> options);
}
