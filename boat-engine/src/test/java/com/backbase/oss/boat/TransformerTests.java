package com.backbase.oss.boat;

import com.backbase.oss.boat.loader.OpenAPILoader;
import com.backbase.oss.boat.loader.OpenAPILoaderException;
import com.backbase.oss.boat.serializer.SerializerUtils;
import com.backbase.oss.boat.transformers.AdditionalPropertiesAdder;
import com.backbase.oss.boat.transformers.CaseFormatTransformer;
import com.backbase.oss.boat.transformers.Deprecator;
import com.backbase.oss.boat.transformers.Normaliser;
import com.backbase.oss.boat.transformers.OpenAPIExtractor;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import io.swagger.v3.oas.models.OpenAPI;
import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.Arrays;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TransformerTests extends AbstractBoatEngineTestBase {

    @Test
    public void testNormalizer() throws OpenAPILoaderException, IOException {

        OpenAPI openAPI = OpenAPILoader.load(getFile("/psd2/psd2-api-1.3.5-20191216v1.yaml"), false);
        new Normaliser().transform(openAPI, null);
        String output = SerializerUtils.toYamlString(openAPI);
        writeOutput(output, "/openapi.yaml");


        assertTrue(new File("target/openapi.yaml").exists());

    }

    @Test
    public void testDeprecator() throws OpenAPILoaderException, IOException {

        OpenAPI openAPI = OpenAPILoader.load(getFile("/psd2/psd2-api-1.3.5-20191216v1.yaml"), false);

        // Set first found operation to deprecated
        openAPI.getPaths().values().stream().findFirst()
            .flatMap(pathItem -> pathItem.readOperations().stream().findFirst())
            .ifPresent(operation -> operation.setDeprecated(true));

        new Deprecator().transform(openAPI, null);
        String output = SerializerUtils.toYamlString(openAPI);
        writeOutput(output, "/openapi.yaml");

        assertTrue(new File("target/openapi.yaml").exists());

    }

    @Test
    public void testDirectoryExploder() throws OpenAPILoaderException, IOException {

        OpenAPI openAPI = OpenAPILoader.load(getFile("/openapi/presentation-service-api/openapi.yaml"), false);

        File output = new File("target/explode/examples/");

        if (output.exists()) {
            output.delete();
        }
        output.mkdirs();


        OpenAPIExtractor extractor = new OpenAPIExtractor(openAPI);
        ObjectMapper mapper = new ObjectMapper();
        ObjectWriter writer = mapper.writer(new DefaultPrettyPrinter());
        new DirectoryExploder(extractor, writer).serializeIntoDirectory(Paths.get("target/explode"));

        String[] explodedFiles = output.list();
        Arrays.sort(explodedFiles);
        String[] expectedFiles = {"bad-request-error.json", "bool.json", "date-time-only.json", "date-time.json",
            "date-time2616.json", "date.json", "forbidden-error.json", "internal-server-error.json",
            "name-on-card.json", "not-acceptable-error.json", "not-found-error.json", "payment-card.json",
            "payment-cards-post-response-body.json", "payment-cards.json", "time.json", "unauthorized-alt-error.json",
            "unsupported-media-type-error.json", "x--request--id.json"};
        assertArrayEquals(expectedFiles, explodedFiles);
    }


    @Test
    public void testPropertiesAdder() throws OpenAPILoaderException, IOException {

        OpenAPI openAPI = OpenAPILoader.load(getFile("/openapi/presentation-client-api/openapi.yaml"), false);
        new AdditionalPropertiesAdder(Arrays.asList("PaymentCard", "TestValuesGetResponseBody"), "test")
            .transform(openAPI, null);
        String output = SerializerUtils.toYamlString(openAPI);
        writeOutput(output, "/openapi.yaml");

        assertTrue(new File("target/openapi.yaml").exists());

    }


    @Test
    public void testCaseFormatTransformer() throws OpenAPILoaderException, IOException {

        OpenAPI openAPI = OpenAPILoader.load(getFile("/openapi/presentation-client-api/openapi.yaml"), true);
        new CaseFormatTransformer().transform(openAPI, null);
        String output = SerializerUtils.toYamlString(openAPI);
        writeOutput(output, "/openapi.yaml");

        assertTrue(new File("target/openapi.yaml").exists());

    }


}
