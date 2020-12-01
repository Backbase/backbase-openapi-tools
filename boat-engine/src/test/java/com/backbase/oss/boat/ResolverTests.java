package com.backbase.oss.boat;

import com.backbase.oss.boat.loader.OpenAPILoader;
import com.backbase.oss.boat.loader.OpenAPILoaderException;
import com.backbase.oss.boat.transformers.bundler.BoatOpenAPIResolver;
import io.swagger.v3.oas.models.OpenAPI;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.net.URL;

import static org.apache.commons.io.FileUtils.getFile;

public class ResolverTests extends AbstractBoatEngineTests {

//    @Test
//    public void deserializationUtilsTest() throws OpenAPILoaderException {
//        OpenAPI openAPI = OpenAPILoader.load(getFile("/openapi/presentation-client-api/openapi.yaml"), false);
//
//        new BoatOpenAPIResolver(openAPI).resolve();
//    }

    @Test(expected = OpenAPILoaderException.class)
    public void exceptionTest() throws OpenAPILoaderException {

        OpenAPILoader.load(new File("invalidOpenAPI.yaml"),false );
    }
}
