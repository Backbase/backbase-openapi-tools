package com.backbase.oss.boat.transformers;

import io.swagger.v3.oas.models.OpenAPI;

import java.util.Map;

public interface Transformer {

    public void transform(OpenAPI openAPI, Map<String, Object> options);
}
