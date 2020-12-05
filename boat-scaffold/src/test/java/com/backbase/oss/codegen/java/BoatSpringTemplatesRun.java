package com.backbase.oss.codegen.java;

import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;
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
 */
@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
class BoatSpringTemplatesRun {
    static final String PROP_BASE = BoatSpringCodeGenTests.class.getSimpleName() + ".";
    static final boolean PROP_FAST = Boolean.getBoolean(PROP_BASE + "fast");

    static class Combination {
        static final String[] CASES = {"val", "opt", "req", "lmb", "nul", "unq", "wth"};

        final String name;

        final boolean useBeanValidation;
        final boolean useOptional;

        final boolean addServletRequest;
        final boolean useLombokAnnotations;
        final boolean openApiNullable;
        final boolean useSetForUniqueItems;
        final boolean useWithModifiers;

        Combination(int mask) {
            this.name = mask == 0
                ? "boat"
                : IntStream.range(0, CASES.length)
                    .filter(n -> (mask & (1 << n)) != 0)
                    .mapToObj(n -> CASES[n])
                    .collect(joining("-", "boat-", ""));

            this.useBeanValidation = (mask & 1 << 0) != 0;
            this.useOptional = (mask & 1 << 1) != 0;
            this.addServletRequest = (mask & 1 << 2) != 0;
            this.useLombokAnnotations = (mask & 1 << 3) != 0;
            this.openApiNullable = (mask & 1 << 4) != 0;
            this.useSetForUniqueItems = (mask & 1 << 5) != 0;
            this.useWithModifiers = (mask & 1 << 6) != 0;
        }

        static Stream<Combination> combinations(boolean minimal) {
            final List<Integer> cases = new ArrayList<>();

            if (minimal) {
                cases.add(0);
            }

            // generate all combinations
            for (int mask = 0; mask < 1 << CASES.length; mask++) {
                if (minimal && Integer.bitCount(mask) != 1) {
                    continue;
                }

                cases.add(mask);
            }

            if (minimal) {
                cases.add(-1);
            }

            return cases.stream().map(Combination::new);
        }
    }

    static final String TEST_OUTPUS = System.getProperty(PROP_BASE + "output", "target/test-outputs");

    private final Combination param;
    private List<File> files;

    void generate() {
        this.files = generateFrom(null);
        // used in development
        // this.files = generateFrom(param, "openapi-generator-originals/JavaSpring-4.3.1");

        assertThat(this.files, not(nullValue()));
        assertThat(this.files.size(), not(equalTo(0)));
    }

    @Test
    void useBeanValidation() {
        assertThat(findPattern("/api/.+\\.java$", "@Valid"),
            equalTo(this.param.useBeanValidation));
        assertThat(findPattern("/model/.+\\.java$", "@Valid"),
            equalTo(this.param.useBeanValidation));
    }

    @Test
    void useOptional() {
        assertThat(findPattern("/api/.+\\.java$", "Optional<(?!NativeWebRequest)[^>]+>"),
            equalTo(this.param.useOptional));
        assertThat(findPattern("/model/.+\\.java$", "Optional<[^>]+>"),
            is(false));
    }

    @Test
    void addServletRequest() {
        assertThat(findPattern("/api/.+\\.java$", "HttpServletRequest\\s+httpServletRequest"),
            equalTo(this.param.addServletRequest));
        assertThat(findPattern("/model/.+\\.java$", "HttpServletRequest\\s+httpServletRequest"),
            is(false));
    }

    @Test
    void useLombokAnnotations() {
        assertThat(findPattern("/api/.+\\.java$", "@lombok\\.Getter"),
            is(false));
        assertThat(findPattern("/model/.+\\.java$", "@lombok\\.Getter"),
            equalTo(this.param.useLombokAnnotations));
    }

    @Test
    void openApiNullable() {
        assertThat(findPattern("/api/.+\\.java$", "JsonNullable<[^>]+>"),
            is(false));
        assertThat(findPattern("/model/.+\\.java$", "JsonNullable<[^>]+>"),
            equalTo(this.param.openApiNullable));
    }

