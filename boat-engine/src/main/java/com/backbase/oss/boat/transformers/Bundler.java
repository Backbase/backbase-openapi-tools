package com.backbase.oss.boat.transformers;

import com.backbase.oss.boat.transformers.bundler.BoatOpenAPIResolver;
import io.swagger.v3.oas.models.OpenAPI;
import java.io.File;
import java.util.Map;

public class Bundler implements Transformer {

    private final String uri;

    public Bundler(File inputFile) {
        super();
        this.uri = inputFile.toURI().toString();
    }

    public Bundler(String url) {
        super();
        this.uri = url;
    }

    @Override
    public OpenAPI transform(OpenAPI openAPI, Map<String, Object> options) {
        // Use the BoatOpenApiResolver...
        BoatOpenAPIResolver openAPIResolver = new BoatOpenAPIResolver(openAPI, null, uri);
        openAPIResolver.resolve();

        return openAPI;
    }

}