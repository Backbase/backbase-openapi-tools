package com.backbase.oss.codegen.java;

import static com.backbase.oss.codegen.java.BoatJavaCodeGen.REST_TEMPLATE_BEAN_NAME;
import static com.backbase.oss.codegen.java.BoatJavaCodeGen.USE_JACKSON_CONVERSION;
import static java.util.stream.Collectors.groupingBy;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.openapitools.codegen.languages.JavaClientCodegen.GENERATE_CLIENT_AS_BEAN;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.BodyDeclaration;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.stmt.ReturnStmt;
import com.github.javaparser.ast.stmt.Statement;
import io.swagger.parser.OpenAPIParser;
import io.swagger.v3.parser.core.models.ParseOptions;
import java.io.File;
import java.io.FileNotFoundException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.openapitools.codegen.CliOption;
import org.openapitools.codegen.ClientOptInput;
import org.openapitools.codegen.DefaultGenerator;
import org.openapitools.codegen.config.CodegenConfigurator;

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

        assertThat(gen.useJacksonConversion, is(false));
        assertThat(gen.restTemplateBeanName, is(nullValue()));
    }

    @Test
    void processOptsWithRestTemplate() {
        final BoatJavaCodeGen gen = new BoatJavaCodeGen();
        final Map<String, Object> options = gen.additionalProperties();

        gen.setLibrary("resttemplate");

        options.put(USE_JACKSON_CONVERSION, "true");
        options.put(REST_TEMPLATE_BEAN_NAME, "the-coolest-rest-template-in-this-universe");

        gen.processOpts();

        assertThat(gen.useJacksonConversion, is(true));
        assertThat(gen.restTemplateBeanName, is("the-coolest-rest-template-in-this-universe"));
    }

    @Test
    void processOptsWithoutRestTemplate() {
        final BoatJavaCodeGen gen = new BoatJavaCodeGen();
        final Map<String, Object> options = gen.additionalProperties();

        options.put(USE_JACKSON_CONVERSION, "true");
        options.put(REST_TEMPLATE_BEAN_NAME, "the-coolest-rest-template-in-this-universe");

        gen.processOpts();

        assertThat(gen.useJacksonConversion, is(false));
        assertThat(gen.restTemplateBeanName, is(nullValue()));
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
        options.put(GENERATE_CLIENT_AS_BEAN, String.valueOf(generate));

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

    @Test
    void shouldGenerateBackwardCompatibleSingleRequestParameterOverloads(@TempDir Path temporaryDirectory) throws FileNotFoundException {
        ClassOrInterfaceDeclaration api = generateRestTemplateClient(
            temporaryDirectory.resolve("generated-enabled"),
            true
        );

        ClassOrInterfaceDeclaration parameters = findNestedClass(api, "ListPetsParam").orElseThrow();
        assertTrue(parameters.isStatic());
        assertTrue(findNestedClass(api, "ShowPetByIdParam").isEmpty());

        findMethod(api, "listPets", "Integer", "String");
        findMethod(api, "listPetsWithHttpInfo", "Integer", "String");

        MethodDeclaration listPets = findMethod(api, "listPets", "ListPetsParam");
        assertEquals("listPetsWithHttpInfo(params).getBody()", returnExpression(listPets));

        MethodDeclaration listPetsWithHttpInfo = findMethod(api, "listPetsWithHttpInfo", "ListPetsParam");
        assertEquals(
            "listPetsWithHttpInfo(params.getLimit(), params.getStatus())",
            returnExpression(listPetsWithHttpInfo)
        );
    }

    @Test
    void shouldNotGenerateSingleRequestParameterOverloadsByDefault(
        @TempDir Path temporaryDirectory
    ) throws FileNotFoundException {
        ClassOrInterfaceDeclaration api = generateRestTemplateClient(
            temporaryDirectory.resolve("generated-disabled"),
            false
        );

        assertFalse(findNestedClass(api, "ListPetsParam").isPresent());
        assertEquals(1, api.getMethodsByName("listPets").size());
        assertEquals(1, api.getMethodsByName("listPetsWithHttpInfo").size());
        findMethod(api, "listPets", "Integer", "String");
        findMethod(api, "listPetsWithHttpInfo", "Integer", "String");
    }

    private ClassOrInterfaceDeclaration generateRestTemplateClient(Path outputDirectory, boolean useSingleRequestParameter)
        throws FileNotFoundException {
        CodegenConfigurator configurator = getCodegenConfigurator(outputDirectory);

        if (useSingleRequestParameter) {
            configurator.addAdditionalProperty("useSingleRequestParameter", true);
        }

        File generatedApi = new DefaultGenerator()
            .opts(configurator.toClientOptInput())
            .generate()
            .stream()
            .filter(file -> file.getName().equals("PetsApi.java"))
            .findFirst()
            .orElseThrow();

        return StaticJavaParser.parse(generatedApi)
            .getClassByName("PetsApi")
            .orElseThrow();
    }

    private CodegenConfigurator getCodegenConfigurator(Path outputDirectory) {
        CodegenConfigurator configurator = new CodegenConfigurator();
        configurator.setGeneratorName("boat-java");
        configurator.setLibrary("resttemplate");
        configurator.setInputSpec(
            getFile("/boat-java/petstore-single-request-parameter.yaml")
                .getAbsolutePath()
        );
        configurator.setOutputDir(outputDirectory.toAbsolutePath().toString());
        configurator.setApiPackage("com.example.api");
        configurator.setModelPackage("com.example.model");
        return configurator;
    }

    private static MethodDeclaration findMethod(ClassOrInterfaceDeclaration api, String name, String... parameterTypes) {
        List<MethodDeclaration> methods = api.getMethodsBySignature(name, parameterTypes);

        assertEquals(1, methods.size(),
            () -> "Expected exactly one method " + name + List.of(parameterTypes) + ", but found " + methods.size()
        );

        return methods.get(0);
    }

    private static Optional<ClassOrInterfaceDeclaration> findNestedClass(ClassOrInterfaceDeclaration api, String name) {
        return api.getMembers()
            .stream()
            .filter(BodyDeclaration::isClassOrInterfaceDeclaration)
            .map(BodyDeclaration::asClassOrInterfaceDeclaration)
            .filter(type -> type.getNameAsString().equals(name))
            .findFirst();
    }

    private static String returnExpression(MethodDeclaration method) {
        return method.getBody()
            .orElseThrow()
            .getStatements()
            .stream()
            .filter(Statement::isReturnStmt)
            .map(Statement::asReturnStmt)
            .map(ReturnStmt::getExpression)
            .flatMap(Optional::stream)
            .map(Object::toString)
            .findFirst()
            .orElseThrow(() -> new AssertionError(
                "No direct return statement found in " + method.getSignature()
            ));
    }

    private File getFile(String fileName) {
        return new File(getClass().getResource(fileName).getFile());
    }
}