    @Test
    void useSetForUniqueItems() {
        assertThat(findPattern("/api/.+\\.java$", "(java\\.util\\.)?Set<.+>"),
            equalTo(this.param.useSetForUniqueItems));
        assertThat(findPattern("/model/.+\\.java$", "(java\\.util\\.)?Set<.+>"),
            equalTo(this.param.useSetForUniqueItems));
    }

    @Test
    void useWithModifiers() {
        assertThat(findPattern("/api/.+\\.java$", "\\s+with\\p{Upper}"),
            is(false));
        assertThat(findPattern("/model/.+\\.java$", "\\s+with\\p{Upper}"),
            equalTo(this.param.useWithModifiers));
    }

    private boolean findPattern(String filePattern, String linePattern) {
        final Predicate<String> fileMatch = Pattern.compile(filePattern).asPredicate();
        final List<String> selection = this.files.stream()
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
        final CodegenConfigurator gcf = new CodegenConfigurator();

        gcf.setGeneratorName(BoatSpringCodeGen.NAME);
        gcf.setInputSpec(input.getAbsolutePath());
        gcf.setOutputDir(TEST_OUTPUS);

        GlobalSettings.setProperty(CodegenConstants.APIS, "");
        GlobalSettings.setProperty(CodegenConstants.API_DOCS, "true");
        GlobalSettings.setProperty(CodegenConstants.API_TESTS, "true");
        GlobalSettings.setProperty(CodegenConstants.MODELS, "");
        GlobalSettings.setProperty(CodegenConstants.MODEL_TESTS, "true");
        GlobalSettings.setProperty(CodegenConstants.MODEL_DOCS, "true");
        GlobalSettings.setProperty(CodegenConstants.SUPPORTING_FILES, "");

        gcf.setApiNameSuffix("-api");
        gcf.setModelNameSuffix(this.param.name);

        gcf.addAdditionalProperty(BeanValidationFeatures.USE_BEANVALIDATION, this.param.useBeanValidation);
        gcf.addAdditionalProperty(OptionalFeatures.USE_OPTIONAL, this.param.useOptional);

        gcf.addAdditionalProperty(BoatSpringCodeGen.USE_CLASS_LEVEL_BEAN_VALIDATION, true);
        gcf.addAdditionalProperty(BoatSpringCodeGen.ADD_SERVLET_REQUEST, this.param.addServletRequest);
        gcf.addAdditionalProperty(BoatSpringCodeGen.USE_LOMBOK_ANNOTATIONS, this.param.useLombokAnnotations);
        gcf.addAdditionalProperty(BoatSpringCodeGen.USE_SET_FOR_UNIQUE_ITEMS, this.param.useSetForUniqueItems);
        gcf.addAdditionalProperty(BoatSpringCodeGen.OPENAPI_NULLABLE, this.param.openApiNullable);
        gcf.addAdditionalProperty(BoatSpringCodeGen.USE_WITH_MODIFIERS, this.param.useWithModifiers);

        final String destPackage = this.param.name.replace('-', '.') + ".";

        gcf.setApiPackage(destPackage + "api");
        gcf.setModelPackage(destPackage + "model");
        gcf.setInvokerPackage(destPackage + "invoker");

        gcf.addAdditionalProperty(SpringCodegen.BASE_PACKAGE, destPackage + "base");
        gcf.addAdditionalProperty(SpringCodegen.CONFIG_PACKAGE, destPackage + "config");

        gcf.addAdditionalProperty(CodegenConstants.HIDE_GENERATION_TIMESTAMP, true);
        gcf.addAdditionalProperty(SpringCodegen.INTERFACE_ONLY, true);
        gcf.addAdditionalProperty(SpringCodegen.USE_TAGS, true);
        gcf.addAdditionalProperty(SpringCodegen.SKIP_DEFAULT_INTERFACE, false);
        gcf.addAdditionalProperty(CodegenConstants.ARTIFACT_ID, "boat-templates-tests");
        gcf.addAdditionalProperty("additionalDependencies", ""
            + "        <dependency>\n"
            + "            <groupId>jakarta.persistence</groupId>\n"
            + "            <artifactId>jakarta.persistence-api</artifactId>\n"
            + "            <version>2.2.3</version>\n"
            + "        </dependency>");

        gcf.setTemplateDir(templates);

        final ClientOptInput coi = gcf.toClientOptInput();

        return new DefaultGenerator().opts(coi).generate();
    }
}
