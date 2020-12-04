package com.backbase.oss.boat;

import com.backbase.oss.boat.loader.OpenAPILoader;
import com.backbase.oss.boat.loader.OpenAPILoaderException;
import io.swagger.v3.oas.models.OpenAPI;
import java.io.File;
import java.util.*;

import lombok.extern.slf4j.Slf4j;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.assertj.core.util.Lists;
import org.codehaus.plexus.logging.console.ConsoleLogger;
import org.junit.Test;
import org.sonatype.plexus.build.incremental.DefaultBuildContext;

import static org.junit.Assert.fail;

@Slf4j
public class GeneratorTests {

    @Test
    public void testDereference() throws OpenAPILoaderException {
        String spec = System.getProperty("spec", getClass().getResource("/oas-examples/petstore.yaml").getFile());
        OpenAPI load = OpenAPILoader.load(new File(spec), true, false);
        System.out.println(load.toString());

    }

    @Test
    public void testHtml2() throws MojoExecutionException {

        String spec = System.getProperty("spec", getClass().getResource("/oas-examples/petstore.yaml").getFile());
        GenerateMojo mojo = new GenerateMojo();
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
        mojo.generatorName = "html2";
        mojo.inputSpec = input.getAbsolutePath();
        mojo.output = output;
        mojo.skip = false;
        mojo.skipIfSpecIsUnchanged = false;
        mojo.execute();
    }

    @Test
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
    }

    @Test
    public void testAngular() throws MojoExecutionException {

        String spec = System.getProperty("spec", getClass().getResource("/oas-examples/petstore.yaml").getFile());

        log.info("Generating client for: {}", spec);

        GenerateMojo mojo = new GenerateMojo();
        File input = new File(spec);
        File output = new File("target/boat-angular");
        if (!output.exists()) {
            output.mkdirs()
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

        try {
            mojo.execute();
        } catch (Exception e){
            fail("Generation should not throw exceptions");
        }

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
    }


    @Test
    public void testBeanValidation() throws MojoExecutionException {
        GenerateMojo mojo = new GenerateMojo();

        String inputFile = getClass().getResource("/oas-examples/petstore.yaml").getFile();
        File input = new File(inputFile);
        File output = new File("target");
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
        mojo.output = new File("target/spring-mvc");
        mojo.skip = false;
        mojo.skipIfSpecIsUnchanged = false;
        mojo.execute();

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

    }
}
