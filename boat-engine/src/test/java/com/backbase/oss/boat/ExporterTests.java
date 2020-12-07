package com.backbase.oss.boat;

import com.backbase.oss.boat.serializer.SerializerUtils;
import com.backbase.oss.boat.transformers.Decomposer;
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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;


public class ExporterTests extends AbstractBoatEngineTestBase {

    Logger log = LoggerFactory.getLogger(ExporterTests.class);

    @Test
    public void testHelloWorld() throws Exception {
        File inputFile = getFile("/raml-examples/backbase-wallet/presentation-client-api.raml");
        OpenAPI openAPI = Exporter.export(inputFile, true, new ArrayList<>());
        String export = SerializerUtils.toYamlString(openAPI);
        validateExport(export);
    }

    @Test
    public void testUnusualTypesSchemaConversion() throws ExportException, IOException {
        File inputFile = getFile("/raml-examples/backbase-wallet/presentation-xml-client-api.raml");
        OpenAPI openAPI = Exporter.export(inputFile, true, new ArrayList<>());
        String export = SerializerUtils.toYamlString(openAPI);
        validateExport(export);
    }

    @Test
    public void normalizeNameTests() {
        String s = Utils.normalizeSchemaName("BatchUpload-GET-Response");
        assertEquals("BatchuploadGetResponse", s);
        s = Utils.normalizeSchemaName("batchUpload-GET-Response");
        assertEquals("BatchuploadGetResponse", s);
    }

    @Test
    public void testWalletPresentation() throws Exception {
        File inputFile = getFile("/raml-examples/backbase-wallet/presentation-client-api.raml");
        OpenAPI openAPI = Exporter.export(inputFile, new ExporterOptions()
            .addJavaTypeExtensions(true)
            .convertExamplesToYaml(true)
            .transformers(Collections.singletonList(new Decomposer())));
        String export = SerializerUtils.toYamlString(openAPI);
        SwaggerParseResult swaggerParseResult = validateExport(export);
        assertNotNull(swaggerParseResult.getOpenAPI().getPaths().get("/client-api/v1/wallet/paymentcards"));
        assertNotNull(swaggerParseResult.getOpenAPI().getPaths().get("/client-api/v1/wallet/paymentcards/{cardId}"));
        assertNotNull(swaggerParseResult.getOpenAPI().getPaths().get("/client-api/v1/patch"));
    }

    @Test
    public void testWalletPresentationMissingRef() {
        File inputFile = getFile("/raml-examples/backbase-wallet/presentation-service-api-invalid-missing-ref.raml");
        assertThrows(ExportException.class,() -> Exporter.export(inputFile, new ExporterOptions()
            .addJavaTypeExtensions(true)
            .convertExamplesToYaml(true)
            .transformers(Collections.singletonList(new Decomposer()))));
    }

    @Test
    public void testWalletPresentationInvalidRef() throws Exception {
        File inputFile = getFile("/raml-examples/backbase-wallet/presentation-service-api-invalid-ref.raml");
        assertThrows(ExportException.class,() -> Exporter.export(inputFile, new ExporterOptions()
            .addJavaTypeExtensions(true)
            .convertExamplesToYaml(true)
            .transformers(Collections.singletonList(new Decomposer()))));
    }

    @Test
    public void testWalletIntegration() throws Exception {
        File inputFile = getFile("/raml-examples/backbase-wallet/presentation-integration-api.raml");
        OpenAPI openAPI = Exporter.export(inputFile, new ExporterOptions()
            .addJavaTypeExtensions(true)
            .convertExamplesToYaml(true)
            .transformers(Collections.singletonList(new Decomposer())));
        String export = SerializerUtils.toYamlString(openAPI);
        SwaggerParseResult swaggerParseResult = validateExport(export);
        assertNotNull(swaggerParseResult.getOpenAPI().getPaths().get("/integration-api/v1/items"));
    }

    @Test
    public void testWalletService() throws Exception {
        File inputFile = getFile("/raml-examples/backbase-wallet/presentation-service-api.raml");
        OpenAPI openAPI = Exporter.export(inputFile, new ExporterOptions()
            .addJavaTypeExtensions(true)
            .convertExamplesToYaml(true)
            .transformers(Collections.singletonList(new Decomposer())));
        String export = SerializerUtils.toYamlString(openAPI);
        SwaggerParseResult swaggerParseResult = validateExport(export);
        assertNotNull(
            swaggerParseResult.getOpenAPI().getPaths().get("/service-api/v1/wallet/admin/{userId}/paymentcards"));
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
