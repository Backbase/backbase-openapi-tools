package com.backbase.oss.boat;

import com.backbase.oss.boat.loader.OpenAPILoader;
import com.backbase.oss.boat.loader.OpenAPILoaderException;
import java.io.File;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

public class ResolverTests extends AbstractBoatEngineTestBase {

    @Test
    public void exceptionTest() throws OpenAPILoaderException {
        assertThrows(OpenAPILoaderException.class, () ->
            OpenAPILoader.load(new File("invalidOpenAPI.yaml"), false));
    }
}
