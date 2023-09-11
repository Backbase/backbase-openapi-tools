package com.backbase.oss.codegen.java;

import static com.backbase.oss.codegen.java.BoatSpringCodeGen.USE_PROTECTED_FIELDS;
import static java.util.stream.Collectors.groupingBy;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasEntry;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.backbase.oss.codegen.java.BoatSpringCodeGen.NewLineIndent;
import com.backbase.oss.codegen.java.VerificationRunner.Verification;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.type.Type;
import com.samskivert.mustache.Template.Fragment;
import io.swagger.parser.OpenAPIParser;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.parser.core.models.ParseOptions;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.UnhandledException;
import org.hamcrest.Matchers;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.openapitools.codegen.CliOption;
import org.openapitools.codegen.ClientOptInput;
import org.openapitools.codegen.CodegenOperation;
import org.openapitools.codegen.CodegenProperty;
import org.openapitools.codegen.DefaultGenerator;
import org.openapitools.codegen.languages.SpringCodegen;

class BoatSpringCodeGenTests {

    static final String PROP_BASE = BoatSpringCodeGenTests.class.getSimpleName() + ".";
    static final String TEST_OUTPUT = System.getProperty(PROP_BASE + "output", "target/boat-spring-codegen-tests");

    @BeforeAll
    static void before() throws IOException {
        Files.createDirectories(Paths.get(TEST_OUTPUT));
        FileUtils.deleteDirectory(new File(TEST_OUTPUT));
    }

    @Test
    void clientOptsUnicity() {
        final BoatSpringCodeGen gen = new BoatSpringCodeGen();
        gen.cliOptions()
            .stream()
            .collect(groupingBy(CliOption::getOpt))
            .forEach((k, v) -> assertEquals(1, v.size(), k + " is described multiple times"));
    }


    @Test
    void processOptsUseProtectedFields() {
        final BoatJavaCodeGen gen = new BoatJavaCodeGen();
        final Map<String, Object> options = gen.additionalProperties();

        options.put(USE_PROTECTED_FIELDS, "true");

        gen.processOpts();

        assertThat(gen.additionalProperties(), hasEntry("modelFieldsVisibility", "protected"));
    }


    @Test
    void newLineIndent() throws IOException {
        final NewLineIndent indent = new BoatSpringCodeGen.NewLineIndent(2, "_");
        final StringWriter output = new StringWriter();
        final Fragment frag = mock(Fragment.class);

        when(frag.execute()).thenReturn("\n Good \r\n   morning,  \r\n  Dave ");

        indent.execute(frag, output);

        assertThat(output.toString(), equalTo(String.format("__%n__Good%n__  morning,%n__ Dave%n")));
    }

    @Test
    void addServletRequestTestFromOperation(){
        final BoatSpringCodeGen gen = new BoatSpringCodeGen();
        gen.addServletRequest = true;
        CodegenOperation co = gen.fromOperation("/test", "POST", new Operation(), null);
        assertEquals(1, co.allParams.size());
        assertEquals("httpServletRequest", co.allParams.get(0).paramName);
        assertTrue(Arrays.stream(co.allParams.get(0).getClass().getDeclaredFields()).anyMatch(f -> "isHttpServletRequest".equals(f.getName())));
    }

