package com.backbase.oss.codegen.java;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;
import io.swagger.parser.OpenAPIParser;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.parser.core.models.ParseOptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.openapitools.codegen.CliOption;
import org.openapitools.codegen.ClientOptInput;
import org.openapitools.codegen.DefaultGenerator;
import org.openapitools.codegen.config.GlobalSettings;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Stream;

import static com.backbase.oss.codegen.java.BoatJavaCodeGen.*;
import static java.util.stream.Collectors.groupingBy;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

class BoatJavaCodeGenTests {

    static final String PROP_BASE = BoatJavaCodeGenTests.class.getSimpleName() + ".";
    static final String TEST_OUTPUT = System.getProperty(PROP_BASE + "output", "target/boat-java-codegen-tests");
    @Test
    void clientOptsUnicity() {
        final BoatJavaCodeGen gen = new BoatJavaCodeGen();
        gen.cliOptions()
            .stream()
            .collect(groupingBy(CliOption::getOpt))
            .forEach((k, v) -> assertEquals(1, v.size(), k + " is described multiple times"));
    }

    @Test
    void processOptsWithRestTemplateDefaults() {
        final BoatJavaCodeGen gen = new BoatJavaCodeGen();

        gen.setLibrary("resttemplate");
        gen.processOpts();

        assertThat(gen.useWithModifiers, is(false));
        assertThat(gen.useClassLevelBeanValidation, is(false));

        assertThat(gen.useJacksonConversion, is(false));
        assertThat(gen.useDefaultApiClient, is(true));
        assertThat(gen.restTemplateBeanName, is(nullValue()));
        assertThat(gen.createApiComponent, is(true));
    }

    @Test
    void processOptsWithRestTemplate() {
        final BoatJavaCodeGen gen = new BoatJavaCodeGen();
        final Map<String, Object> options = gen.additionalProperties();

        gen.setLibrary("resttemplate");
        options.put(USE_WITH_MODIFIERS, "true");
        options.put(USE_CLASS_LEVEL_BEAN_VALIDATION, "true");

        options.put(USE_JACKSON_CONVERSION, "true");
        options.put(USE_DEFAULT_API_CLIENT, "false");
        options.put(REST_TEMPLATE_BEAN_NAME, "the-coolest-rest-template-in-this-universe");
        options.put(CREATE_API_COMPONENT, "false");

        gen.processOpts();

        assertThat(gen.useWithModifiers, is(true));
        assertThat(gen.useClassLevelBeanValidation, is(true));

        assertThat(gen.useJacksonConversion, is(true));
        assertThat(gen.useDefaultApiClient, is(false));
        assertThat(gen.restTemplateBeanName, is("the-coolest-rest-template-in-this-universe"));
        assertThat(gen.createApiComponent, is(false));
    }

    @Test
    void processOptsWithoutRestTemplate() {
        final BoatJavaCodeGen gen = new BoatJavaCodeGen();
        final Map<String, Object> options = gen.additionalProperties();

        options.put(USE_WITH_MODIFIERS, "true");
        options.put(USE_CLASS_LEVEL_BEAN_VALIDATION, "true");

        options.put(USE_JACKSON_CONVERSION, "true");
        options.put(USE_DEFAULT_API_CLIENT, "false");
        options.put(REST_TEMPLATE_BEAN_NAME, "the-coolest-rest-template-in-this-universe");

        gen.processOpts();

        assertThat(gen.useWithModifiers, is(true));
        assertThat(gen.useClassLevelBeanValidation, is(false));

        assertThat(gen.useJacksonConversion, is(false));
        assertThat(gen.useDefaultApiClient, is(true));
        assertThat(gen.restTemplateBeanName, is(nullValue()));
    }

