package com.backbase.boat;

import com.backbase.boat.loader.OpenAPILoader;
import com.backbase.boat.loader.OpenAPILoaderException;
import com.backbase.boat.transformers.Exploder;
import io.swagger.v3.oas.models.OpenAPI;
import java.io.File;
import org.junit.Test;

public class ExplodeApiTest extends AbstractBoatEngineTests {


    @Test
    public void explode() throws Exception, OpenAPILoaderException {

        OpenAPI openAPI = OpenAPILoader.load(getFile("/openapi-examples/legal-entity-saga/openapi.yaml"));

        File target = new File("target/exploder/explode");

        Exploder.explode(openAPI, target);

    }


}
