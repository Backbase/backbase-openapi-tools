package com.backbase.oss.boat.transformers;

import com.backbase.oss.boat.loader.OpenAPILoader;
import com.backbase.oss.boat.loader.OpenAPILoaderException;
import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import java.io.File;
import java.util.*;

import io.swagger.v3.oas.models.media.Schema;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TransformerTests {

    @Test
    void testUnAlias() throws OpenAPILoaderException {
        // should no longer contain refs
        File input = new File("src/test/resources/openapi/presentation-client-api/openapi.yaml");
        OpenAPI openAPI = OpenAPILoader.load(input);
        assertTrue(openAPI.toString().contains("$ref: #/components/schemas/ErrorMessage"));
        OpenAPI transformed = new UnAliasTransformer().transform(openAPI, Collections.EMPTY_MAP);
        assertTrue(!transformed.toString().contains("$ref: #/components/schemas/ErrorMessage"));
    }



    @Test
    void testCaseFormatTransformer() throws OpenAPILoaderException {
        File input = new File("src/test/resources/openapi/presentation-client-api/openapi.yaml");
        OpenAPI openAPI = OpenAPILoader.load(input);
        Schema schema = new Schema();
        schema.example("{ \"name\":\"John\" }");
        schema.name("stringSchema");
        openAPI.getComponents().getSchemas().put("stringExample",schema);
        assertTrue(openAPI.toString().contains("addHops"));
        assertTrue(openAPI.toString().contains("{ \"name\":\"John\" }"));
        new CaseFormatTransformer().transform(openAPI, Collections.EMPTY_MAP );
        List<String> schemaNames = new ArrayList<>();
        assertFalse(openAPI.toString().contains("addHops"));
        assertFalse(openAPI.toString().contains("{ \"name\":\"John\" }"));
    }




}
