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
import java.util.Locale;
import java.util.TimeZone;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.jupiter.api.Assertions.*;


class ExporterTests extends AbstractBoatEngineTestBase {

    Logger log = LoggerFactory.getLogger(ExporterTests.class);

    @BeforeAll
    static void setupLocale() {
        Locale.setDefault(Locale.ENGLISH);
        TimeZone.setDefault(TimeZone.getTimeZone("Europe/London"));
    }

    @Test
    void testHelloWorld() throws Exception {
        File inputFile = getFile("/raml-examples/backbase-wallet/presentation-client-api.raml");
        OpenAPI openAPI = Exporter.export(inputFile, true, new ArrayList<>());
        String export = SerializerUtils.toYamlString(openAPI);
        validateExport(export);
    }

    @Test
    void testUnusualTypesSchemaConversion() throws ExportException, IOException {
        File inputFile = getFile("/raml-examples/backbase-wallet/presentation-xml-client-api.raml");
        OpenAPI openAPI = Exporter.export(inputFile, true, new ArrayList<>());
        String export = SerializerUtils.toYamlString(openAPI);
        validateExport(export);
    }

    @Test
    void testDuplicateOperation() throws ExportException, IOException {
        File inputFile = getFile("/raml-examples/backbase-wallet/presentation-client-api-duplicate-operation-id.raml");
        OpenAPI openAPI = Exporter.export(inputFile, true, new ArrayList<>());
        String export = SerializerUtils.toYamlString(openAPI);
        validateExport(export);
    }


    @Test
    void normalizeNameTests() {
        String s = Utils.normalizeSchemaName("BatchUpload-GET-Response");
        assertEquals("BatchuploadGetResponse", s);
        s = Utils.normalizeSchemaName("batchUpload-GET-Response");
        assertEquals("BatchuploadGetResponse", s);
    }

    @Test
    void testWalletPresentation() throws Exception {
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
    void testMarkupDocumentation() throws IOException, ExportException {
        File inputFile = getFile("/raml-examples/backbase-wallet/presentation-markup-documentation-client.raml");
        OpenAPI openAPI = Exporter.export(inputFile, true, new ArrayList<>());
        String export = SerializerUtils.toYamlString(openAPI);
        validateExport(export);

         String cleanmarkup = Exporter.cleanupMarkdownString(markup());
         assertFalse(cleanmarkup.startsWith("##"));
    }

    private String markup(){
        return "## Innovation Layer / Bi-Modal\n" +
                "    Web API facade on to Alainn's SOA Architecture. Designed to be the central point of access to all business process level SOAP services.\n" +
                "\n" +
                "    # Resource Hierarchy\n" +
                "\n" +
                "    The resources are in part independent of the user browsing them\n" +
                "\n" +
                "    * /items\n" +
                "    * /brands";
    }

    @ParameterizedTest
    @CsvSource( {
            "/raml-examples/invalid-ramls/empty.raml, Error validation RAML",
            "/raml-examples/invalid-ramls/invalid-types.raml, Error validation RAML",
            "/raml-examples/backbase-wallet/presentation-service-api-invalid-missing-ref.raml, Cannot dereference json schema from type: application/json",
            "/raml-examples/backbase-wallet/presentation-service-api-invalid-ref.raml, Cannot dereference json schema from type: application/json"
    })
    void testErrorCatching(String inputFileName, String expected) {
        File inputFile = getFile(inputFileName);

        try {
            Exporter.export(inputFile, new ExporterOptions()
                    .addJavaTypeExtensions(true)
                    .convertExamplesToYaml(true)
                    .transformers(Collections.singletonList(new Decomposer())));
            fail("expected ExportException to be thrown");
        } catch (ExportException e){
            assertEquals(expected,e.getMessage());
        }
    }

    @Test
    void testWalletIntegration() throws Exception {
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
    void testWalletService() throws Exception {
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


    @ParameterizedTest
    @ValueSource(strings = {"/raml-examples/others/banking-api/api.raml",
            "/raml-examples/backbase-wallet/presentation-client-api.raml",
            "/raml-examples/backbase-wallet/presentation-client-api-extension.raml"})
    void testExportWithDifferentApis(String apiFile) throws Exception {

        File inputFile = getFile(apiFile);
        OpenAPI openAPI = Exporter.export(inputFile, true, Collections.singletonList(new Decomposer()));
        String export = SerializerUtils.toYamlString(openAPI);
        validateExport(export);
    }


    @ParameterizedTest
    @CsvSource({
            "/openapi/error-catching/bad-export-api/openapi.yaml, Error validation RAML",
            "/raml-examples/invalid-ramls, Failed to export ramlTypes"
    })
    void testExportErrorCatching(String fileName, String expected) throws IOException {
        File inputFile = getFile(fileName);
        try{
            Exporter.export(inputFile, true, Collections.singletonList(new Decomposer()));
            fail("expected ExportExpectation to be thrown");
        }catch (ExportException e){
            assertEquals(expected,e.getMessage());
        }

    }

//    void testExporterBodyConversion(){
//        new Exporter().convertExamples();
//    }

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
