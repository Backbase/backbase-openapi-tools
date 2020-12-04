package com.backbase.oss.boat.transformers;

import static java.util.Collections.emptyMap;

import java.util.Map;

import io.swagger.v3.oas.models.OpenAPI;

public interface Transformer {

    default OpenAPI transform(OpenAPI openAPI) {
        return transform(openAPI, emptyMap());
    }

    OpenAPI transform(OpenAPI openAPI, Map<String, Object> options);
}