    @Test
    void processOptsUseProtectedFields() {
        final BoatJavaCodeGen gen = new BoatJavaCodeGen();
        final Map<String, Object> options = gen.additionalProperties();

        options.put(USE_PROTECTED_FIELDS, "true");

        gen.processOpts();

        assertThat(gen.additionalProperties(), hasEntry("modelFieldsVisibility", "protected"));
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void shouldHonourGenerateComponentAnnotation(boolean generate) throws FileNotFoundException {

        var input = new File("src/test/resources/boat-spring/openapi.yaml");
        var output = TEST_OUTPUT + "/shouldNotGenerateComponentAnnotation/" + generate;

        final BoatJavaCodeGen gen = new BoatJavaCodeGen();
        gen.setOutputDir(output);
        gen.setInputSpec(input.getAbsolutePath());
        gen.setApiPackage("com.backbase.test.api");
        gen.setModelPackage("com.backbase.test.api.model");
        gen.setInvokerPackage("com.backbase.test.api.invoker");
        gen.setApiNameSuffix("ApiClient");

        final Map<String, Object> options = gen.additionalProperties();
        options.put("library", "resttemplate");
        options.put(CREATE_API_COMPONENT, String.valueOf(generate));

        var openApiInput = new OpenAPIParser()
                .readLocation(input.getAbsolutePath(), null, new ParseOptions())
                .getOpenAPI();
        var clientOptInput = new ClientOptInput();
        clientOptInput.config(gen);
        clientOptInput.openAPI(openApiInput);

        List<File> files = new DefaultGenerator().opts(clientOptInput).generate();

        Function<String, File> getFileByName = (String fileName) -> files.stream()
            .filter(file -> file.getName().equals(fileName))
            .findFirst()
            .get();

        File apiFile = getFileByName.apply("PaymentsApiClient.java");
        TypeDeclaration apiType = StaticJavaParser.parse(apiFile)
                .findFirst(TypeDeclaration.class).get();
        assertThat(apiType.getAnnotationByName("Component").isPresent(), is(generate));

        File apiClientFile = getFileByName.apply("ApiClient.java");
        TypeDeclaration apiClientType = StaticJavaParser.parse(apiClientFile)
            .findFirst(TypeDeclaration.class).get();
        assertThat(apiClientType.getAnnotationByName("Component").isPresent(), is(generate));

        assertThat(gen.createApiComponent, is(generate));
        assertThat(gen.getLibrary(), is("resttemplate"));
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void shouldHonourBeanValidationOption(boolean useBeanValidation) throws FileNotFoundException {

        var input = new File("src/test/resources/boat-spring/openapi.yaml");
        var output = TEST_OUTPUT + "/shouldHonourBeanValidationOption/" + useBeanValidation;

        final BoatJavaCodeGen gen = new BoatJavaCodeGen();
        gen.setOutputDir(output);
        gen.setInputSpec(input.getAbsolutePath());
        gen.setApiPackage("com.backbase.test.api");
        gen.setModelPackage("com.backbase.test.api.model");
        gen.setInvokerPackage("com.backbase.test.api.invoker");
        gen.setApiNameSuffix("ApiClient");

        final Map<String, Object> options = gen.additionalProperties();
        options.put("library", "resttemplate");
        options.put("useBeanValidation", String.valueOf(useBeanValidation));

        var openApiInput = new OpenAPIParser()
                .readLocation(input.getAbsolutePath(), null, new ParseOptions())
                .getOpenAPI();
        var clientOptInput = new ClientOptInput();
        clientOptInput.config(gen);
        clientOptInput.openAPI(openApiInput);

        List<File> files = new DefaultGenerator().opts(clientOptInput).generate();

        Function<String, File> getFileByName = (String fileName) -> files.stream()
                .filter(file -> file.getName().equals(fileName))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("File name found:" + fileName));

        File apiFile = getFileByName.apply("ValidatedPojosApiClient.java");
        CompilationUnit compilationUnit = StaticJavaParser.parse(apiFile);
        MethodDeclaration getPojosMethod = compilationUnit
                .findFirst(MethodDeclaration.class, m -> "getPojos".equals(m.getNameAsString())).get();
        assertThat("Expect Valid annotation.", getPojosMethod.getParameter(0).getType().toString().contains("@Valid"), is(useBeanValidation));
        assertThat("Expect jakarta Valid import", compilationUnit.getImports().stream().anyMatch(
                id -> id.getNameAsString().equals("jakarta.validation.Valid")), is(useBeanValidation));
    }

    @ParameterizedTest
    @MethodSource("testTypeDeclarationValues")
    public void testTypeDeclaration(boolean useBeanValidation, String schemaType,
                                    String itemType, boolean itemEnum, String expected) {

        // Setup
        BoatJavaCodeGen gen = new BoatJavaCodeGen();
        gen.setUseBeanValidation(useBeanValidation);
        GlobalSettings.setProperty("generateAliasAsModel", "true");

        Schema schema = new Schema();
        schema.type(schemaType);
        if (itemType != null) {
            Schema itemSchema = new Schema();
            itemSchema.type(itemType);
            if (itemEnum) {
                itemSchema.setEnum(List.of("1", "2"));
            }
            schema.setItems(itemSchema);
        }

        String s = gen.getTypeDeclaration(schema);
        assertEquals(expected, s);
    }

    static Stream<Arguments> testTypeDeclarationValues() {
        return Stream.of(
                Arguments.of(true, "string", null, false, "String"),
                Arguments.of(false, "string", null, false, "String"),
                Arguments.of(true, "array", "string", false, "List<String>"),
                Arguments.of(true, "array", "string", true, "List<@Valid String>"),
                Arguments.of(true, "array", "object", false, "List<Object>"),
                Arguments.of(true, "array", null, false, "List<String>") // this case logs an ERROR
        );
    }
}
