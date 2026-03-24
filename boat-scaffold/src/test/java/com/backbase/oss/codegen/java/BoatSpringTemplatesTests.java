package com.backbase.oss.codegen.java;

import static java.util.Arrays.stream;
import static java.util.stream.Collectors.toList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.DynamicContainer.dynamicContainer;
import static org.junit.jupiter.api.DynamicTest.dynamicTest;

import com.backbase.oss.codegen.java.VerificationRunner.Verification;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.ArrayType;
import com.fasterxml.jackson.databind.type.TypeBindings;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.google.common.base.CaseFormat;
import java.io.File;
import java.io.IOException;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.UnhandledException;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DynamicNode;
import org.junit.jupiter.api.TestFactory;
import org.openapitools.codegen.ClientOptInput;
import org.openapitools.codegen.CodegenConstants;
import org.openapitools.codegen.DefaultGenerator;
import org.openapitools.codegen.config.CodegenConfigurator;
import org.openapitools.codegen.config.GlobalSettings;
import org.openapitools.codegen.languages.SpringCodegen;
import org.openapitools.codegen.languages.features.BeanValidationFeatures;
import org.openapitools.codegen.languages.features.OptionalFeatures;

/**
 * These tests verify that the code generation works for various combinations of configuration
 * parameters; the projects that are generated are later compiled in the integration test phase.
 * The suite is generated dynamically, read below.
 * <p>
 * With Junit4, the test hierarchy was {@code root-> combination -> method}. The code relied on that
 * structure to speedup the suite execution.
 * </p>
 * <p>
 * With Jupiter, the hierarchy is {@code root -> method -> combination}, that's why the suite is
 * created dynamically.
 * </p>
 */
@Slf4j
class BoatSpringTemplatesTests {

    private static final MavenProjectCompiler MAVEN_PROJECT_COMPILER = new MavenProjectCompiler(
        BoatSpringTemplatesTests.class.getClassLoader());
    static final String PROP_BASE = BoatSpringTemplatesTests.class.getSimpleName() + ".";
    static final boolean PROP_FAST = Boolean.parseBoolean(System.getProperty(PROP_BASE + "fast", "true"));
    static final String TEST_OUTPUT = System.getProperty(PROP_BASE + "output", "target/boat-spring-templates-tests");

    @BeforeAll
    static public void setUpClass() throws IOException {
        Files.createDirectories(Paths.get(TEST_OUTPUT));
        FileUtils.deleteDirectory(new File(TEST_OUTPUT, "src"));
    }

    static class Combination {

        String name = "boat";

        boolean useBeanValidation = false;
        boolean useOptional = false;

        boolean addServletRequest = false;
        boolean addBindingResult = false;

        boolean reactive = false;
        boolean apiUtil = false;

        private Combination() {}

        private Combination(boolean all) {
            if (all) {
                this.addBindingResult = true;
                this.addServletRequest = true;
                this.apiUtil = true;
                this.reactive = true;
                this.useBeanValidation = true;
                this.useOptional = true;
                this.name = "boat-val-opt-bin-utl-servlet-reactive";
            }
        }

        public static Combination create() {
            return new Combination();
        }

        public static Combination all() {
            return new Combination(true);
        }

        public Combination validation() {
            this.useBeanValidation = true;
            this.name += "-validation";
            return this;
        }

        public Combination optional() {
            this.useOptional = true;
            this.name += "-optional";
            return this;
        }

        public Combination servlet() {
            this.addServletRequest = true;
            this.name += "-servlet";
            return this;
        }

        public Combination binding() {
            this.addBindingResult = true;
            this.name += "-binding";
            return this;
        }

        public Combination reactive() {
            this.reactive = true;
            this.name += "-reactive";
            return this;
        }

        public Combination apiUtil() {
            this.apiUtil = true;
            this.name += "-util";
            return this;
        }

        public Combination noReactive() {
            this.reactive = false;
            this.name.replace("-reactive", "");
            return this;
        }

