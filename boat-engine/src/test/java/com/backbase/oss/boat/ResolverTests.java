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





}
