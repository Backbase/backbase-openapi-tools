package com.backbase.oss.codegen.java;

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
import lombok.SneakyThrows;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.BeforeAll;
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
            .filter(node -> node instanceof VariableDeclarator)
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
}
