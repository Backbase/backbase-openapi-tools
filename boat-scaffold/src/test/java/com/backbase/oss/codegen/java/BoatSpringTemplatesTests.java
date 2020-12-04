package com.backbase.oss.codegen.java;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import lombok.SneakyThrows;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.openapitools.codegen.ClientOptInput;
import org.openapitools.codegen.CodegenConstants;
import org.openapitools.codegen.DefaultGenerator;
import org.openapitools.codegen.config.CodegenConfigurator;
import org.openapitools.codegen.config.GlobalSettings;
import org.openapitools.codegen.languages.SpringCodegen;
import org.openapitools.codegen.languages.features.BeanValidationFeatures;
import org.openapitools.codegen.languages.features.OptionalFeatures;

import static java.util.Arrays.asList;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;

/**
 * These tests verifies that the code generation works for various combinations of configuration
 * parameters; the projects that are generated are later compiled in the integration test phase.
 */
public class BoatSpringTemplatesTests {

    private static final String PROP_BASE = BoatSpringTemplatesTests.class.getSimpleName() + ".";
    private static final boolean PROP_FAST = Boolean.getBoolean(PROP_BASE + "fast");

    private static final String[] CASES = {"val", "opt", "req", "lmb", "nul", "unq", "wth"};

    static public Stream<Arguments> parameters() {
        final List<Object[]> data = new ArrayList<>();

        if (PROP_FAST) {
            data.add(new Object[]{caseName(0), 0});
        }

        // generate all combinations
        for (int mask = 0; mask < 1 << CASES.length; mask++) {
            if (PROP_FAST && Integer.bitCount(mask) != 1) {
                continue;
            }

            data.add(new Object[]{caseName(mask), mask,});
        }

        if (PROP_FAST) {
            data.add(new Object[]{caseName(-1), -1});
        }

        Stream<Arguments> argumentsStream = data.stream().map(objects -> Arguments.of(objects[0], objects[1]));

        return argumentsStream;
    }

    static private String caseName(int mask) {
        return mask == 0
            ? "backbase"
            : IntStream.range(0, CASES.length)
            .filter(n -> (mask & (1 << n)) != 0)
            .mapToObj(n -> CASES[n])
            .collect(joining("-", "backbase-", ""));
    }

    static private final String TEST_OUTPUS = System.getProperty(PROP_BASE + "output", "target/test-outputs");

    @BeforeAll
    static public void setUpClass() throws IOException {
        Files.createDirectories(Paths.get(TEST_OUTPUS));
        FileUtils.deleteDirectory(new File(TEST_OUTPUS, "src"));
    }

    private final String caseName;

    private final boolean useBeanValidation;
    private final boolean useOptional;

    private final boolean addServletRequest;
    private final boolean useLombokAnnotations;
    private final boolean openApiNullable;
    private final boolean useSetForUniqueItems;
    private final boolean useWithModifiers;

    static private List<File> files;

    public BoatSpringTemplatesTests(String caseName, int mask) {
        this.caseName = caseName;

        this.useBeanValidation = (mask & 1 << 0) != 0;
        this.useOptional = (mask & 1 << 1) != 0;
        this.addServletRequest = (mask & 1 << 2) != 0;
        this.useLombokAnnotations = (mask & 1 << 3) != 0;
        this.openApiNullable = (mask & 1 << 4) != 0;
        this.useSetForUniqueItems = (mask & 1 << 5) != 0;
        this.useWithModifiers = (mask & 1 << 6) != 0;
    }

    @BeforeEach
    public void generate() throws IOException {
        final Path marker = Paths.get(TEST_OUTPUS, "src", "." + this.caseName);

        // generate once per case name
        if (!Files.exists(marker)) {
            files = generateFrom(null);
            // used in development
            // this.files = generateFrom("openapi-generator-originals/JavaSpring-4.3.1");
            Files.write(marker, asList(""));
        }

        assertThat(files, not(nullValue()));
        assertThat(files.size(), not(equalTo(0)));
    }

    @ParameterizedTest
    @MethodSource("parameters")
    public void useBeanValidation() {
        assertThat(findPattern("/api/.+\\.java$", "@Valid"),
            equalTo(this.useBeanValidation));
        assertThat(findPattern("/model/.+\\.java$", "@Valid"),
            equalTo(this.useBeanValidation));
    }

    @ParameterizedTest
    @MethodSource("parameters")
    public void useOptional() {
        assertThat(findPattern("/api/.+\\.java$", "Optional<(?!NativeWebRequest)[^>]+>"),
            equalTo(this.useOptional));
        assertThat(findPattern("/model/.+\\.java$", "Optional<[^>]+>"),
            is(false));
    }

    @ParameterizedTest
    @MethodSource("parameters")
    public void addServletRequest() {
        assertThat(findPattern("/api/.+\\.java$", "HttpServletRequest\\s+httpServletRequest"),
            equalTo(this.addServletRequest));
        assertThat(findPattern("/model/.+\\.java$", "HttpServletRequest\\s+httpServletRequest"),
            is(false));
    }

    @ParameterizedTest
    @MethodSource("parameters")
    public void useLombokAnnotations() {
        assertThat(findPattern("/api/.+\\.java$", "@lombok\\.Getter"),
            is(false));
        assertThat(findPattern("/model/.+\\.java$", "@lombok\\.Getter"),
            equalTo(this.useLombokAnnotations));
    }

