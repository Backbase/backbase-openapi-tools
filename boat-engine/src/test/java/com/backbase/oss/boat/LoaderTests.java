package com.backbase.oss.boat;

import com.backbase.oss.boat.loader.CachingResourceLoader;
import com.backbase.oss.boat.loader.RamlLoaderException;
import com.backbase.oss.boat.loader.RamlResourceLoader;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;

public class LoaderTests extends AbstractBoatEngineTestBase {

    @Test
    public void testRamlResourceLoaderExceptions(){
        File root = new File("src/test/resources/openapi/error-catching");
        File base = new File("src/test/resources/openapi/error-catching");
        String resourceName = "empty-directory";
        RamlResourceLoader ramlResourceLoader = new RamlResourceLoader(base,root);
        Assert.assertThrows(RamlLoaderException.class, ()->ramlResourceLoader.fetchResource(resourceName));

    }

    @Test
    public void testCatchingResourceLoader(){
        File root = new File("src/test/resources/openapi/error-catching");
        File base = new File("src/test/resources/openapi/error-catching");
        String resourceName = "empty-directory";
        CachingResourceLoader cachingResourceLoader = new CachingResourceLoader( new RamlResourceLoader(base, root));
        Assert.assertThrows(RamlLoaderException.class, ()->cachingResourceLoader.fetchResource(resourceName));
    }


}