    @Test
    void multipartWithFileAndObject() throws IOException {
        var codegen = new BoatSpringCodeGen();
        var input = new File("src/test/resources/boat-spring/multipart.yaml");
        codegen.setLibrary("spring-boot");
        codegen.setInterfaceOnly(true);
        codegen.setOutputDir(TEST_OUTPUT + "/multipart");
        codegen.setInputSpec(input.getAbsolutePath());

        var openApiInput = new OpenAPIParser().readLocation(input.getAbsolutePath(), null, new ParseOptions()).getOpenAPI();
        var clientOptInput = new ClientOptInput();
        clientOptInput.config(codegen);
        clientOptInput.openAPI(openApiInput);

        List<File> files = new DefaultGenerator().opts(clientOptInput).generate();

        File testApi = files.stream().filter(file -> file.getName().equals("TestApi.java"))
            .findFirst()
            .get();
        MethodDeclaration testPostMethod = StaticJavaParser.parse(testApi)
            .findAll(MethodDeclaration.class)
            .get(1);

        Parameter filesParam = testPostMethod.getParameterByName("files").get();
        Parameter contentParam = testPostMethod.getParameterByName("content").get();

        assertTrue(filesParam.getAnnotationByName("RequestPart").isPresent());
        assertTrue(contentParam.getAnnotationByName("RequestPart").isPresent());
        assertThat(contentParam.getTypeAsString(), equalTo("TestObjectPart"));
        assertThat(filesParam.getTypeAsString(), equalTo("List<MultipartFile>"));
    }

