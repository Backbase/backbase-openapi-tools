package com.backbase.oss.boat.transformers;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.parser.OpenAPIResolver;
import java.io.File;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class Bundler implements Transformer {


    @Override
    public void transform(OpenAPI openAPI, Map<String, Object> options) {

        File inputFile = (File) options.get("input");

        OpenAPIResolver openAPIResolver = new OpenAPIResolver(openAPI, null, inputFile.toURI().toString());
        openAPIResolver.resolve();

    }
}