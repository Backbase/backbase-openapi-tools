package com.backbase.oss.codegen.java;

import static com.backbase.oss.codegen.java.BoatSpringCodeGen.USE_PROTECTED_FIELDS;
import static java.util.stream.Collectors.groupingBy;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.isA;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.backbase.oss.codegen.java.BoatSpringCodeGen.NewLineIndent;
import com.backbase.oss.codegen.java.VerificationRunner.Verification;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.expr.Expression;
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
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.UnhandledException;
import org.hamcrest.Matchers;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.openapitools.codegen.CliOption;
import org.openapitools.codegen.ClientOptInput;
import org.openapitools.codegen.CodegenOperation;
import org.openapitools.codegen.CodegenProperty;
import org.openapitools.codegen.DefaultGenerator;
import org.openapitools.codegen.languages.SpringCodegen;
import org.openapitools.jackson.nullable.JsonNullableModule;

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
    void webhookWithCardsApi() throws IOException {
        var codegen = new BoatSpringCodeGen();
        var input = new File("src/test/resources/boat-spring/cardsapi.yaml");
        codegen.setLibrary("spring-boot");
        codegen.setInterfaceOnly(true);
        codegen.setOutputDir(TEST_OUTPUT + "/cards");
        codegen.setInputSpec(input.getAbsolutePath());

        var openApiInput = new OpenAPIParser().readLocation(input.getAbsolutePath(), null, new ParseOptions()).getOpenAPI();
        var clientOptInput = new ClientOptInput();
        clientOptInput.config(codegen);
        clientOptInput.openAPI(openApiInput);

        List<File> files = new DefaultGenerator().opts(clientOptInput).generate();

        File testApi = files.stream().filter(file -> file.getName().equals("ClientApi.java"))
                .findFirst()
                .get();
        MethodDeclaration testPostMethod = StaticJavaParser.parse(testApi)
                .findAll(MethodDeclaration.class)
                .get(1);
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
    @ParameterizedTest
    @CsvSource(value = {"true,true", "true,false", "false,true", "false,false"})
    @SuppressWarnings("unchecked")
    void shouldGenerateValidations(boolean useLombok, boolean bigDecimalsAsStrings) throws InterruptedException, IOException {

        final String REFERENCED_CLASS_NAME = "com.backbase.oss.codegen.java.ValidatedPojo";
        final String REFERENCED_ENUM_NAME = "com.backbase.oss.codegen.java.CommonEnum";

        var modelPackage = "com.backbase.model";
        var input = new File("src/test/resources/boat-spring/openapi.yaml");
        var output = TEST_OUTPUT + String.format("/shouldGenerateValidations_lombok-%s_bigDecString-%s", useLombok,
            bigDecimalsAsStrings);

        // generate project
        var codegen = new BoatSpringCodeGen();
        codegen.setLibrary("spring-boot");
        codegen.setInterfaceOnly(true);
        codegen.setSkipDefaultInterface(true);
        codegen.setOutputDir(output);
        codegen.setInputSpec(input.getAbsolutePath());
        codegen.setContainerDefaultToNull(true);
        codegen.setSerializeBigDecimalAsString(bigDecimalsAsStrings);
        codegen.setUseLombokAnnotations(useLombok);
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

        MethodDeclaration createPaymentsMethod = StaticJavaParser.parse(paymentsApiFile)
            .findAll(MethodDeclaration.class)
            .stream()
            .filter(it -> "createPayments".equals(it.getName().toString()))
            .findFirst().orElseThrow();
        AnnotationExpr sizeAnnotation = createPaymentsMethod.getParameterByName("multiLinePaymentRequest").orElseThrow()
            .getAnnotationByName("Size").orElseThrow();
        assertEquals("@Size(min = 1, max = 55)", sizeAnnotation.toString());

        File paymentRequestLine = files.stream().filter(file -> file.getName().equals("PaymentRequestLine.java"))
            .findFirst()
            .get();
        CompilationUnit paymentRequestLineUnit = StaticJavaParser.parse(paymentRequestLine);
        if (!useLombok) {
            MethodDeclaration getStatus = paymentRequestLineUnit
                .findAll(MethodDeclaration.class)
                .stream()
                .filter(it -> "getStatus".equals(it.getName().toString()))
                .findFirst().orElseThrow();
            assertMethodCollectionReturnType(getStatus, "List", "StatusEnum");
        }
        assertFieldValueAssignment(paymentRequestLineUnit, "additionalPropertiesMap", "new HashMap<>()");

        File paymentRequest = files.stream().filter(file -> file.getName().equals("PaymentRequest.java"))
            .findFirst()
            .get();
        CompilationUnit paymentRequestUnit = StaticJavaParser.parse(paymentRequest);
        assertFieldAnnotation(paymentRequestUnit, "currencyCode", "Pattern");
        assertFieldAnnotation(paymentRequestUnit, "currencyCode", "NotNull");
        assertFieldAnnotation(paymentRequestUnit, "referenceNumber", "Size");
        assertFieldAnnotation(paymentRequestUnit, "referenceNumber", "NotNull");
        assertFieldAnnotation(paymentRequestUnit, "requestLine", "Valid");

        File multiLinePaymentRequest = files.stream().filter(f -> f.getName().equals("MultiLinePaymentRequest.java"))
                .findFirst()
                .get();
        CompilationUnit multiLinePaymentRequestUnit = StaticJavaParser.parse(multiLinePaymentRequest);

        assertFieldAnnotation(multiLinePaymentRequestUnit, "arrangementIds", "NotNull");
        assertFieldValueAssignment(
                multiLinePaymentRequestUnit, "arrangementIds", "new ArrayList<>()");
        assertFieldAnnotation(multiLinePaymentRequestUnit, "uniqueLines", "NotNull");
        assertFieldAnnotation(multiLinePaymentRequestUnit, "name", "Pattern", "@Pattern(regexp = \"^[^\\\\r\\\\n]{1,64}$\")");
        assertFieldValueAssignment(
                multiLinePaymentRequestUnit, "uniqueArrangementIds", null);

        // assert annotation

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
        assertEquals(0, compilationStatus,
                "Failed compilation. see " + projectDir.getAbsolutePath() + "/mvn.log for details");

        // verify
        ClassLoader projectClassLoader = compiler.getProjectClassLoader(projectDir);
        var verificationRunner = new VerificationRunner(projectClassLoader);
        Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

        Runnable verifyDefaultValues = () -> {
            try {
                var className = modelPackage + ".MultiLinePaymentRequest";
                Object requestObject = newInstanceOf(className, projectClassLoader);
                Set<ConstraintViolation<Object>> violations = validator.validate(requestObject);
                assertThat(violations, Matchers.hasSize(3));
                assertThat(
                    violations.stream()
                        .map(ConstraintViolation::getPropertyPath)
                        .map(Objects::toString)
                        .sorted()
                        .collect(Collectors.toList()),
                    Matchers.hasItems("amountNumber", "amountNumberAsString", "name")
                );
                assertThat(
                    violations.stream()
                        .map(ConstraintViolation::getMessageTemplate)
                        .map(Objects::toString)
                        .distinct()
                        .sorted()
                        .collect(Collectors.toList()),
                    Matchers.hasItems("{jakarta.validation.constraints.NotNull.message}")
                );
            } catch (Exception e) {
                throw new UnhandledException(e);
            }
        };
        verificationRunner.runVerification(
            Verification.builder().runnable(verifyDefaultValues).displayName("defaultValues").build()
        );

        Runnable verifyCollectionItems = () -> {
            try {
                var className = modelPackage + ".MultiLinePaymentRequest";
                Object requestObject = newInstanceOf(className, projectClassLoader);

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
                // mapStrings is optional, so it is null
                assertTrue(mapStrings == null);
                // but putMethod should not fail
                requestObject.getClass()
                        .getDeclaredMethod("putMapStringsItem", String.class, String.class)
                        .invoke(requestObject, "key0000", "asdasdasd");

                // mapStrings not null after that
                mapStrings = (Map<String, String>) requestObject.getClass()
                        .getDeclaredMethod("getMapStrings")
                        .invoke(requestObject);
                assertTrue(mapStrings != null);
                mapStrings.put("key1", "abc");
                mapStrings.put("key2", "abcdefghijklmnopq");

                // add mapObjects - which is optional, so initially null
                Map<String, Object> mapObjects = new HashMap<>();
                requestObject.getClass()
                    .getDeclaredMethod("setMapObjects", Map.class)
                    .invoke(requestObject, mapObjects);
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
                assertThat(violations, Matchers.hasSize(8));

                assertViolationsCountByMessage(violations, "{jakarta.validation.constraints.Pattern.message}", 1);
                assertViolationsCountByMessage(violations, "{jakarta.validation.constraints.NotNull.message}", 2);
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
            Verification.builder().runnable(verifyCollectionItems).displayName("collectionItems").build()
        );

        var mapper = new ObjectMapper();
        mapper.registerModule(new JsonNullableModule());

        Runnable verifySerDes = () -> {
            try {
                var className = modelPackage + ".MultiLinePaymentRequest";
                Object requestObject = newInstanceOf(className, projectClassLoader);

                requestObject.getClass()
                    .getDeclaredMethod("setName", String.class)
                    .invoke(requestObject, "someName");

                requestObject.getClass()
                    .getDeclaredMethod("setAmountNumber", BigDecimal.class)
                    .invoke(requestObject, new BigDecimal("100.123"));

                requestObject.getClass()
                    .getDeclaredMethod("setAmountNumberAsString", BigDecimal.class)
                    .invoke(requestObject, new BigDecimal("200.123"));

                String json = mapper.writeValueAsString(requestObject);
                Object deserializedPojo = mapper.readValue(json, requestObject.getClass());

                Object deserializedAmountNumber = deserializedPojo.getClass()
                    .getDeclaredMethod("getAmountNumber")
                    .invoke(deserializedPojo);
                assertThat(deserializedAmountNumber, isA(BigDecimal.class));
                assertEquals(new BigDecimal("100.123"), deserializedAmountNumber);

                Object deserializedAmountNumberAsString = deserializedPojo.getClass()
                    .getDeclaredMethod("getAmountNumberAsString")
                    .invoke(deserializedPojo);
                assertThat(deserializedAmountNumberAsString, isA(BigDecimal.class));
                assertEquals(new BigDecimal("200.123"), deserializedAmountNumberAsString);

            } catch (Exception e) {
                throw new UnhandledException(e);
            }
        };
        verificationRunner.runVerification(
            Verification.builder().runnable(verifySerDes).displayName("json-ser-des").build()
        );
    }

    private static Object newInstanceOf(String className, ClassLoader classLoader) throws Exception {
        Class<?> requestClass = classLoader.loadClass(className);
        return requestClass.getConstructor().newInstance();
    }

    private static void assertFieldAnnotation(
            CompilationUnit unit, String fieldName, String annotationName) throws FileNotFoundException {
        FieldDeclaration fieldDeclaration = findFieldDeclaration(unit, fieldName);
        assertThat("Expect annotation to be present on field: " + annotationName + " " + fieldName,
                fieldDeclaration.getAnnotationByName(annotationName).isPresent(), is(true));
    }

    private static void assertFieldAnnotation(
        CompilationUnit unit, String fieldName, String annotationName, String value) throws FileNotFoundException {
        FieldDeclaration fieldDeclaration = findFieldDeclaration(unit, fieldName);
        AnnotationExpr annotation = fieldDeclaration.getAnnotationByName(annotationName)
            .orElseThrow(() -> new AssertionError(
                "Expect annotation to be present on field: " + annotationName + " " + fieldName));
        assertThat(annotation.toString(), equalTo(value));
    }

    private static void assertFieldValueAssignment(
            CompilationUnit unit, String fieldName, String valueAssignment) throws FileNotFoundException {
        FieldDeclaration fieldDeclaration = findFieldDeclaration(unit, fieldName);

        Optional<com.github.javaparser.ast.expr.Expression> expression = fieldDeclaration.getChildNodes()
                .stream()
                .filter(n -> n instanceof VariableDeclarator)
                .map(n -> ((VariableDeclarator)n).getInitializer())
                .findFirst().orElseThrow(
                        () -> new RuntimeException("VariableDeclarator not found on Field " + fieldName));
        if (expression.isEmpty()) {
            assertThat("Expected value " + valueAssignment + " for field " + fieldName,  valueAssignment == null);
        } else {
            Expression expr = expression.get();
            // Depending on 'toString' implementation is shaky but works for now...
            assertThat(expr.toString(), is(valueAssignment));
        }
    }

    private static FieldDeclaration findFieldDeclaration(CompilationUnit unit, String fieldName) {
        Optional<FieldDeclaration> result = unit
                .findAll(FieldDeclaration.class)
                .stream()
                .filter(field -> field.getVariable(0).getName().getIdentifier().equals(fieldName))
                .findFirst();
        assertThat("Expect field declaration to be present: " + fieldName,
                result.isPresent(), is(true));
        return result.get();
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
