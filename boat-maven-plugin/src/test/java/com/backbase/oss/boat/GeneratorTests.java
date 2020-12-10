package com.backbase.oss.boat;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.shared.invoker.DefaultInvocationRequest;
import org.apache.maven.shared.invoker.DefaultInvoker;
import org.apache.maven.shared.invoker.InvocationRequest;
import org.apache.maven.shared.invoker.InvocationResult;
import org.apache.maven.shared.invoker.Invoker;
import org.apache.maven.shared.invoker.MavenInvocationException;
import org.codehaus.plexus.logging.console.ConsoleLogger;
import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.sonatype.plexus.build.incremental.DefaultBuildContext;

@Slf4j
public class GeneratorTests {

    // @Test
    public void testHtml2() throws MojoExecutionException {

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
        String[] expectedGeneratedDocs = {"index.html",
                ".openapi-generator-ignore",".openapi-generator"};
        assertArrayEquals(expectedGeneratedDocs,output.list());

    }

    // @Test
    public void testBoatDocs() throws MojoExecutionException {

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
        mojo.execute();

        String[] expectedGeneratedDocs = {"index.html",
                ".openapi-generator-ignore",".openapi-generator"};
        assertArrayEquals(expectedGeneratedDocs,output.list());
    }

    @Test
    public void testBundledBoatDocs() throws MojoExecutionException, MojoFailureException {

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
        String[] expectedFiles= {".openapi-generator",".openapi-generator-ignore","dereferenced-openapi.yml","index.html"};
        assertArrayEquals(expectedFiles,actualGeneratedFiles);
    }

    @Test
    public void testAngular() {

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
        mojo.generatorName= "boat-angular";
        mojo.enablePostProcessFile= true;

        if(Objects.isNull(mojo.additionalProperties)){
            mojo.additionalProperties = new LinkedList<>();
        }
        mojo.additionalProperties.add("withMocks=true");
        mojo.additionalProperties.add("npmName=@petstore/http");
        mojo.additionalProperties.add("npmRepository=https://repo.example.com");

        assertDoesNotThrow(mojo::execute, "Angular client generation should not throw exceptions");
    }

    @Test
    public void testBeanValidation() throws MojoExecutionException {
        GenerateMojo mojo = new GenerateMojo();

        String inputFile = getClass().getResource("/oas-examples/petstore.yaml").getFile();
        File input = new File(inputFile);
        File output = new File("target/spring-mvc");
        if (!output.exists()) {
            output.mkdirs();
        }

        DefaultBuildContext defaultBuildContext = new DefaultBuildContext();
        defaultBuildContext.enableLogging(new ConsoleLogger());

        Map<String,String> configOption = new HashMap<>();
        configOption.put("library", "spring-mvc");
        configOption.put("dateLibrary", "java8");
        configOption.put("apiPackage", "com.backbase.accesscontrol.service.rest.spec.api");
        configOption.put("modelPackage", "com.backbase.accesscontrol.service.rest.spec.model");
        configOption.put("useBeanValidation", "true");
        configOption.put("useClassLevelBeanValidation", "false");


        mojo.getLog();
        mojo.buildContext = defaultBuildContext;
        mojo.project = new MavenProject();
        mojo.generatorName = "spring";
        mojo.configOptions = configOption;
        mojo.inputSpec = input.getAbsolutePath();
        mojo.output = output;
        mojo.skip = false;
        mojo.skipIfSpecIsUnchanged = false;
        mojo.execute();
        String[] actualFilesGenerated = output.list();
        Arrays.sort(actualFilesGenerated);
        String[] expected = {".openapi-generator",".openapi-generator-ignore","README.md","pom.xml","src"};
        assertArrayEquals(expected,actualFilesGenerated);

    }

    @Test
    public void testWebClient() throws MojoExecutionException {
        GenerateWebClientEmbeddedMojo mojo = new GenerateWebClientEmbeddedMojo();

        String inputFile = getClass().getResource("/oas-examples/petstore.yaml").getFile();
        File input = new File(inputFile);
        File output = new File("target/webclient");
        if (!output.exists()) {
            output.mkdirs();
        }

        DefaultBuildContext defaultBuildContext = new DefaultBuildContext();
        defaultBuildContext.enableLogging(new ConsoleLogger());

        mojo.buildContext = defaultBuildContext;
        mojo.project = new MavenProject();
        mojo.inputSpec = input.getAbsolutePath();
        mojo.output = output;
        mojo.skip = false;
        mojo.skipIfSpecIsUnchanged = false;
        mojo.execute();

        String[] actualFilesGenerated = output.list();
        Arrays.sort(actualFilesGenerated);
        String[] expected = {".openapi-generator",".openapi-generator-ignore","api","gradle","src"};
        assertArrayEquals(expected,actualFilesGenerated);

    }

    @Test
    @Disabled
    public void testJavaClient() throws MojoExecutionException, MavenInvocationException {
        GenerateMojo mojo = new GenerateMojo();

        String spec = System.getProperty("spec", getClass().getResource("/oas-examples/petstore.yaml").getFile());

        File input = new File(spec);
        File output = new File("target/javaclient");
        if (output.exists()) {
            output.delete();
        }
        output.mkdirs();

        DefaultBuildContext defaultBuildContext = new DefaultBuildContext();
        defaultBuildContext.enableLogging(new ConsoleLogger());
        mojo.generatorName = "java";
        mojo.library = "native";
        mojo.buildContext = defaultBuildContext;
        mojo.project = new MavenProject();
        mojo.inputSpec = input.getAbsolutePath();
        mojo.output = output;
        mojo.skip = false;
        mojo.skipIfSpecIsUnchanged = false;
        mojo.skipOverwrite  = false;
        mojo.generateAliasAsModel = false;
        mojo.execute();

        InvocationRequest invocationRequest = new DefaultInvocationRequest();
        invocationRequest.setPomFile(new File(output, "pom.xml"));
        invocationRequest.setGoals(Arrays.asList("compile"));
        invocationRequest.setBatchMode(true);

        Invoker invoker = new DefaultInvoker();
        InvocationResult invocationResult = invoker.execute(invocationRequest);
        assertNull(invocationResult.getExecutionException());

    }

}
