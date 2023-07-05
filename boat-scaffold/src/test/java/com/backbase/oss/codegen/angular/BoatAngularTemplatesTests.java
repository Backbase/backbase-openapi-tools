package com.backbase.oss.codegen.angular;

import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import lombok.SneakyThrows;
import org.apache.commons.io.FileUtils;
import org.assertj.core.api.Assert;
import org.junit.jupiter.api.*;
import org.openapitools.codegen.ClientOptInput;
import org.openapitools.codegen.CodegenConstants;
import org.openapitools.codegen.DefaultGenerator;
import org.openapitools.codegen.config.CodegenConfigurator;
import org.openapitools.codegen.config.GlobalSettings;
import org.openapitools.codegen.languages.AbstractTypeScriptClientCodegen;
import org.openapitools.codegen.utils.SemVer;
import java.io.File;
import java.io.IOException;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static java.util.Arrays.stream;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Stream.concat;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.DynamicContainer.dynamicContainer;
import static org.junit.jupiter.api.DynamicTest.dynamicTest;

/**
 * These tests verifies that the code generation works for various combinations of configuration
 * parameters; the projects that are generated are later compiled in the integration test phase.
 */
class BoatAngularTemplatesTests {
    static final String PROP_BASE = BoatAngularTemplatesTests.class.getSimpleName() + ".";
    static final boolean PROP_FAST = Boolean.valueOf(System.getProperty(PROP_BASE + "fast", "true"));
    static final String TEST_OUTPUT = System.getProperty(PROP_BASE + "output", "target/boat-angular-templates-tests");
    /**
     * the actual testing code
     **/

    private Combination param;
    private List<File> files;

    @BeforeAll
    static public void setUpClass() throws IOException {
        Files.createDirectories(Paths.get(TEST_OUTPUT));
        FileUtils.deleteDirectory(new File(TEST_OUTPUT));
    }

    @TestFactory
    Stream<DynamicNode> withCombinations() {
        return Combination
                .combinations(PROP_FAST)
                .map(param -> dynamicContainer(param.name, testStream(param)));
    }

    Stream<DynamicTest> testStream(BoatAngularTemplatesTests.Combination param) {
        return concat(
                Stream.of(dynamicTest("generate", () -> generate(param))),
                stream(getClass().getDeclaredMethods())
                        .filter(m -> m.isAnnotationPresent(BoatAngularTemplatesTests.Check.class))
                        .map(m -> dynamicTest(m.getName(), () -> invoke(m))));
    }

    @SneakyThrows
    private void invoke(Method m) {
        m.invoke(this);
    }

    void generate(Combination param) {
        this.param = param;
        this.files = generateFrom(null);
        // used in development
        // this.files = generateFrom(param, "openapi-generator-originals/JavaSpring-4.3.1");

        assertThat(this.files, not(nullValue()));
        assertThat(this.files.size(), not(equalTo(0)));
    }

    @Check
    public void ngPackageConfig() throws IOException {
        var ngPackageFiles = selectFiles("/ng-package\\.json$");
        if (this.param.npmName) {
            assertThat(ngPackageFiles, hasSize(1));
            List<String> content;
            try (var lines = Files.lines(Paths.get(ngPackageFiles.get(0)))) {
                content = lines.collect(Collectors.toList());
            }
            var dist = Optional.ofNullable(this.param.buildDist).orElse("dist");
            assertThat(content, hasItem(containsString("\"dest\": \"" + dist + "\"")));
        } else {
            assertThat(ngPackageFiles, hasSize(0));
        }
    }

    @Check
    public void npmName() {
        assertThat(
                findPattern(selectFiles("/package\\.json$"), "\"name\": \"@example/angular-http\""),
                equalTo(this.param.npmName));
    }

    @Check
    public void npmRepository() {
        assertThat(
                findPattern(selectFiles("/package\\.json$"), "\"registry\":"),
                equalTo(this.param.npmRepository && this.param.npmName));
    }

