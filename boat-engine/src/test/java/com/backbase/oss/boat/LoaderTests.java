package com.backbase.oss.boat;

import com.backbase.oss.boat.loader.*;

import java.io.File;
import org.junit.jupiter.api.Test;
import org.raml.v2.api.loader.ResourceUriCallback;

import static org.junit.jupiter.api.Assertions.assertThrows;

class LoaderTests extends AbstractBoatEngineTestBase {

    @Test
    void testRamlResourceLoaderException() {
        File root = new File("src/test/resources/openapi/error-catching");
        File base = new File("src/test/resources/openapi/error-catching");
        String resourceName = "empty-directory";
        RamlResourceLoader ramlResourceLoader = new RamlResourceLoader(base, root);
        assertThrows(RamlLoaderException.class, () -> ramlResourceLoader.fetchResource(resourceName));


    }


    @Test
    void testOpenApiLoader() {
        assertThrows(OpenAPILoaderException.class,()->OpenAPILoader.load("src/test/resources/openapi/error-catching/empty-directory"));
    }



    @Test
    void testCatchingResourceLoader() {
        File root = new File("src/test/resources/openapi/error-catching");
        File base = new File("src/test/resources/openapi/error-catching");
        String resourceName = "empty-directory";
        CachingResourceLoader cachingResourceLoader = new CachingResourceLoader(new RamlResourceLoader(base, root));
        assertThrows(RamlLoaderException.class, () -> cachingResourceLoader.fetchResource(resourceName));


    }


}
