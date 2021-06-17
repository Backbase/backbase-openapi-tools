package com.backbase.oss.codegen.marina;

import com.backbase.oss.boat.loader.OpenAPILoader;
import com.backbase.oss.boat.loader.OpenAPILoaderException;
import com.backbase.oss.codegen.doc.BoatDocsGenerator;
import io.swagger.v3.oas.models.OpenAPI;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.openapitools.codegen.ClientOptInput;
import org.openapitools.codegen.DefaultGenerator;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

@Slf4j
class BoatMarinaTest {

    @Test
    void testGenerate() throws IOException {
        String spec = System.getProperty("spec");
        if (spec != null) {
            generateDocs(new File(spec));
        } else {
            generateDocs(getFile("/psd2/psd2-api-1.3.5-20191216v1.yaml"));
        }
//        String generated = String.join( " ", Files.readAllLines(Paths.get("target/docs/index.html")));
    }

    @Test
    void testGenerateDocs() throws IOException {
        generateDocs(getFile("/openapi/presentation-service-api/openapi.yaml"));

        File output = new File("target/marina-docs/");
        String[] actualDirectorySorted = output.list();
        Arrays.sort(actualDirectorySorted);
        String[] expectedDirectory = {".openapi-generator", ".openapi-generator-ignore", "index.html"};
        File index = new File("target/marina-docs/index.html");
        String generated = String.join(" ", Files.readAllLines(Paths.get(index.getPath())));
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
        BoatMarinaGenerator codegenConfig = new BoatMarinaGenerator();

        codegenConfig.setSkipOverwrite(false);
        codegenConfig.setOutputDir(new File("target/marina-docs/").toString());

        File output = new File(codegenConfig.getOutputDir());
        output.mkdirs();

//        log.info("Clearing output: {}, {}", output);


        ClientOptInput input = new ClientOptInput();
        input.config(codegenConfig);
        input.openAPI(openAPI);

        new DefaultGenerator().opts(input).generate();
    }


}
