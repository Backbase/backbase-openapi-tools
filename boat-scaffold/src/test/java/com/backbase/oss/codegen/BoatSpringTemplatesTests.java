package com.backbase.oss.codegen;

import static java.lang.String.format;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import lombok.RequiredArgsConstructor;
import org.apache.commons.io.FileUtils;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.openapitools.codegen.ClientOptInput;
import org.openapitools.codegen.CodegenConstants;
import org.openapitools.codegen.DefaultGenerator;
import org.openapitools.codegen.config.CodegenConfigurator;
import org.openapitools.codegen.config.GlobalSettings;
import org.openapitools.codegen.languages.SpringCodegen;

/**
 * These tests verifies that the code generation works for various combinations of configuration
 * parameters; the projects that are generated are later compiled in the integration test phase.
 */
@RunWith(Parameterized.class)
@RequiredArgsConstructor
public class BoatSpringTemplatesTests {

    @Parameterized.Parameters(name = "{0}")
    static public Object parameters() {
        final List<Object[]> data = new ArrayList<>();

        // all combinations of
        for (int mask = 0; mask < 1 << 6; mask++) {
            final boolean useBeanValidation = (mask & 1 << 0) != 0;
            final boolean useOptional = (mask & 1 << 1) != 0;
            final boolean useLombokAnnotations = (mask & 1 << 2) != 0;
            final boolean fullJavaUtil = (mask & 1 << 3) != 0;
            final boolean openApiNullable = (mask & 1 << 4) != 0;
            final boolean useSetForUniqueItems = (mask & 1 << 5) != 0;

            data.add(new Object[] {
                caseName(useBeanValidation,
                    useOptional,
                    useLombokAnnotations,
                    fullJavaUtil,
                    openApiNullable,
                    useSetForUniqueItems),
                useBeanValidation,
                useOptional,
                useLombokAnnotations,
                fullJavaUtil,
                openApiNullable,
                useSetForUniqueItems,
            });
        }

        return data;
    }

    static private String caseName(
        boolean useBeanValidation,
        boolean useOptional,
        boolean useLombokAnnotations,
        boolean fullJavaUtil,
        boolean openApiNullable,
        boolean useSetForUniqueItems) {

        return format("backbase%s%s%s%s%s%s",
            useBeanValidation ? "-val" : "",
            useOptional ? "-opt" : "",
            useLombokAnnotations ? "-lmb" : "",
            fullJavaUtil ? "-utl" : "",
            openApiNullable ? "-nul" : "",
            useSetForUniqueItems ? "-unq" : "");
    }

    static private final File TEST_OUTPUS =
        new File(System.getProperty("SpringBoatTemplateTests.output", "target/test-outputs"));

    @BeforeClass
    static public void setUpClass() throws IOException {
        TEST_OUTPUS.mkdirs();
        FileUtils.deleteDirectory(new File(TEST_OUTPUS, "src/main/java/backbase"));
    }

    private final String testName;
    private final boolean useBeanValidation;
    private final boolean useOptional;
    private final boolean useLombokAnnotations;
    private final boolean fullJavaUtil;
    private final boolean openApiNullable;
    private final boolean useSetForUniqueItems;

    @Test
    public void boat() {
        final List<File> files = generateFrom(null);

        assertThat(files.size(), not(equalTo(0)));
    }

    // @Test // used in development
    public void original() {
        generateFrom("openapi-generator-originals/JavaSpring-4.3.1");
    }

    private List<File> generateFrom(String templates) {
        final String target = caseName(
            this.useBeanValidation,
            this.useOptional,
            this.useLombokAnnotations,
            this.fullJavaUtil,
            this.openApiNullable,
            this.useSetForUniqueItems)
            + (templates != null ? "-orig" : "");

        final File input = new File("src/test/resources/boat-spring/openapi.yaml");
        final CodegenConfigurator cf = new CodegenConfigurator();

        cf.setGeneratorName(BoatSpringCodeGen.NAME);
        cf.setInputSpec(input.getAbsolutePath());
        cf.setOutputDir(TEST_OUTPUS.getAbsolutePath());

        GlobalSettings.setProperty(CodegenConstants.APIS, "");
        GlobalSettings.setProperty(CodegenConstants.API_DOCS, "true");
        GlobalSettings.setProperty(CodegenConstants.API_TESTS, "true");
        GlobalSettings.setProperty(CodegenConstants.MODELS, "");
        GlobalSettings.setProperty(CodegenConstants.MODEL_TESTS, "true");
        GlobalSettings.setProperty(CodegenConstants.MODEL_DOCS, "true");
        GlobalSettings.setProperty(CodegenConstants.SUPPORTING_FILES, "");

        cf.setApiNameSuffix("-api");
        cf.setModelNameSuffix(target);

        final String destPackage = target.replace('-', '.') + ".";

        cf.setApiPackage(destPackage + "api");
        cf.setModelPackage(destPackage + "model");
        cf.setInvokerPackage(destPackage + "invoker");

        cf.addAdditionalProperty(SpringCodegen.BASE_PACKAGE, destPackage + "base");
        cf.addAdditionalProperty(SpringCodegen.CONFIG_PACKAGE, destPackage + "config");

        cf.addAdditionalProperty(CodegenConstants.HIDE_GENERATION_TIMESTAMP, true);
        cf.addAdditionalProperty(SpringCodegen.INTERFACE_ONLY, true);
        cf.addAdditionalProperty(SpringCodegen.USE_TAGS, true);
        cf.addAdditionalProperty(SpringCodegen.SKIP_DEFAULT_INTERFACE, false);
        cf.addAdditionalProperty(BoatSpringCodeGen.ADD_SERVLET_REQUEST, true);
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