        public Combination noServlet() {
            this.addServletRequest = false;
            this.name.replace("-servlet", "");
            return this;
        }

        @Override
        public String toString() {
            return "Combination(" +
                   name +
                   ')';
        }
    }

    static Stream<Combination> cases() {
        return Stream.of(
            Combination.create(),
            Combination.create().validation(),
            Combination.create().optional(),
            Combination.create().servlet(),
            Combination.create().binding(),
            Combination.create().reactive(),
            Combination.create().apiUtil(),
            Combination.all().noReactive(),
            Combination.all().noServlet()
        );
    }


    /** dynamic suite creation **/
    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    @interface Check {
    }

    @TestFactory
    Stream<DynamicNode> withCombinations() {
        return cases()
            .map(param -> {

                List<File> files = generateFrom(null, param);

                return dynamicContainer(
                    param.name,
                    findCheckMethods().map(
                        m -> dynamicTest(m.getName(), () -> invoke(m, param, files))));
            });
    }

    Stream<Method> findCheckMethods() {
        return stream(this.getClass().getDeclaredMethods())
            .filter(m -> m.isAnnotationPresent(Check.class))
            .filter(m -> {
                Class<?>[] params = m.getParameterTypes();
                if (params.length != 2 ||
                    !params[0].equals(Combination.class) ||
                    !params[1].equals(List.class)) {

                    throw new IllegalStateException(
                        "@Check method must include (Combination param, List files) parameters: " + m
                    );
                }
                return true;
            });
    }

    @SneakyThrows
    private void invoke(Method m, Combination param, List<File> files) {
        m.invoke(this, param, files);
    }

    /** the actual testing code **/

    @Check
    void generated(Combination param, List<File> files) {
        assertThat(files, not(nullValue()));
        assertThat(files.size(), not(equalTo(0)));
    }

    @Check
    void useBeanValidation(Combination param, List<File> files) {
        assertThat(findPattern(files, "/api/.+\\.java$", "@Valid"),
            equalTo(param.useBeanValidation || param.addBindingResult));
        assertThat(findPattern(files, "/model/.+\\.java$", "@Valid"),
            equalTo(param.useBeanValidation || param.addBindingResult));
        assertThat(findPattern(files, "/model/MultiLinePaymentRequest.*\\.java$", "List<@Pattern\\(regexp"),
            equalTo(param.useBeanValidation || param.addBindingResult));
        assertThat(findPattern(files, "/model/MultiLinePaymentRequest.*\\.java$", "Map<String, @Size\\(min = 7, max = 10\\)"),
            equalTo(param.useBeanValidation || param.addBindingResult));
    }

    @Check
    void queryParamsCustomNotNullValidation(Combination param, List<File> files) {
        assertThat(findPattern(files, "/api/ArrayTypesApi\\.java$", "List<@NotNull.*>\\s+qParamsNotNull"),
            equalTo(param.useBeanValidation || param.addBindingResult));
        assertThat(findPattern(files, "/api/SetTypesApi\\.java$", "Set\\s*<@NotNull.*>\\s+qParamsNotNull"),
            equalTo(param.useBeanValidation || param.addBindingResult));
        assertThat(findPattern(files, "/api/SimpleTypesApi\\.java$", "Set\\s*<@NotNull.*>\\s+qParamsNotNull"),
            equalTo(param.useBeanValidation || param.addBindingResult));
        assertThat(findPattern(files,"/api/MapTypesApi\\.java$", "List<@NotNull.*>\\s+qParamsNotNull"),
            equalTo(param.useBeanValidation || param.addBindingResult));
    }

    @Check
    void useOptional(Combination param, List<File> files) {
        assertThat(findPattern(files, "/api/.+\\.java$", "Optional<(?!NativeWebRequest)[^>]+>"),
            equalTo(param.useOptional));
        assertThat(findPattern(files, "/model/.+\\.java$", "Optional<[^>]+>"),
            is(false));
    }

