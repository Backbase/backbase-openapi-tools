package com.backbase.oss.boat;

import com.backbase.oss.boat.loader.OpenAPILoader;
import com.backbase.oss.boat.loader.OpenAPILoaderException;
import com.backbase.oss.boat.serializer.SerializerUtils;
import com.backbase.oss.boat.transformers.Normaliser;
import io.swagger.v3.oas.models.OpenAPI;
import java.io.IOException;
import org.junit.Test;

public class TransformerTests extends AbstractBoatEngineTests {

    @Test
    public void testNormalizer() throws OpenAPILoaderException, IOException {

        OpenAPI openAPI = OpenAPILoader.load(getFile("/psd2/psd2-api-1.3.5-20191216v1.yaml"));
        new Normaliser().transform(openAPI, null);
        String output = SerializerUtils.toYamlString(openAPI);
        writeOutput(output, "/openapi.yaml");

    }

}