    @Test
    void testReplaceBeanValidationCollectionType() {
        var codegen = new BoatSpringCodeGen();
        codegen.setUseBeanValidation(true);
        var codegenProperty = new CodegenProperty();
        codegenProperty.isModel = true;
        codegenProperty.baseName = "request"; // not a response

        String result = codegen.replaceBeanValidationCollectionType(
                codegenProperty,"Set<@Valid com.backbase.dbs.arrangement.commons.model.TranslationItemDto>");
        assertEquals("Set<com.backbase.dbs.arrangement.commons.model.@Valid TranslationItemDto>", result);
    }
    @Test
    @SuppressWarnings("unchecked")
    void shouldGenerateValidations() throws InterruptedException, IOException {

        final String REFERENCED_CLASS_NAME = "com.backbase.oss.codegen.java.ValidatedPojo";
        final String REFERENCED_ENUM_NAME = "com.backbase.oss.codegen.java.CommonEnum";

        var modelPackage = "com.backbase.model";
        var input = new File("src/test/resources/boat-spring/openapi.yaml");
        var output = TEST_OUTPUT + "/shouldGenerateValidations";

        // generate project
        var codegen = new BoatSpringCodeGen();
        codegen.setLibrary("spring-boot");
        codegen.setInterfaceOnly(true);
        codegen.setSkipDefaultInterface(true);
        codegen.setOutputDir(output);
        codegen.setInputSpec(input.getAbsolutePath());
        codegen.schemaMapping().put("ValidatedPojo", REFERENCED_CLASS_NAME);
        codegen.schemaMapping().put("CommonEnum", REFERENCED_ENUM_NAME);
        codegen.additionalProperties().put(SpringCodegen.USE_SPRING_BOOT3, Boolean.TRUE.toString());
        codegen.additionalProperties().put(BoatSpringCodeGen.USE_CLASS_LEVEL_BEAN_VALIDATION, Boolean.TRUE.toString());
        codegen.setModelPackage(modelPackage);

        var openApiInput = new OpenAPIParser()
            .readLocation(input.getAbsolutePath(), null, new ParseOptions())
            .getOpenAPI();
        var clientOptInput = new ClientOptInput();
        clientOptInput.config(codegen);
        clientOptInput.openAPI(openApiInput);

        List<File> files = new DefaultGenerator().opts(clientOptInput).generate();

        File paymentsApiFile = files.stream().filter(file -> file.getName().equals("PaymentsApi.java"))
            .findFirst()
            .get();
        MethodDeclaration getPaymentsMethod = StaticJavaParser.parse(paymentsApiFile)
            .findAll(MethodDeclaration.class)
            .stream()
            .filter(it -> "getPayments".equals(it.getName().toString()))
            .findFirst().orElseThrow();
        assertHasCollectionParamWithType(getPaymentsMethod, "approvalTypeIds", "List", "String");
        assertHasCollectionParamWithType(getPaymentsMethod, "status", "List", "String");
        assertHasCollectionParamWithType(getPaymentsMethod, "headerParams", "List", "String");

        File PaymentRequestLine = files.stream().filter(file -> file.getName().equals("PaymentRequestLine.java"))
            .findFirst()
            .get();
        MethodDeclaration getStatus = StaticJavaParser.parse(PaymentRequestLine)
            .findAll(MethodDeclaration.class)
            .stream()
            .filter(it -> "getStatus".equals(it.getName().toString()))
            .findFirst().orElseThrow();
        assertMethodCollectionReturnType(getStatus, "List", "StatusEnum");

        FileUtils.copyToFile(
                getClass().getResourceAsStream("/boat-spring/ValidatedPojo.java"),
                new File(output + "/src/main/java/"
                        + REFERENCED_CLASS_NAME.replaceAll("\\.", "/") + "/ValidatedPojo.java"));
        FileUtils.copyToFile(
                getClass().getResourceAsStream("/boat-spring/CommonEnum.java"),
                new File(output + "/src/main/java/"
                        + REFERENCED_ENUM_NAME.replaceAll("\\.", "/") + "/CommonEnum.java"));

        // compile generated project
        var compiler = new MavenProjectCompiler(BoatSpringCodeGenTests.class.getClassLoader());
        var projectDir = new File(output);
        int compilationStatus = compiler.compile(projectDir);
        assertEquals(0, compilationStatus);

        // verify
        ClassLoader projectClassLoader = compiler.getProjectClassLoader(projectDir);
        var verificationRunner = new VerificationRunner(projectClassLoader);
        Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

        Runnable verifyDefaultValues = () -> {
            try {
                var className = modelPackage + ".MultiLinePaymentRequest";
                Class<?> requestClass = projectClassLoader.loadClass(className);
                Object requestObject = requestClass.getConstructor().newInstance();
                Set<ConstraintViolation<Object>> violations = validator.validate(requestObject);
                assertThat(violations, Matchers.hasSize(1));
                assertThat(violations.stream().findFirst().get().getPropertyPath().toString(), Matchers.equalTo("name"));
                assertThat(
                    violations.stream().findFirst().get().getMessageTemplate(),
                    Matchers.equalTo("{jakarta.validation.constraints.NotNull.message}")
                );
            } catch (Exception e) {
                throw new UnhandledException(e);
            }
        };
        verificationRunner.runVerification(
            Verification.builder().runnable(verifyDefaultValues).displayName("validations").build()
        );

        Runnable verifyCollectionItems = () -> {
            try {
                var className = modelPackage + ".MultiLinePaymentRequest";
                Class<?> requestClass = projectClassLoader.loadClass(className);
                Object requestObject = requestClass.getConstructor().newInstance();

                // set name on MultiLinePaymentRequest
                requestObject.getClass()
                    .getDeclaredMethod("setName", String.class)
                    .invoke(requestObject, "someName");

                // set arrangement ids
                Collection<String> arrangementIds = (Collection<String>) requestObject.getClass()
                    .getDeclaredMethod("getArrangementIds")
                    .invoke(requestObject);
                arrangementIds.add("1");
                arrangementIds.add("");

                // add PaymentRequestLine to lines
                Collection<Object> lines = (Collection<Object>) requestObject.getClass()
                    .getDeclaredMethod("getLines")
                    .invoke(requestObject);
                lines.add(newPaymeyntRequestLineObject(modelPackage, projectClassLoader, "invalidId"));

                // add mapStrings
                Map<String, String> mapStrings = (Map<String, String>) requestObject.getClass()
                    .getDeclaredMethod("getMapStrings")
                    .invoke(requestObject);
                mapStrings.put("key1", "abc");
                mapStrings.put("key2", "abcdefghijklmnopq");

                // add mapObjects
                Map<String, Object> mapObjects = (Map<String, Object>) requestObject.getClass()
                    .getDeclaredMethod("getMapObjects")
                    .invoke(requestObject);
                mapObjects.put(
                    "key1",
                    newPaymeyntRequestLineObject(modelPackage, projectClassLoader, UUID.randomUUID().toString())
                );
                mapObjects.put(
                    "key2",
                    newPaymeyntRequestLineObject(modelPackage, projectClassLoader, UUID.randomUUID().toString())
                );

                // validate
                Set<ConstraintViolation<Object>> violations = validator.validate(requestObject);
                assertThat(violations, Matchers.hasSize(6));

                assertViolationsCountByMessage(violations, "{jakarta.validation.constraints.Pattern.message}", 1);
                assertViolationsCountByPath(violations, "lines[0].accountId", 1);

                assertViolationsCountByMessage(violations, "{jakarta.validation.constraints.Size.message}", 5);
                assertViolationsCountByPath(violations, "arrangementIds[0].<list element>", 1);
                assertViolationsCountByPath(violations, "arrangementIds[1].<list element>", 1);
                assertViolationsCountByPath(violations, "mapStrings[key1].<map value>", 1);
                assertViolationsCountByPath(violations, "mapStrings[key2].<map value>", 1);
                assertViolationsCountByPath(violations, "mapObjects", 1);

            } catch (Exception e) {
                throw new UnhandledException(e);
            }
        };
        verificationRunner.runVerification(
            Verification.builder().runnable(verifyCollectionItems).displayName("validations").build()
        );
    }