    @Check
    void addServletRequest(Combination param, List<File> files) {
        assertThat(findPattern(files, "/api/.+\\.java$", "HttpServletRequest\\s+httpServletRequest"),
            equalTo(param.addServletRequest));
        assertThat(findPattern(files, "/model/.+\\.java$", "HttpServletRequest\\s+httpServletRequest"),
            is(false));
    }

    @Check
    void addBindingResult(Combination param, List<File> files){
        assertThat(findPattern(files, "/api/.+\\.java$", "BindingResult\\s+bindingResult"),
            equalTo(param.addBindingResult));
        assertThat(findPattern(files, "/model/.+\\.java$", "HttpServletRequest\\s+httpServletRequest"),
            is(false));
    }

    @Check
    void checkCompiles(Combination param, List<File> files) throws Exception {
        final var projectDir = new File(TEST_OUTPUT, param.name);
        assertThat(projectDir + " is not a directory", projectDir.isDirectory());
        compileGeneratedProject(projectDir);
        verifyGeneratedClasses(param.name, projectDir);
    }

    private static void compileGeneratedProject(File projectDir) {
        int compilationStatus = MAVEN_PROJECT_COMPILER.compile(projectDir);
        assertEquals(0, compilationStatus, "Could not compile generated project in dir: " + projectDir);
    }

    private void verifyGeneratedClasses(String paramName, File projectDir) throws Exception {
        var classLoader = MAVEN_PROJECT_COMPILER.getProjectClassLoader(projectDir);
        verifyReceivableRequestModelJsonConversion(paramName, classLoader);
        verifyMultiLineRequest(paramName, classLoader);
    }

    private void verifyReceivableRequestModelJsonConversion(String paramName, ClassLoader classLoader) throws InterruptedException {
        String testedModelClassName = buildReceivableRequestModelClassName(paramName);
        String parentModelClassName = buildPaymentRequestModelClassName(paramName);
        var objectMapper = new ObjectMapper();
        Runnable verificationRunnable = () -> {
            try {
                Class<?> modelClass = classLoader.loadClass(testedModelClassName);
                Class<?> parentClass = classLoader.loadClass(parentModelClassName);
                Constructor<?> constructor = modelClass.getConstructor(String.class, String.class, String.class);
                Object modelObject1 = constructor.newInstance("OK_status", "ref123", "EUR");
                Object modelObject2 = constructor.newInstance("BAD_status", "ref456", "USD");

                // Serialize using the parent (discriminated) type so that Jackson's
                // is aware of full polymorphic context
                TypeFactory tf = TypeFactory.defaultInstance();

                // serialize and deserialize list
                List<?> modelObjects = List.of(modelObject1, modelObject2);
                String serializedObjects = objectMapper.writerFor(
                    tf.constructCollectionType(List.class, parentClass)
                ).writeValueAsString(modelObjects);
                Object[] deserializedModelObjects = objectMapper.readValue(
                    serializedObjects,
                    ArrayType.construct(
                        tf.constructFromCanonical(parentClass.getName()),
                        TypeBindings.emptyBindings()
                    )
                );
                assertEquals(modelObjects.size(), deserializedModelObjects.length);
                assertEquals(modelObject1.getClass(), deserializedModelObjects[0].getClass());

                // serialize and deserialize single object
                String serializedObject1 = objectMapper.writerFor(parentClass).writeValueAsString(modelObject1);
                Object deserializedObject1 = objectMapper.readValue(serializedObject1, parentClass);
                assertEquals(modelClass, deserializedObject1.getClass());

                verifyJsonIgnoreAnnotation(modelClass);
                verifyJsonIgnoreAnnotation(parentClass);
            } catch (Exception e) {
                throw new UnhandledException(e);
            }
        };
        var verificationRunner = new VerificationRunner(classLoader);
        verificationRunner.runVerification(
            Verification.builder().runnable(verificationRunnable).displayName(paramName).build()
        );
    }

