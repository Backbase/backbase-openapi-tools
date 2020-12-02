package com.backbase.oss.boat;

import java.io.File;

import com.backbase.oss.boat.loader.OpenAPILoader;
import com.backbase.oss.boat.loader.OpenAPILoaderException;
import io.swagger.v3.oas.models.OpenAPI;
import lombok.extern.slf4j.Slf4j;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.checkerframework.checker.units.qual.A;
import org.codehaus.plexus.logging.console.ConsoleLogger;
import org.junit.Assert;
import org.junit.Test;
import org.sonatype.plexus.build.incremental.DefaultBuildContext;

@Slf4j
public class ExportMojoTests {

    @Test
    public void testInputFile() throws MojoExecutionException {

        ExportMojo mojo = new ExportMojo();

        File input = getFile("/raml-examples/helloworld/helloworld.raml");
        File output = new File("target");
        if (!output.exists()) {
            output.mkdirs();
        }

        File outputYaml = new File(output, "helloworld/openapi.yaml");
        outputYaml.delete();

        DefaultBuildContext defaultBuildContext = new DefaultBuildContext();
        defaultBuildContext.enableLogging(new ConsoleLogger(2, "BOAT"));

        mojo.getLog();
        mojo.project = new MavenProject();
        mojo.inputFile = input;
        mojo.output = output;
        mojo.execute();

        Assert.assertTrue(outputYaml.exists());

    }

    @Test
    public void testLicenceAdder() throws MojoExecutionException, OpenAPILoaderException {

        ExportMojo mojo = new ExportMojo();

        File input = getFile("/raml-examples/helloworld/helloworld.raml");
        File output = new File("target");
        if (!output.exists()) {
            output.mkdirs();
        }

        File outputYaml = new File(output, "helloworld/openapi.yaml");
        outputYaml.delete();

        DefaultBuildContext defaultBuildContext = new DefaultBuildContext();
        defaultBuildContext.enableLogging(new ConsoleLogger(2, "BOAT"));

        mojo.getLog();
        mojo.project = new MavenProject();
        mojo.inputFile = input;
        mojo.output = output;
        mojo.licenseName = "testLicence";
        mojo.licenseUrl= "testUrl";
        mojo.execute();

        Assert.assertTrue(outputYaml.exists());
        OpenAPI load = OpenAPILoader.load(outputYaml, false, false);
        Assert.assertEquals("testLicence",load.getInfo().getLicense().getName() );
        Assert.assertEquals("testUrl",load.getInfo().getLicense().getUrl());
    }

    @Test
    public void testInputDir() throws MojoExecutionException {
        ExportMojo mojo = new ExportMojo();

        File input = getFile("/raml-examples/backbase-wallet");
        File output = new File("target");
        if (!output.exists()) {
            output.mkdirs();
        }

        DefaultBuildContext defaultBuildContext = new DefaultBuildContext();
        defaultBuildContext.enableLogging(new ConsoleLogger(2, "BOAT"));

        mojo.getLog();
        mojo.project = new MavenProject();
        mojo.input = input;
        mojo.output = output;
        mojo.execute();

        Assert.assertTrue(new File("target/presentation-client-api/openapi.yaml").exists());
        Assert.assertTrue(new File("target/presentation-integration-api/openapi.yaml").exists());
        Assert.assertTrue(new File("target/presentation-service-api/openapi.yaml").exists());

    }

    private File getFile(String fileName) {
        return new File(getClass().getResource(fileName).getFile());
    }
}