    @NotNull
    private static Object newPaymeyntRequestLineObject(String modelPackage, ClassLoader projectClassLoader, String id)
        throws Exception {
        Class<?> lineObjectClass = projectClassLoader.loadClass(modelPackage + ".PaymentRequestLine");
        Object lineObject = lineObjectClass.getConstructor().newInstance();
        lineObject.getClass()
            .getDeclaredMethod("setAccountId", String.class)
            .invoke(lineObject, id);
        return lineObject;
    }

    private static void assertViolationsCountByMessage(Set<ConstraintViolation<Object>> violations, String messageTemplate, int count) {
        long actualCount = violations.stream()
            .map(ConstraintViolation::getMessageTemplate)
            .filter(messageTemplate::equals)
            .count();
        assertEquals(count, actualCount,
            String.format("Number of violations '%s', count mismatch", messageTemplate));
    }

    private static void assertViolationsCountByPath(Set<ConstraintViolation<Object>> violations, String propertyPath, int count) {
        long actualCount = violations.stream()
            .map(ConstraintViolation::getPropertyPath)
            .map(String::valueOf)
            .filter(propertyPath::equals)
            .count();
        assertEquals(count, actualCount,
            String.format("Number of violations '%s', count mismatch", propertyPath));
    }

    private static void assertHasCollectionParamWithType(MethodDeclaration method, String paramName, String collectionType, String itemType) {
        Parameter parameter = method.getParameterByName(paramName).get();
        assertEquals(ClassOrInterfaceType.class, parameter.getType().getClass());
        assertEquals(collectionType, ((ClassOrInterfaceType) parameter.getType()).getName().toString());
        NodeList<Type> argumentTypes = ((ClassOrInterfaceType) parameter.getType())
            .getTypeArguments()
            .orElseThrow();
        ClassOrInterfaceType actualItemType = argumentTypes.getFirst().orElseThrow().stream()
            .map(ClassOrInterfaceType.class::cast)
            .findFirst()
            .orElseThrow();
        assertEquals(itemType, actualItemType.getName().toString());
    }

    private static void assertMethodCollectionReturnType(MethodDeclaration method, String collectionType, String itemType) {
        assertEquals(collectionType, ((ClassOrInterfaceType) method.getType()).getName().toString());
        ClassOrInterfaceType collectionItemType = (ClassOrInterfaceType) ((ClassOrInterfaceType) method.getType())
            .getTypeArguments().get().getFirst().get();
        assertEquals(itemType, collectionItemType.getName().toString());
    }
}
