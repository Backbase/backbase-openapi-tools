package com.backbase.boat.transformers;

import io.swagger.v3.oas.models.OpenAPI;

public interface Transformer {

    public void transform(OpenAPI openAPI);
}