    @Check
    public void withMocks() {
        assertThat(
                findPattern(selectFiles("/mocks/.+\\.service\\.mocks\\.array\\.js$"), "module.exports"),
                equalTo(this.param.withMocks));
    }

    @Check
    public void apiModulePrefix() {
        assertThat(
                findPattern("/api\\.module\\.ts$", "export class BoatApiModule"),
                equalTo(this.param.apiModulePrefix));
        assertThat(
                findPattern("/api\\.module\\.ts$", "export class ApiModule"),
                equalTo(!this.param.apiModulePrefix));
    }

    @Check
    public void apiModulePrefixConfiguration() {
        assertThat(
                findPattern("/configuration\\.ts$", "export class BoatConfiguration"),
                equalTo(this.param.apiModulePrefix));
        assertThat(
                findPattern("/configuration\\.ts$", "export class Configuration"),
                equalTo(!this.param.apiModulePrefix));
        assertThat(
                findPattern("/configuration\\.ts$", "export interface BoatConfigurationParameters"),
                equalTo(this.param.apiModulePrefix));
        assertThat(
                findPattern("/configuration\\.ts$", "export interface ConfigurationParameters"),
                equalTo(!this.param.apiModulePrefix));
    }

    @Check
    public void apiModulePrefixBasePath() {
        assertThat(
                findPattern("/variables\\.ts$", "export const BOAT_BASE_PATH"),
                equalTo(this.param.apiModulePrefix));
        assertThat(
                findPattern("/variables\\.ts$", "export const BASE_PATH"),
                equalTo(!this.param.apiModulePrefix));
    }

    @Check
    public void serviceSuffix() {
        assertThat(
                findPattern("/api/.+\\.service.ts$$", "export class .*Gateway "),
                equalTo(this.param.serviceSuffix));
        assertThat(
                findPattern("/api/.+\\.service.ts$", "export class .*Service "),
                equalTo(!this.param.serviceSuffix));
    }

    private boolean findPattern(String filePattern, String linePattern) {
        final List<String> selection = selectFiles(filePattern);
        assertThat(selection, not(hasSize(0)));
        return findPattern(selection, linePattern);
    }

    private List<String> selectFiles(String filePattern) {
        final Predicate<String> fileMatch = Pattern.compile(filePattern).asPredicate();
        return this.files.stream()
                .map(File::getPath)
                .map(path -> path.replace(File.separatorChar, '/'))
                .filter(fileMatch)
                .collect(toList());
    }

