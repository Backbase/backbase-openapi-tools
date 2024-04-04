package com.backbase.oss.codegen.doc;

import com.backbase.oss.boat.loader.OpenAPILoader;
import com.backbase.oss.boat.loader.OpenAPILoaderException;
import io.swagger.v3.oas.models.OpenAPI;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import lombok.extern.slf4j.Slf4j;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import org.openapitools.codegen.ClientOptInput;
import org.openapitools.codegen.DefaultGenerator;

@Slf4j
class BoatDocsTest {
    @Test
    void testGenerate() throws IOException {
        String spec = System.getProperty("spec");
        if (spec != null) {
            generateDocs(new File(spec));
        } else {
            generateDocs(getFile("/psd2/psd2-api-1.3.5-20191216v1.yaml"));
        }
//       String generated = String.join( " ", Files.readAllLines(Paths.get("target/docs/index.html")));
//        assertTrue(generated.contains("<title>NextGenPSD2 XS2A Framework</title>"));
    }

    @Test
    void testOpenAPiWithExamples() throws OpenAPILoaderException {
        assertDoesNotThrow(() -> generateDocs(getFile("/openapi-with-examples/openapi.yaml")));
    }

    @Test
    public void testGenerateDocsExampleRefs() {
        assertDoesNotThrow(() -> generateDocs(getFile("/oas-examples/petstore-example-refs.yaml")));
    }


    @Test
    void testGenerateDocs_Multiple_Permissions() throws IOException {
        generateDocs(getFile("/openapi-with-examples/openapi-with-multiple-permissions.yaml"));

        File output = new File("target/docs/");
        String[] actualDirectorySorted = output.list();
        Arrays.sort(actualDirectorySorted);
        String[] expectedDirectory = {".openapi-generator", ".openapi-generator-ignore", "index.html"};
//        assertArrayEquals(expectedDirectory, actualDirectorySorted);
        File index = new File("target/docs/index.html");
        String generated = String.join(" ", Files.readAllLines(Paths.get(index.getPath())));
        assertTrue(generated.startsWith("<!DOCTYPE html>"));
    }

    @Test
    void testGenerateDocs() throws IOException {
        generateDocs(getFile("/openapi-with-examples/openapi-with-json.yaml"));

        File output = new File("target/docs/");
        String[] actualDirectorySorted = output.list();
        Arrays.sort(actualDirectorySorted);
        String[] expectedDirectory = {".openapi-generator", ".openapi-generator-ignore", "index.html"};
//        assertArrayEquals(expectedDirectory, actualDirectorySorted);
        File index = new File("target/docs/index.html");
        String generated = String.join(" ", Files.readAllLines(Paths.get(index.getPath())));
        assertTrue(generated.startsWith("<!DOCTYPE html>"));
    }

    @Test
    void testGenerateDocs_MultiplePermissions() throws IOException {
        generateDocs(getFile("/openapi-with-examples/openapi-with-multiple-permissions.yaml"));
        File index = new File("target/docs/index.html");
        String generated = String.join(" ", Files.readAllLines(Paths.get(index.getPath())));
        assertTrue(generated.startsWith("<!DOCTYPE html>"));
    }

    @Test
    void testGenerateDocsQuery() throws IOException {
        generateDocs(getFile("/oas-examples/petstore-query-string-array.yaml"));
        File output = new File("target/docs/");
        String[] actualDirectorySorted = output.list();
        Arrays.sort(actualDirectorySorted);
        String[] expectedDirectory = {".openapi-generator", ".openapi-generator-ignore", "index.html"};
        assertArrayEquals(expectedDirectory, actualDirectorySorted);
        File index = new File("target/docs/index.html");
        String generated = String.join(" ", Files.readAllLines(Paths.get(index.getPath())));
        assertTrue(generated.contains("<title>Swagger Petstore</title>"));
    }


    protected File getFile(String name) {
        URL resource = getClass().getResource(name);
        assert resource != null;
        return new File(resource.getFile());
    }


    private static void generateDocs(File spec) {
        log.info("Generate docs for: {}", spec);
        OpenAPI openAPI = null;
        try {
            openAPI = OpenAPILoader.load(spec, true, true);
        } catch (OpenAPILoaderException e) {
            log.error("Failed to load open api: {}", spec, e);
            System.exit(1);
        }
        BoatDocsGenerator codegenConfig = new BoatDocsGenerator();

        codegenConfig.setSkipOverwrite(false);
        codegenConfig.setOutputDir(new File("target/docs/").toString());

        File output = new File(codegenConfig.getOutputDir());
        output.mkdirs();


//        log.info("Clearing output: {}, {}", output);


        ClientOptInput input = new ClientOptInput();
        input.config(codegenConfig);
        input.openAPI(openAPI);

        new DefaultGenerator().opts(input).generate();
    }


}
