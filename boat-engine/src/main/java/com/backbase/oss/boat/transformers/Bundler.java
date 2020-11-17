package com.backbase.oss.boat.transformers;

import com.backbase.oss.boat.transformers.bundler.BoatOpenAPIResolver;
import io.swagger.v3.oas.models.OpenAPI;
import java.io.File;
import java.util.Collections;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class Bundler implements Transformer {

    private final File inputFile;

    public Bundler(File inputFile) {
        super();
        this.inputFile = inputFile;
    }

    @Override
    public void transform(OpenAPI openAPI, Map<String, Object> options) {
        // Use the BoatOpenApiResolver...
        BoatOpenAPIResolver openAPIResolver = new BoatOpenAPIResolver(openAPI, null, inputFile.toURI().toString());
        openAPIResolver.resolve();
    }

}