package com.backbase.oss.boat.transformers;

import com.backbase.oss.boat.loader.OpenAPILoader;
import com.backbase.oss.boat.loader.OpenAPILoaderException;
import io.swagger.v3.oas.models.OpenAPI;
import java.io.File;
import java.util.Collections;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class TransformerTests {

    @Test
    public void testUnAlias() throws OpenAPILoaderException {
        // should no longer contain refs
        File input = new File("src/test/resources/openapi/presentation-client-api/openapi.yaml");
        OpenAPI openAPI = OpenAPILoader.load(input);
        new UnAliasTransformer().transform(openAPI, Collections.EMPTY_MAP);
        assertTrue(!openAPI.getOpenapi().contains("$ref"));
    }



}
