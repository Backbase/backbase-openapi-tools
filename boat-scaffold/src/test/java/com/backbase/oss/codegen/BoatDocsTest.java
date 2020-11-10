package com.backbase.oss.codegen;

import com.backbase.oss.boat.loader.OpenAPILoader;
import com.backbase.oss.boat.loader.OpenAPILoaderException;
import io.swagger.v3.oas.models.OpenAPI;
import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import lombok.extern.slf4j.Slf4j;
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
                    generateDocs();
                }
                key.reset();

            }


        } catch (IOException | InterruptedException | OpenAPILoaderException e) {
            e.printStackTrace();
        }

    }

    private static void generateDocs() throws OpenAPILoaderException {


        File spec = new File(System.getProperty("spec"));

        OpenAPI openAPI = OpenAPILoader.load(spec, true, true);
        BoatDocsGenerator codegenConfig = new BoatDocsGenerator();

        codegenConfig.setSkipOverwrite(false);
        codegenConfig.generateAliasModel = true;
        codegenConfig.setOutputDir(new File("target/docs/").toString());

        File output = new File(codegenConfig.getOutputDir());


        log.info("Clearing output: {}, {}", output, output.delete());


        ClientOptInput input = new ClientOptInput();
        input.config(codegenConfig);
        input.openAPI(openAPI);

        new DefaultGenerator().opts(input).generate();
    }


}
