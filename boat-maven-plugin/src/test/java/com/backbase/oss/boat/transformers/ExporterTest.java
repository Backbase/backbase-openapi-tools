package com.backbase.oss.boat.transformers;

import com.backbase.oss.boat.ExportException;
import com.backbase.oss.boat.Exporter;
import com.backbase.oss.boat.serializer.SerializerUtils;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.parser.OpenAPIV3Parser;
import io.swagger.v3.parser.core.models.ParseOptions;
import io.swagger.v3.parser.core.models.SwaggerParseResult;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;


class ExporterTest extends AbstractBoatEngineTests {

    Logger log = LoggerFactory.getLogger(ExporterTest.class);

    @Test
    void testBackbaseWalletApi() throws Exception {

        File inputFile = getFile("/raml-examples/backbase-wallet/presentation-client-api.raml");
        OpenAPI openAPI = Exporter.export(inputFile, true, Collections.singletonList(new Decomposer()));
        String export = SerializerUtils.toYamlString(openAPI);
        validateExport(export);
    }


    protected SwaggerParseResult validateExport(String export) throws IOException, ExportException {
        if (export == null) {
            throw new ExportException("Invalid Export");
        }

        String child = "openapi.yaml";

        writeOutput(export, child);

        OpenAPIV3Parser openAPIParser = new OpenAPIV3Parser();
        ParseOptions parseOptions = new ParseOptions();
        SwaggerParseResult swaggerParseResult = openAPIParser.readContents(export, new ArrayList<>(), parseOptions);
        for (String message : swaggerParseResult.getMessages()) {
            log.error("Error parsing Open API: {}", message);
        }
        assertTrue(swaggerParseResult.getMessages().isEmpty());

        return swaggerParseResult;
    }


    protected File getFile(String name) {
        URL resource = getClass().getResource(name);
        assert resource != null;
        return new File(resource.getFile());
    }


}
