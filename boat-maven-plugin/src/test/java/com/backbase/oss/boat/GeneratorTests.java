package com.backbase.oss.boat;

import lombok.extern.slf4j.Slf4j;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.logging.console.ConsoleLogger;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.sonatype.plexus.build.incremental.DefaultBuildContext;

import java.io.File;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

@Slf4j
class GeneratorTests {

    @BeforeAll
    static void setupLocale() {
        Locale.setDefault(Locale.ENGLISH);
        TimeZone.setDefault(TimeZone.getTimeZone("Europe/London"));
    }

    @Test
    void testHtml2() throws MojoExecutionException, MojoFailureException {

        String spec = System.getProperty("spec", getClass().getResource("/oas-examples/petstore.yaml").getFile());
        GenerateMojo mojo = new GenerateMojo();
        File input = new File(spec);
        File output = new File("target/boat-docs-generate");
        if (!output.exists()) {
            output.mkdirs();
        }

        DefaultBuildContext defaultBuildContext = new DefaultBuildContext();
        defaultBuildContext.enableLogging(new ConsoleLogger());

        mojo.getLog();
        mojo.buildContext = defaultBuildContext;
        mojo.project = new MavenProject();
        mojo.generatorName = "html2";
        mojo.inputSpec = input.getAbsolutePath();
        mojo.output = output;
        mojo.skip = false;
        mojo.skipIfSpecIsUnchanged = false;
        mojo.execute();

        assertThat(output.list()).containsExactlyInAnyOrder("index.html", ".openapi-generator-ignore", ".openapi-generator");
    }

    @Test
    void testBoatDocs() throws MojoExecutionException, MojoFailureException {

        String spec = System.getProperty("spec", getClass().getResource("/oas-examples/petstore.yaml").getFile());

        log.info("Generating docs for: {}", spec);

        GenerateDocMojo mojo = new GenerateDocMojo();
        File input = new File(spec);
        File output = new File("target/boat-docs");
        if (!output.exists()) {
            output.mkdirs();
        }

        DefaultBuildContext defaultBuildContext = new DefaultBuildContext();
        defaultBuildContext.enableLogging(new ConsoleLogger());

        mojo.getLog();
        mojo.buildContext = defaultBuildContext;
        mojo.project = new MavenProject();
        mojo.inputSpec = input.getAbsolutePath();
        mojo.output = output;
        mojo.skip = false;
        mojo.skipIfSpecIsUnchanged = false;
        mojo.bundleSpecs = true;
        mojo.dereferenceComponents = true;
        mojo.markersDirectory = new File("target/boat-markers");
        mojo.execute();

        assertThat(output.list()).containsExactlyInAnyOrder("index.html", ".openapi-generator-ignore", ".openapi-generator");
    }

    @Test
    void testBoatDocsWithDirectory() throws MojoExecutionException, MojoFailureException {

        String spec = System.getProperty("spec", getClass().getResource("/boat-doc-oas-examples").getFile());

        log.info("Generating docs for: {}", spec);

        GenerateDocMojo mojo = new GenerateDocMojo();
        File input = new File(spec);
        File output = new File("target/boat-docs-directory");
        if (!output.exists()) {
            output.mkdirs();
        }

        DefaultBuildContext defaultBuildContext = new DefaultBuildContext();
        defaultBuildContext.enableLogging(new ConsoleLogger());

        mojo.getLog();
        mojo.buildContext = defaultBuildContext;
        mojo.project = new MavenProject();
        mojo.inputSpec = input.getAbsolutePath();
        mojo.output = output;
        mojo.skip = false;
        mojo.skipIfSpecIsUnchanged = false;
        mojo.bundleSpecs = true;
        mojo.dereferenceComponents = true;
        mojo.openApiFileFilters = "**/*.yaml";
        mojo.markersDirectory = new File("target/boat-markers");
        mojo.execute();


        assertThat(output.list()).contains("link", "petstore", "petstore-new-non-breaking", "upto");
    }

    @Test
    void testBoatDocsWithDirectoryAndInvalidFiles() throws MojoExecutionException, MojoFailureException {

        String spec = System.getProperty("spec", getClass().getResource("/oas-examples").getFile());

        log.info("Generating docs for: {}", spec);

        GenerateDocMojo mojo = new GenerateDocMojo();
        File input = new File(spec);
        File output = new File("target/boat-docs-directory");
        if (!output.exists()) {
            output.mkdirs();
        }

        DefaultBuildContext defaultBuildContext = new DefaultBuildContext();
        defaultBuildContext.enableLogging(new ConsoleLogger());

        mojo.getLog();
        mojo.buildContext = defaultBuildContext;
        mojo.project = new MavenProject();
        mojo.inputSpec = input.getAbsolutePath();
        mojo.output = output;
        mojo.skip = false;
        mojo.skipIfSpecIsUnchanged = false;
        mojo.bundleSpecs = true;
        mojo.dereferenceComponents = true;
        mojo.openApiFileFilters = "**/*.yaml";
        mojo.markersDirectory = new File("target/boat-markers");
        mojo.execute();


        assertThat(output.list()).contains("link", "petstore", "petstore-new-non-breaking", "upto");
    }