    private void verifyJsonIgnoreAnnotation(Class<?> modelClass) {
        JsonIgnoreProperties ignoreAnnotation = modelClass.getAnnotation(JsonIgnoreProperties.class);

        if (ignoreAnnotation != null) {
            assertFalse(ignoreAnnotation.allowGetters(),
                "Class " + modelClass.getName() + " should not have allowGetters=true in @JsonIgnoreProperties");
        }
    }

    private void verifyMultiLineRequest(String paramName, ClassLoader classLoader) throws InterruptedException {
        String testedModelClassName = buildMultiLineRequestModelClassName(paramName);
        Runnable verificationRunnable = () -> {
            try {
                Class<?> modelClass = classLoader.loadClass(testedModelClassName);
                Constructor<?> constructor = modelClass.getConstructor();
                Object modelObject = constructor.newInstance();
                List<?> listProperty = (List<?>) modelClass.getDeclaredMethod("getLines").invoke(modelObject);
                assertNotNull(listProperty);
            } catch (Exception e) {
                throw new UnhandledException(e);
            }
        };
        var verificationRunner = new VerificationRunner(classLoader);
        verificationRunner.runVerification(
            Verification.builder().runnable(verificationRunnable).displayName(paramName).build()
        );
    }

    /**
     * Build proper class name for `ReceivableRequest`.
     */
    private String buildReceivableRequestModelClassName(String paramName) {
        return buildModelClassName(paramName, "ReceivableRequest");
    }

    /**
     * Build proper class name for `PaymentRequest` (parent/discriminator base).
     */
    private String buildPaymentRequestModelClassName(String paramName) {
        return buildModelClassName(paramName, "PaymentRequest");
    }

    private String buildModelClassName(String paramName, String baseName) {
        var modelPackage = paramName.replace('-', '.') + ".model";
        var classNameSuffix = org.apache.commons.lang3.StringUtils.capitalize(
            paramName.indexOf('-') > -1
                ? CaseFormat.LOWER_HYPHEN.to(CaseFormat.LOWER_CAMEL, paramName)
                : paramName
        );
        return modelPackage + "." + baseName + classNameSuffix;
    }

    private String buildMultiLineRequestModelClassName(String paramName) {
        return buildModelClassName(paramName, "MultiLinePaymentRequest");
    }

    private boolean findPattern(List<File> files, String filePattern, String linePattern) {
        final Predicate<String> fileMatch = Pattern.compile(filePattern).asPredicate();
        log.info("Files: {}", files);
        final List<String> selection = files.stream()
            .map(File::getPath)
            .map(path -> path.replace(File.separatorChar, '/'))
            .filter(fileMatch)
            .collect(toList());

        assertThat(selection, not(hasSize(0)));

        final Predicate<String> lineMatch = Pattern.compile(linePattern).asPredicate();
        return selection.stream()
            .anyMatch(file -> contentMatches(file, lineMatch));
    }

    @SneakyThrows
    private boolean contentMatches(String path, Predicate<String> lineMatch) {
        try (final Stream<String> lines = Files.lines(Paths.get(path))) {
            return lines.anyMatch(lineMatch);
        }
    }

