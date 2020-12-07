package com.backbase.oss.boat;

import com.backbase.oss.boat.loader.CachingResourceLoader;
import com.backbase.oss.boat.loader.RamlLoaderException;
import com.backbase.oss.boat.loader.RamlResourceLoader;
import java.io.File;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

class LoaderTests extends AbstractBoatEngineTestBase {

    @Test
    void testRamlResourceLoaderExceptions() {
        File root = new File("src/test/resources/openapi/error-catching");
        File base = new File("src/test/resources/openapi/error-catching");
        String resourceName = "empty-directory";
        RamlResourceLoader ramlResourceLoader = new RamlResourceLoader(base, root);
        assertThrows(RamlLoaderException.class, () -> ramlResourceLoader.fetchResource(resourceName));

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
