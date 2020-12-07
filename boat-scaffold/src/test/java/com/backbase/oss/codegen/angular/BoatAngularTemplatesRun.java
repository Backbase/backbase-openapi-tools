package com.backbase.oss.codegen.angular;

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

import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;

/**
 * These tests verify that the code generation works for various combinations of configuration
 * parameters; the projects that are generated are later compiled in the integration test phase.
 */
@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
class BoatAngularTemplatesRun {
    static final String PROP_BASE = BoatAngularTemplatesTests.class.getSimpleName() + ".";
    static final boolean PROP_FAST = Boolean.getBoolean(PROP_BASE + "fast");

    static class Combination {
        static final String[] CASES = {"nom", "reg", "mck", "pir", "amp", "srv"};


        final String name;
        final boolean npmRepository;
        final boolean npmName;
        final boolean withMocks;
        final boolean providedInRoot;
        final boolean apiModulePrefix;
        final boolean serviceSuffix;

        Combination(int mask) {
            this.name = caseName(mask);
            this.npmName = (mask & 1) != 0;
            this.npmRepository = (mask & 1 << 1) != 0;
            this.withMocks = (mask & 1 << 2) != 0;
            this.providedInRoot = (mask & 1 << 3) != 0;
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

            return cases.stream().map(integer -> new Combination(integer));
        }
    }

    static final String TEST_OUTPUTS = System.getProperty(PROP_BASE + "output", "target/test-outputs");

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
    public void npmName() {
        assertThat(
            findPattern(selectFiles("/package\\.json$"), "\"name\": \"@example/angular-http\""),
            equalTo(this.param.npmName)
        );
    }

    @Test
    public void npmRepository() {
        assertThat(
            findPattern(selectFiles("/package\\.json$"), "\"registry\":"),
            equalTo(this.param.npmRepository && this.param.npmName)
        );
    }

    @Test
    public void withMocks() {
        assertThat(
            findPattern(selectFiles("/api/.+\\.service\\.mocks\\.ts$"), "MocksProvider: Provider = createMocks"),
            equalTo(this.param.withMocks)
        );
    }

    @Test
    public void providedInRoot() {
        assertThat(
            findPattern("/api/.+\\.service.ts$", "providedIn: 'root'"),
            equalTo(this.param.providedInRoot)
        );
        assertThat(
            findPattern("/api\\.module\\.ts$", "providers: \\[]"),
            equalTo(this.param.providedInRoot)
        );
    }

    @Test
    public void apiModulePrefix() {
        assertThat(
            findPattern("/api\\.module\\.ts$", "export class BoatApiModule"),
            equalTo(this.param.apiModulePrefix)
        );
        assertThat(
            findPattern("/api\\.module\\.ts$", "export class ApiModule"),
            equalTo(!this.param.apiModulePrefix)
        );
    }

    @Test
    public void serviceSuffix() {
        assertThat(
            findPattern("/api/.+\\.service.ts$$", "export class .*Gateway "),
            equalTo(this.param.serviceSuffix)
        );
        assertThat(
            findPattern("/api/.+\\.service.ts$", "export class .*Service "),
            equalTo(!this.param.serviceSuffix)
        );
    }

    private boolean findPattern(String filePattern, String linePattern) {
        List<String> selection = selectFiles(filePattern);
        assertThat(selection, not(hasSize(0)));
        return findPattern(selection, linePattern);
    }

    private List<String> selectFiles(String filePattern) {
        final Predicate<String> fileMatch = Pattern.compile(filePattern).asPredicate();
        return files.stream()
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
        cf.setOutputDir(TEST_OUTPUTS);

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
            cf.addAdditionalProperty(BoatAngularGenerator.NPM_NAME, "@example/angular-http");
        }
        cf.addAdditionalProperty(BoatAngularGenerator.PROVIDED_IN_ROOT, this.param.providedInRoot);
        if (this.param.apiModulePrefix) {
            cf.addAdditionalProperty(BoatAngularGenerator.API_MODULE_PREFIX, "Boat");
        }
        if (this.param.serviceSuffix) {
            cf.addAdditionalProperty(BoatAngularGenerator.SERVICE_SUFFIX, "Gateway");
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

}
