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
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

import org.junit.jupiter.api.Test;
import org.raml.yagi.framework.util.DateType;
import org.raml.yagi.framework.util.DateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.jupiter.api.Assertions.*;


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
    public void testInvalidFile() {
        File inputFile = getFile("/raml-examples/invalid-ramls/empty.raml");
        assertThrows(ExportException.class, () -> Exporter.export(inputFile, new ExporterOptions()
                .addJavaTypeExtensions(true)
                .convertExamplesToYaml(true)
                .transformers(Collections.singletonList(new Decomposer()))));
    }

    @Test
    public void testWrongTypes() {
        File inputFile = getFile("/raml-examples/invalid-ramls/invalid-types.raml");
        assertThrows(ExportException.class, () -> Exporter.export(inputFile, new ExporterOptions()
                .addJavaTypeExtensions(true)
                .convertExamplesToYaml(true)
                .transformers(Collections.singletonList(new Decomposer()))));
    }

    @Test
    public void testMarkupDocumentation() throws IOException, ExportException {
        File inputFile = getFile("/raml-examples/backbase-wallet/presentation-markup-documentation-client.raml");
        OpenAPI openAPI = Exporter.export(inputFile, true, new ArrayList<>());
        String export = SerializerUtils.toYamlString(openAPI);
        validateExport(export);
    }

    @Test
    public void testWalletPresentationMissingRef() {
        File inputFile = getFile("/raml-examples/backbase-wallet/presentation-service-api-invalid-missing-ref.raml");
        assertThrows(ExportException.class, () -> Exporter.export(inputFile, new ExporterOptions()
                .addJavaTypeExtensions(true)
                .convertExamplesToYaml(true)
                .transformers(Collections.singletonList(new Decomposer()))));
    }

    @Test
    public void testWalletPresentationInvalidRef() throws Exception {
        File inputFile = getFile("/raml-examples/backbase-wallet/presentation-service-api-invalid-ref.raml");
        assertThrows(ExportException.class, () -> Exporter.export(inputFile, new ExporterOptions()
                .addJavaTypeExtensions(true)
                .convertExamplesToYaml(true)
                .transformers(Collections.singletonList(new Decomposer()))));
    }

    @Test
    public void testDateFormat() {
        Locale.setDefault(Locale.ENGLISH);
        TimeZone.setDefault(TimeZone.getTimeZone("Europe/London"));

        Date date = new Date();
        SimpleDateFormat usDateFormat = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.US);
        SimpleDateFormat nlDateFormat = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.forLanguageTag("nl"));
        SimpleDateFormat systemDateFormat = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z");
        String usDate = usDateFormat.format(date);
        String nlDate = nlDateFormat.format(date);
        String systemDate = systemDateFormat.format(date);



        System.out.println("US      : " + usDate);
        System.out.println("NL      : " + nlDate);
        System.out.println("System  : " + systemDate);


        DateUtils strictDateUtils = DateUtils.createStrictDateUtils();
        assertTrue(strictDateUtils.isValidDate(usDate, DateType.datetime, "rfc2616"), usDate);
        assertFalse(strictDateUtils.isValidDate(nlDate, DateType.datetime, "rfc2616"), nlDate);
        assertTrue(strictDateUtils.isValidDate(systemDate, DateType.datetime, "rfc2616"), systemDate);

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
