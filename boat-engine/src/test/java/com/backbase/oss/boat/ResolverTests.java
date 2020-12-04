package com.backbase.oss.boat;

import com.backbase.oss.boat.loader.OpenAPILoader;
import com.backbase.oss.boat.loader.OpenAPILoaderException;
import org.junit.Test;

import java.io.File;

import static org.apache.commons.io.FileUtils.getFile;

public class ResolverTests extends AbstractBoatEngineTestBase {

    @Test(expected = OpenAPILoaderException.class)
    public void exceptionTest() throws OpenAPILoaderException {
        OpenAPILoader.load(new File("invalidOpenAPI.yaml"),false );
    }
}
