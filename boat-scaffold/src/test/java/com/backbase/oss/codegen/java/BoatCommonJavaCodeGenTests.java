package com.backbase.oss.codegen.java;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.openapitools.codegen.languages.JavaClientCodegen.APACHE;
import static org.openapitools.codegen.languages.JavaClientCodegen.RESTTEMPLATE;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import io.swagger.parser.OpenAPIParser;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.parser.core.models.ParseOptions;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;
import lombok.SneakyThrows;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.openapitools.codegen.ClientOptInput;
import org.openapitools.codegen.DefaultGenerator;
import org.openapitools.codegen.languages.AbstractJavaCodegen;

class BoatCommonJavaCodeGenTests {

    static final String PROP_BASE = BoatCommonJavaCodeGenTests.class.getSimpleName() + ".";
    static final String TEST_OUTPUT = System.getProperty(PROP_BASE + "output",
        "target/" + BoatCommonJavaCodeGenTests.class.getSimpleName());

    @BeforeAll
    static void before() throws IOException {
        Files.createDirectories(Paths.get(TEST_OUTPUT));
        FileUtils.deleteDirectory(new File(TEST_OUTPUT));
    }

    @ParameterizedTest
    @ValueSource(strings = {"true", "false", ""})
    void shouldGenerateArrayField(String containerDefaultToNull) {
        generateAndAssert(javaCodeGenWithLib(RESTTEMPLATE), containerDefaultToNull);
        generateAndAssert(javaCodeGenWithLib(APACHE), containerDefaultToNull);
        generateAndAssert(springCodeGen(), containerDefaultToNull);
    }

    @Test
    void shouldExplodePojoQueryParam() throws IOException {

        BoatJavaCodeGen codegen = javaCodeGenWithLib(RESTTEMPLATE);

        var input = new File("src/test/resources/boat-spring/openapi.yaml");
        var outputDir = TEST_OUTPUT + "/shouldExplodePojoQueryParam";

        codegen.setInputSpec(input.getAbsolutePath());
        codegen.setOutputDir(outputDir);

        OpenAPI openApiInput = new OpenAPIParser()
            .readLocation(input.getAbsolutePath(), null, new ParseOptions())
            .getOpenAPI();
        var clientOptInput = new ClientOptInput();
        clientOptInput.config(codegen);
        clientOptInput.openAPI(openApiInput);

        List<File> files = new DefaultGenerator().opts(clientOptInput).generate();

        File pojosInQueryApi = files.stream()
            .filter(it -> it.getName().equals("PojosInQueryApi.java"))
            .findFirst()
            .orElseThrow();

        List<String> lines = Files.readAllLines(pojosInQueryApi.toPath()).stream()
            .map(String::trim)
            .collect(Collectors.toList());

        assertThat(lines)
            .contains("public ResponseEntity<Void> getWithPojosInQueryWithHttpInfo(String simpleParam, MyPojo pojoParam) throws RestClientException {")
            .contains("localVarQueryParams.putAll(apiClient.parameterToMultiValueMap(null, \"simpleParam\", simpleParam));")
            .contains("localVarQueryParams.putAll(apiClient.parameterToMultiValueMap(null, \"field1\", pojoParam.getField1()));")
            .contains("localVarQueryParams.putAll(apiClient.parameterToMultiValueMap(null, \"field2\", pojoParam.getField2()));");
    }

    private static BoatJavaCodeGen javaCodeGenWithLib(String library) {
        var codeGen = new BoatJavaCodeGen();
        codeGen.setLibrary(library);
        return codeGen;
    }

    private static BoatSpringCodeGen springCodeGen() {
        var codeGen = new BoatSpringCodeGen();
        codeGen.setLibrary("spring-boot");
        return codeGen;
    }

