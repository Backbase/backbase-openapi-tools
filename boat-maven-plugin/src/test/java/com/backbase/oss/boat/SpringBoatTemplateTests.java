package com.backbase.oss.boat;

import static java.lang.String.format;
import static java.util.Arrays.asList;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assume.assumeThat;

import com.backbase.oss.codegen.SpringBoatCodeGen;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.apache.commons.io.FileUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.logging.console.ConsoleLogger;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.openapitools.codegen.CodegenConstants;
import org.openapitools.codegen.languages.AbstractJavaCodegen;
import org.openapitools.codegen.languages.SpringCodegen;
import org.openapitools.codegen.languages.features.BeanValidationFeatures;
import org.openapitools.codegen.languages.features.OptionalFeatures;
import org.sonatype.plexus.build.incremental.DefaultBuildContext;

/**
 * These tests verifies that the code generation works for various combinations of configuration
 * parameters; the projects that are generated are later compiled in the integration test phase.
 */
@RunWith(Parameterized.class)
@RequiredArgsConstructor
@Ignore
public class SpringBoatTemplateTests {

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

    static private final File TEST_OUTPUS = new File("target/test-outputs");

    @BeforeClass
    static public void setUpClass() throws IOException {
        FileUtils.deleteDirectory(TEST_OUTPUS);

        TEST_OUTPUS.mkdirs();
    }

    private final String testName;
    private final boolean useBeanValidation;
    private final boolean useOptional;
    private final boolean useLombokAnnotations;
    private final boolean fullJavaUtil;
    private final boolean openApiNullable;
    private final boolean useSetForUniqueItems;

    @Test
    public void boat() throws MojoExecutionException {
        createMojo(false).execute();
    }

    // @Test // used in development
    public void original() throws MojoExecutionException {
        assumeThat(this.useLombokAnnotations, is(false));
        assumeThat(this.useSetForUniqueItems, is(false));
        assumeThat(this.openApiNullable, is(true));

        createMojo(true).execute();
    }

    private GenerateMojo createMojo(boolean original) {
        final String target = caseName(
            this.useBeanValidation,
            this.useOptional,
            this.useLombokAnnotations,
            this.fullJavaUtil,
            this.openApiNullable,
            this.useSetForUniqueItems)
            + (original ? "-orig" : "");

        final File input = new File("src/test/resources/spring-boat/openapi.yaml");

        final GenerateMojo mojo = new GenerateMojo();
        final DefaultBuildContext defaultBuildContext = new DefaultBuildContext();
        defaultBuildContext.enableLogging(new ConsoleLogger());

        mojo.buildContext = defaultBuildContext;
        mojo.project = new MavenProject();
        mojo.generatorName = "spring";
        mojo.additionalProperties =
            asList("generatorVersion=" + System.getProperty("project.version", "0-SNAPSHOT"),
                "additionalDependencies=<dependency>\n"
                    + "        <groupId>jakarta.persistence</groupId>\n"
                    + "            <artifactId>jakarta.persistence-api</artifactId>\n"
                    + "            <version>2.2.3</version>\n"
                    + "        </dependency>");
        mojo.inputSpec = input.getAbsolutePath();
        mojo.output = TEST_OUTPUS.getAbsoluteFile();
        mojo.skip = false;
        mojo.skipIfSpecIsUnchanged = false;
        mojo.generateApis = true;
        mojo.generateApiDocumentation = true;
        mojo.generateApiTests = true;
        mojo.generateModels = true;
        mojo.generateModelDocumentation = true;
        mojo.generateModelTests = true;
        mojo.generateSupportingFiles = !original; // don't compile 4.3.1 code (wrong for JsonNullable with Map)
        mojo.apiNameSuffix = target + "-api";
        mojo.modelNameSuffix = target;

        final Map<String, String> configOptions = new HashMap<>();

        configOptions.put("library", SpringCodegen.SPRING_BOOT);
        configOptions.put("dateLibrary", SpringCodegen.JAVA_8);

        String destPackage = target.replace('-', '.') + ".";

        configOptions.put(SpringCodegen.BASE_PACKAGE, destPackage + "base");
        configOptions.put(SpringCodegen.CONFIG_PACKAGE, destPackage + "config");
        configOptions.put(CodegenConstants.API_PACKAGE, destPackage + "api");
        configOptions.put(CodegenConstants.MODEL_PACKAGE, destPackage + "model");

        configOptions.put(CodegenConstants.HIDE_GENERATION_TIMESTAMP, "true");
        configOptions.put(SpringCodegen.INTERFACE_ONLY, "true");
        configOptions.put(SpringCodegen.USE_TAGS, "true");
        configOptions.put(SpringCodegen.SKIP_DEFAULT_INTERFACE, "true");

        configOptions.put(BeanValidationFeatures.USE_BEANVALIDATION, Boolean.toString(this.useBeanValidation));
        configOptions.put(OptionalFeatures.USE_OPTIONAL, Boolean.toString(this.useOptional));
        configOptions.put(SpringBoatCodeGen.USE_LOMBOK_ANNOTATIONS, Boolean.toString(this.useLombokAnnotations));
        configOptions.put(AbstractJavaCodegen.FULL_JAVA_UTIL, Boolean.toString(this.fullJavaUtil));
        configOptions.put(SpringBoatCodeGen.USE_SET_FOR_UNIQUE_ITEMS, Boolean.toString(this.useSetForUniqueItems));
        configOptions.put(SpringBoatCodeGen.OPENAPI_NULLABLE, Boolean.toString(this.openApiNullable));

        if (original) {
            mojo.templateDirectory = new File("src/test/resources/JavaSpring-4.3.1");
        }

        mojo.configOptions = configOptions;

        return mojo;
    }
}


