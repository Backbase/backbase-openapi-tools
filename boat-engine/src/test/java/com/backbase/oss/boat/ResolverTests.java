package com.backbase.oss.boat;

import com.backbase.oss.boat.loader.OpenAPILoader;
import com.backbase.oss.boat.loader.OpenAPILoaderException;
import java.io.File;
import java.util.ArrayList;

import com.backbase.oss.boat.transformers.bundler.BoatCache;
import com.backbase.oss.boat.transformers.bundler.ExamplesProcessor;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.examples.Example;
import io.swagger.v3.parser.models.RefFormat;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class ResolverTests extends AbstractBoatEngineTestBase {

    @Test
    public void exceptionTest() throws OpenAPILoaderException {
        assertThrows(OpenAPILoaderException.class, () ->
            OpenAPILoader.load(new File("invalidOpenAPI.yaml"), false));
    }

    @Test
    public void testBoatCache() throws OpenAPILoaderException {
        OpenAPI remoteReference = OpenAPILoader.load(getFile("/openapi/bundler-examples-test-api/openapi.yaml"));
        BoatCache boatCache = new BoatCache(remoteReference, new ArrayList<>(), "src/test/resources/openapi/bundler-examples-test-api/openapi.yaml",new ExamplesProcessor(remoteReference,"src/test/resources/openapi/bundler-examples-test-api/openapi.yaml"));

        assertEquals("{\n" + "  \"id\": \"11bd3ca6-5a26-4d97-a3f1-c59df4d6c02f\",\n" + "  \"name\": \"Alex\",\n" + "  \"role\": \"Manager\",\n" + "  \"rank\": 12,\n" + "  \"optional-param\": \"012\"\n" + "}"
                ,boatCache.loadRef("./examples/user-post-response.json#/element", RefFormat.RELATIVE, Example.class).getValue().toString());
    }




}