    void generateAndAssert(AbstractJavaCodegen codegen, String containerDefaultToNull) {

        var input = new File("src/test/resources/boat-spring/openapi.yaml");
        var outputDir = TEST_OUTPUT + String.format("/generateAndAssert_%s_%s_%s", codegen.getClass().getSimpleName(),
            codegen.getLibrary(), containerDefaultToNull);

        codegen.setInputSpec(input.getAbsolutePath());
        codegen.setOutputDir(outputDir);

        OpenAPI openApiInput = new OpenAPIParser()
            .readLocation(input.getAbsolutePath(), null, new ParseOptions())
            .getOpenAPI();
        var clientOptInput = new ClientOptInput();
        clientOptInput.config(codegen);
        clientOptInput.openAPI(openApiInput);
        if (StringUtils.isNotBlank(containerDefaultToNull)) {
            clientOptInput.getConfig().additionalProperties().put("containerDefaultToNull", containerDefaultToNull);
        }

        List<File> files = new DefaultGenerator().opts(clientOptInput).generate();

        CompilationUnit multilinePaymentRequest = files.stream()
            .filter(it -> it.getName().equals("MultiLinePaymentRequest.java"))
            .findFirst()
            .map(file -> {
                try {
                    return StaticJavaParser.parse(file);
                } catch (FileNotFoundException e) {
                    throw new RuntimeException(e);
                }
            })
            .orElseThrow();

        if (Boolean.parseBoolean(containerDefaultToNull)) {
            // containerDefaultToNull=true
            assertVariableDeclarator(multilinePaymentRequest, "lines", "ArrayList");
            assertVariableDeclarator(multilinePaymentRequest, "uniqueLines", "LinkedHashSet");
            assertVariableDeclarator(multilinePaymentRequest, "optionalLines", ""); // optional field, therefore null
            assertVariableDeclarator(multilinePaymentRequest, "optionalUniqueLines", ""); // optional field, therefore null
        } else {
            // containerDefaultToNull=false or default behaviour
            assertVariableDeclarator(multilinePaymentRequest, "lines", "ArrayList");
            assertVariableDeclarator(multilinePaymentRequest, "uniqueLines", "LinkedHashSet");
            assertVariableDeclarator(multilinePaymentRequest, "optionalLines", "ArrayList");
            assertVariableDeclarator(multilinePaymentRequest, "optionalUniqueLines", "LinkedHashSet");
        }
    }

    @SneakyThrows
    private void assertVariableDeclarator(CompilationUnit requestClass, String fieldName, String declarationType) {
        VariableDeclarator listDeclarator = requestClass
            .findAll(FieldDeclaration.class)
            .stream()
            .flatMap(field -> field.getChildNodes().stream())
            .filter(VariableDeclarator.class::isInstance)
            .map(VariableDeclarator.class::cast)
            .filter(declarator -> declarator.getName().getIdentifier().equals(fieldName))
            .findFirst()
            .orElseThrow();

        if (StringUtils.isNotBlank(declarationType)) {
            assertTrue(listDeclarator.getInitializer().isPresent());
            assertEquals(listDeclarator.getInitializer().get().toString(),
                String.format("new %s<>()", declarationType));
        } else {
            assertFalse(listDeclarator.getInitializer().isPresent());
        }
    }

    @Test
    void shouldGenerateDeprecationAnnotationsFromSpecLevel() throws IOException {
        var modelPackage = "com.backbase.model";
        var apiPackage = "com.backbase.api";
        var input = new File("src/test/resources/boat-spring/deprecated-spec.yaml");
        var output = TEST_OUTPUT + "/shouldGenerateDeprecationAnnotationsFromSpecLevel";

        var codegen = new BoatJavaCodeGen();
        codegen.setOutputDir(output);
        codegen.setInputSpec(input.getAbsolutePath());
        codegen.setModelPackage(modelPackage);
        codegen.setApiPackage(apiPackage);

        var openApiInput = new OpenAPIParser()
            .readLocation(input.getAbsolutePath(), null, new ParseOptions())
            .getOpenAPI();
        var clientOptInput = new ClientOptInput();
        clientOptInput.config(codegen);
        clientOptInput.openAPI(openApiInput);

        List<File> files = new DefaultGenerator().opts(clientOptInput).generate();

        // Verify API class has @Deprecated and deprecation message
        File apiFile = files.stream().filter(file -> file.getName().equals("ItemsApi.java"))
            .findFirst()
            .orElseThrow();
        String apiContent = Files.readString(apiFile.toPath());
        assertTrue(apiContent.contains("@Deprecated"), "API should have @Deprecated annotation");
        assertTrue(apiContent.contains("@deprecated"), "API should have @deprecated Javadoc tag");
        assertTrue(apiContent.contains("will be removed on 2026-12-31"), "API should have sunset date in message");

        // Verify model with deprecated property
        File itemFile = files.stream().filter(file -> file.getName().equals("Item.java"))
            .findFirst()
            .orElseThrow();
        String itemContent = Files.readString(itemFile.toPath());
        assertTrue(itemContent.contains("@Deprecated"), "Item model should have @Deprecated annotation");
        assertTrue(itemContent.contains("will be removed on 2026-12-31"), "Item model should have sunset date in message");
    }
}
