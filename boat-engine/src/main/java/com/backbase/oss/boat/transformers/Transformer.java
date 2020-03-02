package com.backbase.oss.boat.transformers;

import io.swagger.v3.oas.models.OpenAPI;

import java.util.HashMap;

public interface Transformer {

    public void transform(OpenAPI openAPI, HashMap<String, Object> options);
}