    @ParameterizedTest
    @MethodSource("parameters")
    public void openApiNullable() {
        assertThat(findPattern("/api/.+\\.java$", "JsonNullable<[^>]+>"),
            is(false));
        assertThat(findPattern("/model/.+\\.java$", "JsonNullable<[^>]+>"),
            equalTo(this.openApiNullable));
    }

    @ParameterizedTest
    @MethodSource("parameters")
    public void useSetForUniqueItems() {
        assertThat(findPattern("/api/.+\\.java$", "(java\\.util\\.)?Set<.+>"),
            equalTo(this.useSetForUniqueItems));
        assertThat(findPattern("/model/.+\\.java$", "(java\\.util\\.)?Set<.+>"),
            equalTo(this.useSetForUniqueItems));
    }

    @ParameterizedTest
    @MethodSource("parameters")
    public void useWithModifiers() {
        assertThat(findPattern("/api/.+\\.java$", "\\s+with\\p{Upper}"),
            is(false));
        assertThat(findPattern("/model/.+\\.java$", "\\s+with\\p{Upper}"),
            equalTo(this.useWithModifiers));
    }

    private boolean findPattern(String filePattern, String linePattern) {
        final Predicate<String> fileMatch = Pattern.compile(filePattern).asPredicate();
        final List<String> selection = files.stream()
            .map(File::getPath)
            .map(path -> path.replace(File.separatorChar, '/'))
            .filter(fileMatch)
            .collect(toList());

        assertThat(selection, not(hasSize(0)));

        final Predicate<String> lineMatch = Pattern.compile(linePattern).asPredicate();
        return selection.stream()
            .filter(file -> contentMatches(file, lineMatch))
            .findAny()
            .isPresent();
    }

    @SneakyThrows
    private boolean contentMatches(String path, Predicate<String> lineMatch) {
        try (final Stream<String> lines = Files.lines(Paths.get(path))) {
            return lines.anyMatch(lineMatch);
        }
    }

    private List<File> generateFrom(String templates) {
        final File input = new File("src/test/resources/boat-spring/openapi.yaml");
        final CodegenConfigurator cf = new CodegenConfigurator();

        cf.setGeneratorName(BoatSpringCodeGen.NAME);
        cf.setInputSpec(input.getAbsolutePath());
        cf.setOutputDir(TEST_OUTPUS);

        GlobalSettings.setProperty(CodegenConstants.APIS, "");
        GlobalSettings.setProperty(CodegenConstants.API_DOCS, "true");
        GlobalSettings.setProperty(CodegenConstants.API_TESTS, "true");
        GlobalSettings.setProperty(CodegenConstants.MODELS, "");
        GlobalSettings.setProperty(CodegenConstants.MODEL_TESTS, "true");
        GlobalSettings.setProperty(CodegenConstants.MODEL_DOCS, "true");
        GlobalSettings.setProperty(CodegenConstants.SUPPORTING_FILES, "");

        cf.setApiNameSuffix("-api");
        cf.setModelNameSuffix(this.caseName);

        cf.addAdditionalProperty(BeanValidationFeatures.USE_BEANVALIDATION, this.useBeanValidation);
        cf.addAdditionalProperty(OptionalFeatures.USE_OPTIONAL, this.useOptional);

        cf.addAdditionalProperty(BoatSpringCodeGen.USE_CLASS_LEVEL_BEAN_VALIDATION, true);
        cf.addAdditionalProperty(BoatSpringCodeGen.ADD_SERVLET_REQUEST, this.addServletRequest);
        cf.addAdditionalProperty(BoatSpringCodeGen.USE_LOMBOK_ANNOTATIONS, this.useLombokAnnotations);
        cf.addAdditionalProperty(BoatSpringCodeGen.USE_SET_FOR_UNIQUE_ITEMS, this.useSetForUniqueItems);
        cf.addAdditionalProperty(BoatSpringCodeGen.OPENAPI_NULLABLE, this.openApiNullable);
        cf.addAdditionalProperty(BoatSpringCodeGen.USE_WITH_MODIFIERS, this.useWithModifiers);

        final String destPackage = this.caseName.replace('-', '.') + ".";

        cf.setApiPackage(destPackage + "api");
        cf.setModelPackage(destPackage + "model");
        cf.setInvokerPackage(destPackage + "invoker");

        cf.addAdditionalProperty(SpringCodegen.BASE_PACKAGE, destPackage + "base");
        cf.addAdditionalProperty(SpringCodegen.CONFIG_PACKAGE, destPackage + "config");

        cf.addAdditionalProperty(CodegenConstants.HIDE_GENERATION_TIMESTAMP, true);
        cf.addAdditionalProperty(SpringCodegen.INTERFACE_ONLY, true);
        cf.addAdditionalProperty(SpringCodegen.USE_TAGS, true);
        cf.addAdditionalProperty(SpringCodegen.SKIP_DEFAULT_INTERFACE, false);
        cf.addAdditionalProperty(CodegenConstants.ARTIFACT_ID, "boat-templates-tests");
        cf.addAdditionalProperty("additionalDependencies", ""
            + "        <dependency>\n"
            + "            <groupId>jakarta.persistence</groupId>\n"
            + "            <artifactId>jakarta.persistence-api</artifactId>\n"
            + "            <version>2.2.3</version>\n"
            + "        </dependency>");

        cf.setTemplateDir(templates);

        final ClientOptInput coi = cf.toClientOptInput();

        return new DefaultGenerator().opts(coi).generate();
    }
}
