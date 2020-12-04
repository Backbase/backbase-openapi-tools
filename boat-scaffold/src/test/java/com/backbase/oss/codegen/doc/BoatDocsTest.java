package com.backbase.oss.codegen.doc;

import com.backbase.oss.boat.loader.OpenAPILoader;
import com.backbase.oss.boat.loader.OpenAPILoaderException;
import io.swagger.v3.oas.models.OpenAPI;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.FileSystems;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.openapitools.codegen.ClientOptInput;
import org.openapitools.codegen.DefaultGenerator;

import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_DELETE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY;

@SuppressWarnings("InfiniteLoopStatement")
@Slf4j
public class BoatDocsTest {

    public static void main(String[] args) {

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
    public void testGenerateDocs() {
        System.setProperty("spec", getFile("/psd2/psd2-api-1.3.5-20191216v1.yaml").getAbsolutePath());
        generateDocs();
    }

    @Test
    public void testGenerateDocsQuery() {
        System.setProperty("spec", getFile("/oas-examples/petstore-query-string-array.yaml").getAbsolutePath());
        generateDocs();
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
