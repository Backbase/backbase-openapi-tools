package com.backbase.oss.boat.transformers;

import io.swagger.v3.oas.models.OpenAPI;
import java.io.File;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class Bundler implements Transformer {

    @Override
    public void transform(OpenAPI openAPI, Map<String, Object> options) {
        File input = (File) options.get("input");

        // inspiration here: https://github.com/networknt/openapi-bundler
        log.info("Bundling all references in new OpenAPI with base dir: {}", input);


    }
}
