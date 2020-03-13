package com.backbase.oss.boat;

import com.backbase.oss.boat.serializer.SerializerUtils;
import com.backbase.oss.boat.transformers.Decomposer;
import io.swagger.parser.OpenAPIParser;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.parser.core.models.ParseOptions;
import io.swagger.v3.parser.core.models.SwaggerParseResult;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.rmi.server.ExportException;
import java.util.ArrayList;
import java.util.Collections;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ExporterTest {

    Logger log = LoggerFactory.getLogger(ExporterTest.class);

    @Test
    public void testHelloWorld() throws Exception {
        File inputFile = getFile("/raml-examples/helloworld/helloworld.raml");
        OpenAPI openAPI = Exporter.export(inputFile, true, Collections.singletonList(new Decomposer()));
        String export = SerializerUtils.toYamlString(openAPI);
        validateExport(export);
    }

    @Test
    public void testWallet() throws Exception {
        File inputFile = getFile("/raml-examples/backbase-wallet/presentation-client-api.raml");
        OpenAPI openAPI = Exporter.export(inputFile, new ExporterOptions()
                .addJavaTypeExtensions(true)
                .convertExamplesToYaml(false)
                .transformers(Collections.singletonList(new Decomposer())));
        String export = SerializerUtils.toYamlString(openAPI);
        validateExport(export);
    }


    @Test
    public void testBankingApi() throws Exception {

        File inputFile = getFile("/raml-examples/others/banking-api/api.raml");
        OpenAPI openAPI = Exporter.export(inputFile, true, Collections.singletonList(new Decomposer()));
        String export = SerializerUtils.toYamlString(openAPI);
        validateExport(export);
    }

    @Test
    public void testBackbaseWalletApi() throws Exception {

        File inputFile = getFile("/raml-examples/backbase-wallet/presentation-client-api.raml");
        OpenAPI openAPI = Exporter.export(inputFile, true, Collections.singletonList(new Decomposer()));
        String export = SerializerUtils.toYamlString(openAPI);
        validateExport(export);
    }


    protected void validateExport(String export) throws IOException, com.backbase.oss.boat.ExportException {
        if (export == null)
            throw new ExportException("Invalid Export");

        String child = "openapi.yaml";

        writeOutput(export, child);

        OpenAPIParser openAPIParser = new OpenAPIParser();
        ParseOptions parseOptions = new ParseOptions();
        parseOptions.setFlatten(true);
        SwaggerParseResult swaggerParseResult = openAPIParser.readContents(export, new ArrayList<>(), parseOptions);
        for (String message : swaggerParseResult.getMessages()) {
            log.error("Error parsing Open API: {}", message);
        }
        Assert.assertTrue(swaggerParseResult.getMessages().isEmpty());

    }

    protected void writeOutput(String yaml, String fileName) throws IOException {
        File directory = new File("target");
        directory.mkdirs();
        File file = new File(directory, fileName);
        file.delete();
        file.createNewFile();
        Path path = file.toPath();
        Files.write(path, yaml.getBytes());
    }

    protected File getFile(String name) {
        URL resource = getClass().getResource(name);
        assert resource != null;
        return new File(resource.getFile());
    }


}