    @Test
    void testBoatDocsWithNonExistingMarkersDirectory() {

        assertThatExceptionOfType(MojoExecutionException.class).isThrownBy(() -> {
            String spec = System.getProperty("spec", getClass().getResource("/boat-doc-oas-examples").getFile());

            log.info("Generating docs for: {}", spec);

            GenerateDocMojo mojo = new GenerateDocMojo();
            File input = new File(spec);
            File output = new File("target/boat-docs-directory");
            if (!output.exists()) {
                output.mkdirs();
            }

            DefaultBuildContext defaultBuildContext = new DefaultBuildContext();
            defaultBuildContext.enableLogging(new ConsoleLogger());

            mojo.getLog();
            mojo.buildContext = defaultBuildContext;
            mojo.project = new MavenProject();
            mojo.inputSpec = input.getAbsolutePath();
            mojo.output = output;
            mojo.skip = false;
            mojo.skipIfSpecIsUnchanged = false;
            mojo.bundleSpecs = true;
            mojo.dereferenceComponents = true;
            mojo.openApiFileFilters = "**/*.yaml";
            mojo.markersDirectory = new File(" //43243 \\d a1r1\4t 11t134 t835jyz");
            mojo.execute();
        });
    }

    @Test
    void testBundledBoatDocs() throws MojoExecutionException, MojoFailureException {

        String spec = System.getProperty("spec", getClass().getResource("/oas-examples/petstore.yaml").getFile());

        log.info("Generating docs for: {}", spec);


        BundleMojo bundleMojo = new BundleMojo();
        bundleMojo.setInput(new File(spec));
        File dereferenced = new File("target/boat-docs-bundled/dereferenced-openapi.yml");
        bundleMojo.setOutput(dereferenced);
        bundleMojo.execute();


        GenerateDocMojo mojo = new GenerateDocMojo();
        File output = new File("target/boat-docs-bundled/");
        if (!output.exists()) {
            output.mkdirs();
        }

        DefaultBuildContext defaultBuildContext = new DefaultBuildContext();
        defaultBuildContext.enableLogging(new ConsoleLogger());

        mojo.getLog();
        mojo.buildContext = defaultBuildContext;
        mojo.project = new MavenProject();
        mojo.inputSpec = dereferenced.getAbsolutePath();
        mojo.output = output;
        mojo.skip = false;
        mojo.skipIfSpecIsUnchanged = false;
        mojo.bundleSpecs = false;
        mojo.dereferenceComponents = false;
        mojo.execute();
        String[] actualGeneratedFiles = output.list();
        Arrays.sort(actualGeneratedFiles);
        String[] expectedFiles = {".openapi-generator", ".openapi-generator-ignore", "dereferenced-openapi.yml", "index.html"};
        assertArrayEquals(expectedFiles, actualGeneratedFiles);
    }

    @Test
    void testAngular() {

        String spec = System.getProperty("spec", getClass().getResource("/oas-examples/petstore.yaml").getFile());

        log.info("Generating client for: {}", spec);

        GenerateMojo mojo = new GenerateMojo();
        File input = new File(spec);
        File output = new File("target/boat-angular");
        if (!output.exists()) {
            output.mkdirs();
        }

        DefaultBuildContext defaultBuildContext = new DefaultBuildContext();
        defaultBuildContext.enableLogging(new ConsoleLogger());

        mojo.getLog();
        mojo.buildContext = defaultBuildContext;
        mojo.project = new MavenProject();
        mojo.inputSpec = input.getAbsolutePath();
        mojo.output = output;
        mojo.skip = false;
        mojo.skipIfSpecIsUnchanged = false;
        mojo.bundleSpecs = true;
        mojo.dereferenceComponents = true;
        mojo.generatorName = "boat-angular";
        mojo.enablePostProcessFile = true;

        if (Objects.isNull(mojo.additionalProperties)) {
            mojo.additionalProperties = new LinkedList<>();
        }
        mojo.additionalProperties.add("withMocks=true");
        mojo.additionalProperties.add("apiModulePrefix=PetStore");
        mojo.additionalProperties.add("npmName=@petstore/http");
        mojo.additionalProperties.add("npmRepository=https://repo.example.com");

        assertDoesNotThrow(mojo::execute, "Angular client generation should not throw exceptions");
    }

    @Test
    void testAngularExamplesInComponents() {

        String spec = System.getProperty("spec", getClass().getResource("/oas-examples/pet-store-example-in-components.yaml").getFile());

        log.info("Generating client for: {}", spec);

        GenerateMojo mojo = new GenerateMojo();
        File input = new File(spec);
        File output = new File("target/boat-angular-examples-in-components");
        if (!output.exists()) {
            output.mkdirs();
        }

        DefaultBuildContext defaultBuildContext = new DefaultBuildContext();
        defaultBuildContext.enableLogging(new ConsoleLogger());

        mojo.getLog();
        mojo.buildContext = defaultBuildContext;
        mojo.project = new MavenProject();
        mojo.inputSpec = input.getAbsolutePath();
        mojo.output = output;
        mojo.skip = false;
        mojo.skipIfSpecIsUnchanged = false;
        mojo.bundleSpecs = true;
        mojo.dereferenceComponents = true;
        mojo.generatorName = "boat-angular";
        mojo.enablePostProcessFile = true;

        if (Objects.isNull(mojo.additionalProperties)) {
            mojo.additionalProperties = new LinkedList<>();
        }
        mojo.additionalProperties.add("withMocks=true");
        mojo.additionalProperties.add("npmName=@petstore/http");
        mojo.additionalProperties.add("npmRepository=https://repo.example.com");

        assertDoesNotThrow(mojo::execute, "Angular client generation should not throw exceptions");
    }
}
