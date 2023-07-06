package com.backbase.oss.codegen.java;

import static java.util.Arrays.asList;
import static java.util.Arrays.stream;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.DynamicContainer.*;
import static org.junit.jupiter.api.DynamicTest.*;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Stream.*;
import java.io.File;
import java.io.IOException;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DynamicNode;
import org.junit.jupiter.api.DynamicTest;
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
    static final String PROP_BASE = BoatSpringTemplatesTests.class.getSimpleName() + ".";
    static final boolean PROP_FAST = Boolean.parseBoolean(System.getProperty(PROP_BASE + "fast", "true"));
    static final String TEST_OUTPUT = System.getProperty(PROP_BASE + "output", "target/boat-spring-templates-tests");

    @BeforeAll
    static public void setUpClass() throws IOException {
        Files.createDirectories(Paths.get(TEST_OUTPUT));
        FileUtils.deleteDirectory(new File(TEST_OUTPUT, "src"));
    }

    static class Combination {
        static final List<String> CASES = asList("flx", "val", "opt", "req", "bin", "lmb", "wth", "utl");

        final String name;

        final boolean useBeanValidation;
        final boolean useOptional;

        final boolean addServletRequest;
        final boolean addBindingResult;
        final boolean useLombokAnnotations;
        final boolean useWithModifiers;

        final boolean reactive;
        final boolean apiUtil;

        Combination(int mask) {
            this.name = mask == 0
                ? "boat"
                : IntStream.range(0, CASES.size())
                    .filter(n -> (mask & (1 << n)) != 0)
                    .mapToObj(CASES::get)
                    .collect(joining("-", "boat-", ""));

            this.useBeanValidation = (mask & 1 << CASES.indexOf("val")) != 0;
            this.addBindingResult = (mask & 1 << CASES.indexOf("bin")) != 0;
            this.useOptional = (mask & 1 << CASES.indexOf("opt")) != 0;
            this.addServletRequest = (mask & 1 << CASES.indexOf("req")) != 0;
            this.useLombokAnnotations = (mask & 1 << CASES.indexOf("lmb")) != 0;
            this.useWithModifiers = (mask & 1 << CASES.indexOf("wth")) != 0;
            this.reactive = (mask & 1 << CASES.indexOf("flx")) != 0;
            this.apiUtil = (mask & 1 << CASES.indexOf("utl")) != 0;
        }

        static Stream<Combination> combinations(boolean minimal) {
            final List<Integer> cases = new ArrayList<>();

            if (minimal) {
                cases.add(0);
            }

            // generate all combinations
            // TODO find a better way to keep only the relevant combinations
            for (int mask = 0; mask < 1 << CASES.size(); mask++) {
                if (minimal && Integer.bitCount(mask) != 1) {
                    continue;
                }

                cases.add(mask);
            }

            if (minimal) {
                cases.add(~(1 << CASES.indexOf("flx")));
                //everything except flx & utl (because req & flx together is incorrect
                cases.add(-514);
                //everything except req
                cases.add(~(1 << CASES.indexOf("req")));
            }

            return cases.stream().map(Combination::new);
        }
    }

    /** dynamic suite creation **/
    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    @interface Check {
    }

    @TestFactory
    Stream<DynamicNode> withCombinations() {
        return Combination
            .combinations(PROP_FAST)
            .map(param -> dynamicContainer(param.name, testStream(param)));
    }

    Stream<DynamicTest> testStream(Combination param) {
        return concat(
            Stream.of(dynamicTest("generate", () -> generate(param))),
            stream(getClass().getDeclaredMethods())
                .filter(m -> m.isAnnotationPresent(Check.class))
                .map(m -> dynamicTest(m.getName(), () -> invoke(m))));
    }

    @SneakyThrows
    private void invoke(Method m) {
        m.invoke(this);
    }

    /** the actual testing code **/

    private Combination param;
    private List<File> files;

    void generate(Combination param) {
        this.param = param;
        this.files = generateFrom(null, param.name);
        // used in development
        // this.files = generateFrom("openapi-generator-originals/JavaSpring-4.3.1");

        assertThat(this.files, not(nullValue()));
        assertThat(this.files.size(), not(equalTo(0)));
    }

    @Check
    void useBeanValidation() {
        assertThat(findPattern("/api/.+\\.java$", "@Valid"),
            equalTo(this.param.useBeanValidation||this.param.addBindingResult));
        assertThat(findPattern("/model/.+\\.java$", "@Valid"),
            equalTo(this.param.useBeanValidation||this.param.addBindingResult));
    }

    @Check
    void useOptional() {
        assertThat(findPattern("/api/.+\\.java$", "Optional<(?!NativeWebRequest)[^>]+>"),
            equalTo(this.param.useOptional));
        assertThat(findPattern("/model/.+\\.java$", "Optional<[^>]+>"),
            is(false));
    }

    @Check
    void addServletRequest() {
        assertThat(findPattern("/api/.+\\.java$", "HttpServletRequest\\s+httpServletRequest"),
            equalTo(this.param.addServletRequest));
        assertThat(findPattern("/model/.+\\.java$", "HttpServletRequest\\s+httpServletRequest"),
            is(false));
    }

    @Check
    void addBindingResult(){
        assertThat(findPattern("/api/.+\\.java$", "BindingResult\\s+bindingResult"),
                equalTo(this.param.addBindingResult));
        assertThat(findPattern("/model/.+\\.java$", "HttpServletRequest\\s+httpServletRequest"),
                is(false));
    }

    @Check
    void useLombokAnnotations() {
        assertThat(findPattern("/api/.+\\.java$", "@lombok\\.Getter"),
            is(false));
        assertThat(findPattern("/model/.+\\.java$", "@lombok\\.Getter"),
            equalTo(this.param.useLombokAnnotations));
    }

    @Check
    void useWithModifiers() {
        assertThat(findPattern("/api/.+\\.java$", "\\s+with\\p{Upper}"),
            is(false));
        assertThat(findPattern("/model/.+\\.java$", "\\s+with\\p{Upper}"),
            equalTo(this.param.useWithModifiers));
    }

    private boolean findPattern(String filePattern, String linePattern) {
        final Predicate<String> fileMatch = Pattern.compile(filePattern).asPredicate();
        log.info("Files: {}", files);
        final List<String> selection = this.files.stream()
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

    private List<File> generateFrom(String templates, String combination) {
        final File input = new File("src/test/resources/boat-spring/openapi.yaml");
        final CodegenConfigurator gcf = new CodegenConfigurator();

        gcf.setGeneratorName(BoatSpringCodeGen.NAME);
        gcf.setInputSpec(input.getAbsolutePath());
        gcf.setOutputDir(TEST_OUTPUT + "/" + combination);

        GlobalSettings.setProperty(CodegenConstants.APIS, "");
        GlobalSettings.setProperty(CodegenConstants.API_DOCS, "true");
        GlobalSettings.setProperty(CodegenConstants.API_TESTS, "true");
        GlobalSettings.setProperty(CodegenConstants.MODELS, "");
        GlobalSettings.setProperty(CodegenConstants.MODEL_TESTS, "true");
        GlobalSettings.setProperty(CodegenConstants.MODEL_DOCS, "true");
        GlobalSettings.setProperty(CodegenConstants.SUPPORTING_FILES, "ApiUtil.java,pom.xml,OpenApiGeneratorApplication.java");


        gcf.setApiNameSuffix("-api");
        gcf.setModelNameSuffix(this.param.name);


        gcf.addAdditionalProperty(OptionalFeatures.USE_OPTIONAL, this.param.useOptional);

        gcf.addAdditionalProperty(BoatSpringCodeGen.USE_CLASS_LEVEL_BEAN_VALIDATION, true);
        gcf.addAdditionalProperty(BoatSpringCodeGen.ADD_SERVLET_REQUEST, this.param.addServletRequest);
        gcf.addAdditionalProperty(BoatSpringCodeGen.ADD_BINDING_RESULT,this.param.addBindingResult);
        if(this.param.addBindingResult){
            gcf.addAdditionalProperty(BeanValidationFeatures.USE_BEANVALIDATION, true);
        }else {
            gcf.addAdditionalProperty(BeanValidationFeatures.USE_BEANVALIDATION, this.param.useBeanValidation);
        }
        gcf.addAdditionalProperty(BoatSpringCodeGen.USE_LOMBOK_ANNOTATIONS, this.param.useLombokAnnotations);
        gcf.addAdditionalProperty(BoatSpringCodeGen.OPENAPI_NULLABLE, false);
        gcf.addAdditionalProperty(BoatSpringCodeGen.USE_WITH_MODIFIERS, this.param.useWithModifiers);
        gcf.addAdditionalProperty(SpringCodegen.REACTIVE, this.param.reactive);

        final String destPackage = this.param.name.replace('-', '.') + ".";

        gcf.setApiPackage(destPackage + "api");
        gcf.setModelPackage(destPackage + "model");
        gcf.setInvokerPackage(destPackage + "invoker");

        gcf.addAdditionalProperty(SpringCodegen.BASE_PACKAGE, destPackage + "base");
        gcf.addAdditionalProperty(SpringCodegen.CONFIG_PACKAGE, destPackage + "config");

        gcf.addAdditionalProperty(CodegenConstants.HIDE_GENERATION_TIMESTAMP, true);
        gcf.addAdditionalProperty(SpringCodegen.INTERFACE_ONLY, false);
        gcf.addAdditionalProperty(SpringCodegen.USE_TAGS, true);
        gcf.addAdditionalProperty(SpringCodegen.SKIP_DEFAULT_INTERFACE, false);
        gcf.addAdditionalProperty(CodegenConstants.ARTIFACT_ID, "boat-templates-tests");
        gcf.addAdditionalProperty("additionalDependencies", ""
            + "        <dependency>\n"
            + "            <groupId>jakarta.persistence</groupId>\n"
            + "            <artifactId>jakarta.persistence-api</artifactId>\n"
            + "            <version>2.2.3</version>\n"
            + "        </dependency>\n"
            + "        <dependency>\n"
            + "            <groupId>jakarta.servlet</groupId>\n"
            + "            <artifactId>jakarta.servlet-api</artifactId>\n"
            + "            <version>4.0.4</version>\n"
            + "        </dependency>\n"
            + "        <dependency>\n"
            + "            <groupId>org.springframework.boot</groupId>\n"
            + "            <artifactId>spring-boot-starter-webflux</artifactId>\n"
            + "        </dependency>\n"
            + "        <dependency>\n"
            + "            <groupId>org.openapitools</groupId>\n"
            + "            <artifactId>jackson-databind-nullable</artifactId>\n"
            + "            <version>0.2.1</version>\n"
            + "        </dependency>\n"
            + "");

        gcf.setTemplateDir(templates);

        final ClientOptInput coi = gcf.toClientOptInput();

        return new DefaultGenerator().opts(coi).generate();
    }
}