    private boolean findPattern(List<String> selection, String linePattern) {
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

    private List<File> generateFrom(String templates) {
        final File input = new File("src/test/resources/boat-spring/openapi.yaml");
        final CodegenConfigurator cf = new CodegenConfigurator();

        cf.setGeneratorName(BoatAngularGenerator.NAME);
        cf.setInputSpec(input.getAbsolutePath());
        cf.setOutputDir(TEST_OUTPUT);

        GlobalSettings.setProperty(CodegenConstants.APIS, "");
        GlobalSettings.setProperty(CodegenConstants.API_DOCS, "true");
        GlobalSettings.setProperty(CodegenConstants.API_TESTS, "true");
        GlobalSettings.setProperty(CodegenConstants.MODELS, "");
        GlobalSettings.setProperty(CodegenConstants.MODEL_TESTS, "true");
        GlobalSettings.setProperty(CodegenConstants.MODEL_DOCS, "true");
        GlobalSettings.setProperty(CodegenConstants.SUPPORTING_FILES, "");

        cf.setApiNameSuffix("-api");
        cf.setModelNameSuffix(this.param.name);

        cf.addAdditionalProperty(BoatAngularGenerator.WITH_MOCKS, this.param.withMocks);
        if (this.param.npmRepository) {
            cf.addAdditionalProperty(BoatAngularGenerator.NPM_REPOSITORY, "https://registry.example.com/npm");
        }
        if (this.param.npmName) {
            cf.addAdditionalProperty(AbstractTypeScriptClientCodegen.NPM_NAME, "@example/angular-http");
        }
        if (this.param.apiModulePrefix) {
            cf.addAdditionalProperty(BoatAngularGenerator.API_MODULE_PREFIX, "Boat");
        }
        if (this.param.serviceSuffix) {
            cf.addAdditionalProperty(BoatAngularGenerator.SERVICE_SUFFIX, "Gateway");
        }

        if (!Objects.isNull(this.param.ngVersion)) {
            cf.addAdditionalProperty(BoatAngularGenerator.NG_VERSION, this.param.ngVersion);
        }

        if (!Objects.isNull(this.param.specVersion)) {
            cf.addAdditionalProperty(BoatAngularGenerator.SPEC_VERSION, this.param.specVersion);
        }

        if (!Objects.isNull(this.param.specArtifactId)) {
            cf.addAdditionalProperty(BoatAngularGenerator.SPEC_ARTIFACT_ID, this.param.specArtifactId);
        }

        if (!Objects.isNull(this.param.specGroupId)) {
            cf.addAdditionalProperty(BoatAngularGenerator.SPEC_GROUP_ID, this.param.specGroupId);
        }

        if (!Objects.isNull(this.param.buildDist)) {
            cf.addAdditionalProperty(BoatAngularGenerator.BUILD_DIST, this.param.buildDist);
        }

        final String destPackage = this.param.name.replace('-', '.') + ".";

        cf.setApiPackage(destPackage + "api");
        cf.setModelPackage(destPackage + "model");
        cf.setInvokerPackage(destPackage + "invoker");

        cf.addAdditionalProperty(CodegenConstants.ARTIFACT_ID, "boat-angular-templates-tests");
        cf.setTemplateDir(templates);

        final ClientOptInput coi = cf.toClientOptInput();

        return new DefaultGenerator().opts(coi).generate();
    }

    /**
     * dynamic suite creation
     **/
    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    @interface Check {
    }

    static class Combination {
        static final String[] CASES = {"nom", "reg", "mck", "pir", "amp", "srv"};


        final String name;
        final String ngVersion;
        final String specVersion;
        final String specArtifactId;
        final String specGroupId;
        final String buildDist;
        final boolean npmRepository;
        final boolean npmName;
        final boolean withMocks;
        final boolean apiModulePrefix;
        final boolean serviceSuffix;

        Combination(int mask, String ngVersion) {
            this.name = caseName(mask) + "-ng-" + ngVersion;
            this.ngVersion = ngVersion;
            this.specVersion = mask == 0 ? "1.0.0" : mask == 1 ? "2.0.0" : null;
            this.specArtifactId = "specArtifactId";
            this.specGroupId = "specGroupId";
            this.buildDist = mask > 0 ? caseName(mask) : null;
            this.npmName = (mask & 1) != 0;
            this.npmRepository = (mask & 1 << 1) != 0;
            this.withMocks = (mask & 1 << 2) != 0;
            this.apiModulePrefix = (mask & 1 << 4) != 0;
            this.serviceSuffix = (mask & 1 << 5) != 0;
        }

        static private String caseName(int mask) {
            return mask == 0
                    ? "backbase"
                    : IntStream.range(0, CASES.length)
                    .filter(n -> (mask & (1 << n)) != 0)
                    .mapToObj(n -> CASES[n])
                    .collect(joining("-", "backbase-", ""));
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
            final String[] ngVersions = {
                "16.0.0",
                "14.0.0",
                "13.0.0",
                "12.0.0",
                "11.0.0",
                "10.0.0",
                null,
            };

            return cases.stream().flatMap((mask) -> stream(ngVersions).map(ngVersion -> new Combination(mask, ngVersion)));
        }
    }
}
