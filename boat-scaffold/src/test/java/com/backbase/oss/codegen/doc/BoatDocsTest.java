package com.backbase.oss.codegen.doc;

import com.backbase.oss.boat.loader.OpenAPILoader;
import com.backbase.oss.boat.loader.OpenAPILoaderException;
import io.swagger.v3.oas.models.OpenAPI;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.Arrays;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.openapitools.codegen.ClientOptInput;
import org.openapitools.codegen.DefaultGenerator;

import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_DELETE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SuppressWarnings("InfiniteLoopStatement")
@Slf4j
class BoatDocsTest {

    static void main(String[] args) {

        try {

            WatchService watcher = FileSystems.getDefault().newWatchService();
            File templates = new File(System.getProperty("templates"));

            templates.toPath().register(watcher, ENTRY_CREATE,
                ENTRY_DELETE,
                ENTRY_MODIFY);

            generateDocs();

            WatchKey key;
            while ((key = watcher.take()) != null) {
                for (WatchEvent<?> event : key.pollEvents()) {
                    System.out.println("Event kind:" + event.kind() + ". File affected: " + event.context() + ".");
                    new Thread(BoatDocsTest::generateDocs).start();

                }
                key.reset();

            }


        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }

    }

    @Test
    void testGenerateDocs() throws IOException {
        System.setProperty("spec", getFile("/psd2/psd2-api-1.3.5-20191216v1.yaml").getAbsolutePath());
        generateDocs();

        File output = new File("target/docs/");
        String[] actualDirectorySorted = output.list();
        Arrays.sort(actualDirectorySorted);
        String[] expectedDirectory = {".openapi-generator", ".openapi-generator-ignore", "index.html"};
        assertArrayEquals(expectedDirectory, actualDirectorySorted);
        File index = new File("target/docs/index.html");
        String generated = String.join(" ", Files.readAllLines(Paths.get(index.getPath())));
        assertTrue(generated.contains("<title>NextGenPSD2 XS2A Framework</title>"));
    }

    @Test
    void testGenerateDocsQuery() throws IOException {
        System.setProperty("spec", getFile("/oas-examples/petstore-query-string-array.yaml").getAbsolutePath());
        generateDocs();
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


    private static void generateDocs() {


        File spec = new File(System.getProperty("spec"));

        OpenAPI openAPI = null;
        try {
            openAPI = OpenAPILoader.load(spec, true, true);
        } catch (OpenAPILoaderException e) {
            log.error("Failed to load open api: {}", spec, e);
            System.exit(1);
        }
        BoatDocsGenerator codegenConfig = new BoatDocsGenerator();

        codegenConfig.setSkipOverwrite(false);
        codegenConfig.generateAliasModel = true;
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