    private List<File> generateFrom(String templates, Combination param) {
        final File input = new File("src/test/resources/boat-spring/openapi.yaml");
        final CodegenConfigurator gcf = new CodegenConfigurator();

        gcf.setGeneratorName(BoatSpringCodeGen.NAME);
        gcf.setInputSpec(input.getAbsolutePath());
        gcf.setOutputDir(TEST_OUTPUT + "/" + param.name);

        GlobalSettings.setProperty(CodegenConstants.APIS, "");
        GlobalSettings.setProperty(CodegenConstants.API_DOCS, "true");
        GlobalSettings.setProperty(CodegenConstants.API_TESTS, "true");
        GlobalSettings.setProperty(CodegenConstants.MODELS, "");
        GlobalSettings.setProperty(CodegenConstants.MODEL_TESTS, "true");
        GlobalSettings.setProperty(CodegenConstants.MODEL_DOCS, "true");
        GlobalSettings.setProperty(CodegenConstants.SUPPORTING_FILES,
            "ApiUtil.java,pom.xml,OpenApiGeneratorApplication.java,BigDecimalCustomSerializer.java");


        gcf.setApiNameSuffix("-api");
        gcf.setModelNameSuffix(param.name);


        gcf.addAdditionalProperty(OptionalFeatures.USE_OPTIONAL, param.useOptional);

        gcf.addAdditionalProperty(BoatSpringCodeGen.ADD_SERVLET_REQUEST, param.addServletRequest);
        gcf.addAdditionalProperty(BoatSpringCodeGen.ADD_BINDING_RESULT,param.addBindingResult);
        if (param.addBindingResult) {
            gcf.addAdditionalProperty(BeanValidationFeatures.USE_BEANVALIDATION, true);
        } else {
            gcf.addAdditionalProperty(BeanValidationFeatures.USE_BEANVALIDATION, param.useBeanValidation);
        }
        gcf.addAdditionalProperty(BoatSpringCodeGen.OPENAPI_NULLABLE, false);
        gcf.addAdditionalProperty(SpringCodegen.REACTIVE, param.reactive);

        final String destPackage = param.name.replace('-', '.') + ".";

        gcf.setApiPackage(destPackage + "api");
        gcf.setModelPackage(destPackage + "model");
        gcf.setInvokerPackage(destPackage + "invoker");

        gcf.addAdditionalProperty(SpringCodegen.BASE_PACKAGE, destPackage + "base");
        gcf.addAdditionalProperty(SpringCodegen.CONFIG_PACKAGE, destPackage + "config");

        gcf.addAdditionalProperty(SpringCodegen.USE_SPRING_BOOT3, true);
        gcf.addAdditionalProperty(CodegenConstants.HIDE_GENERATION_TIMESTAMP, true);
        gcf.addAdditionalProperty(SpringCodegen.INTERFACE_ONLY, false);
        gcf.addAdditionalProperty(SpringCodegen.USE_TAGS, true);
        gcf.addAdditionalProperty(SpringCodegen.SKIP_DEFAULT_INTERFACE, false);
        gcf.addAdditionalProperty(CodegenConstants.ARTIFACT_ID, "boat-templates-tests");
        gcf.addAdditionalProperty("additionalDependencies", ""
            + "        <dependency>\n"
            + "            <groupId>jakarta.persistence</groupId>\n"
            + "            <artifactId>jakarta.persistence-api</artifactId>\n"
            + "            <version>3.1.0</version>\n"
            + "        </dependency>\n"
            + "        <dependency>\n"
            + "            <groupId>jakarta.servlet</groupId>\n"
            + "            <artifactId>jakarta.servlet-api</artifactId>\n"
            + "            <version>6.0.0</version>\n"
            + "        </dependency>\n"
            + "        <dependency>\n"
            + "            <groupId>org.springframework.boot</groupId>\n"
            + "            <artifactId>spring-boot-starter-webflux</artifactId>\n"
            + "        </dependency>\n"
            + "            <groupId>org.springframework.boot</groupId>\n"
            + "            <artifactId>spring-boot-starter-json</artifactId>\n"
            + "        </dependency>\n"
            + "        <dependency>\n"
            + "            <groupId>com.fasterxml.jackson.core</groupId>\n"
            + "            <artifactId>jackson-databind</artifactId>\n"
            + "        </dependency>\n"
            + "        <dependency>\n"
            + "            <groupId>org.openapitools</groupId>\n"
            + "            <artifactId>jackson-databind-nullable</artifactId>\n"
            + "            <version>0.2.9</version>\n"
            + "        </dependency>\n"
            + "");

        gcf.setTemplateDir(templates);

        final ClientOptInput coi = gcf.toClientOptInput();

        return new DefaultGenerator().opts(coi).generate();
    }
}
