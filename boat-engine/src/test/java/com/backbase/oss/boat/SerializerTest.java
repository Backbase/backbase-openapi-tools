package com.backbase.oss.boat;

import com.backbase.oss.boat.loader.OpenAPILoader;
import com.backbase.oss.boat.loader.OpenAPILoaderException;
import com.backbase.oss.boat.serializer.OpenAPISerializer;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import io.swagger.v3.oas.models.OpenAPI;
import org.junit.Test;

import javax.json.Json;
import javax.json.stream.JsonGeneratorFactory;
import java.io.File;
import java.util.ArrayList;

public class SerializerTest  extends AbstractBoatEngineTests {

    @Test
    public void testOpenApiSerializer() throws OpenAPILoaderException {
//        OpenAPI openAPI = OpenAPILoader.load(getFile("/openapi/presentation-client-api/openapi.yaml"), true);
//        JsonGeneratorFactory factory = Json.createGeneratorFactory();
//        JsonGenerator generator1 = factory.createGenerator(new Writer);
//        new OpenAPISerializer().serialize(openAPI,new JsonGenerator(), new SerializerProvider());
    }
}
