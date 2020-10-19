package com.backbase.oss.boat;

import static java.lang.String.format;
import static java.util.Arrays.asList;

import com.backbase.oss.codegen.SpringCodeGen;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.apache.commons.io.FileUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.logging.console.ConsoleLogger;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.openapitools.codegen.CodegenConstants;
import org.openapitools.codegen.languages.AbstractJavaCodegen;
import org.openapitools.codegen.languages.SpringCodegen;
import org.openapitools.codegen.languages.features.BeanValidationFeatures;
import org.sonatype.plexus.build.incremental.DefaultBuildContext;

/**
 * These tests verifies that the code generation works for various combinations of configuration
 * parameters; the projects that are generated are later compiled in the integration test phase.
 */
@RunWith(Parameterized.class)
@RequiredArgsConstructor
public class SpringTemplateTests {

    @Parameterized.Parameters(name = "{0}")
    static public Object parameters() {
        final List<Object[]> data = new ArrayList<>();

        for (final String resource : asList("simple", "array", "set", "map")) {
            // all combinations of useBeanValidation, useOptional, useLombokAnnotations
            for (int mask = 0; mask < (1 << 3); mask++) {
                boolean useBeanValidation = (mask & 1) != 0;
                boolean useOptional = (mask & 2) != 0;
                boolean useLombokAnnotations = (mask & 4) != 0;

                data.add(new Object[] {
                    caseName(resource, useBeanValidation, useOptional, useLombokAnnotations, false),
                    resource, useBeanValidation, useOptional, useLombokAnnotations,});
            }
        }

        return data;
    }

    static private String caseName(String resource, boolean useBeanValidation, boolean useOptional,
        boolean useLombokAnnotations, boolean original) {
        return format("%s%s%s%s%s", resource,
            useBeanValidation ? "-validation" : "",
            useOptional ? "-optional" : "",
            useLombokAnnotations ? "-lombok" : "",
            original ? "-orig" : "");
    }

    private final String testName;
    private final String resource;
    private final boolean useBeanValidation;
    private final boolean useOptional;
    private final boolean useLombokAnnotations;

    @Test
    public void run() throws MojoExecutionException {
        createMojo(false).execute();

        if (!this.useLombokAnnotations) {
            createMojo(true).execute();
        }
    }

    @SneakyThrows
    private GenerateMojo createMojo(boolean original) {
        final File input = new File(format("src/test/resources/backbase/%s-types.yaml", this.resource));
        final File output =
            new File("target/test-outputs/"
                + caseName(resource, useBeanValidation, useOptional, useLombokAnnotations, original));

        FileUtils.deleteDirectory(output);
        output.mkdirs();

        final GenerateMojo mojo = new GenerateMojo();
        final DefaultBuildContext defaultBuildContext = new DefaultBuildContext();
        defaultBuildContext.enableLogging(new ConsoleLogger());

        mojo.buildContext = defaultBuildContext;
        mojo.project = new MavenProject();
        mojo.generatorName = "spring";
        mojo.additionalProperties =
            asList("generatorVersion=" + System.getProperty("project.version", "0.2.7-SNAPSHOT"),
                "additionalDependencies=<dependency>\n"
                    + "        <groupId>jakarta.persistence</groupId>\n"
                    + "            <artifactId>jakarta.persistence-api</artifactId>\n"
                    + "            <version>2.2.3</version>\n"
                    + "        </dependency>"
                    + "        <dependency>\n"
                    + "            <groupId>com.backbase.buildingblocks</groupId>\n"
                    + "            <artifactId>building-blocks-common</artifactId>\n"
                    + "            <version>11.2.1</version>\n"
                    + "        </dependency>");
        mojo.inputSpec = input.getAbsolutePath();
        mojo.output = output.getAbsoluteFile();
        mojo.skip = false;
        mojo.skipIfSpecIsUnchanged = false;
        mojo.generateApis = true;
        mojo.generateApiDocumentation = true;
        mojo.generateApiTests = false;
        mojo.generateModels = true;
        mojo.generateModelDocumentation = true;
        mojo.generateModelTests = false;
        mojo.generateSupportingFiles = !original; // 4.3.1 code is wrong for JsonNullable with Map
        mojo.apiNameSuffix =
            caseName(resource, useBeanValidation, useOptional, useLombokAnnotations, original) + "-api";
        mojo.modelNamePrefix = "model-";
        mojo.modelNameSuffix = caseName(resource, useBeanValidation, useOptional, useLombokAnnotations, original);
        final Map<String, String> configOptions = new HashMap<>();

        configOptions.put("library", SpringCodegen.SPRING_BOOT);
        configOptions.put("dateLibrary", SpringCodegen.JAVA_8);
        configOptions.put(SpringCodegen.BASE_PACKAGE, "base");
        configOptions.put(SpringCodegen.CONFIG_PACKAGE, "config");
        configOptions.put(CodegenConstants.API_PACKAGE, "api");
        configOptions.put(CodegenConstants.API_SUFFIX,
            caseName(resource, useBeanValidation, useOptional, useLombokAnnotations, original));
        configOptions.put(CodegenConstants.API_NAME_SUFFIX,
            caseName(resource, useBeanValidation, useOptional, useLombokAnnotations, original));
        configOptions.put(CodegenConstants.MODEL_PACKAGE, "model");
        configOptions.put(CodegenConstants.HIDE_GENERATION_TIMESTAMP, "true");
        configOptions.put(BeanValidationFeatures.USE_BEANVALIDATION, Boolean.toString(this.useBeanValidation));
        configOptions.put(SpringCodeGen.USE_OPTIONAL, Boolean.toString(this.useOptional));
        configOptions.put(SpringCodeGen.USE_LOMBOK_ANNOTATIONS, Boolean.toString(this.useLombokAnnotations));
        configOptions.put(SpringCodegen.INTERFACE_ONLY, "true");
        configOptions.put(SpringCodegen.USE_TAGS, "true");
        configOptions.put(SpringCodegen.SKIP_DEFAULT_INTERFACE, "true");

        // 4.3.1 uses JsonNullable unconditionally
        configOptions.put(SpringCodeGen.OPENAPI_NULLABLE, "true");

        if (original) {
            // 4.3.1 doesn't know Set
            configOptions.put(AbstractJavaCodegen.FULL_JAVA_UTIL, "true");
            mojo.templateDirectory = new File("src/test/resources/JavaSpring-4.3.1");
        }

        mojo.configOptions = configOptions;

        return mojo;
    }
}


