package com.backbase.oss.boat;

import java.io.File;
import lombok.extern.slf4j.Slf4j;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
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
        defaultBuildContext.enableLogging(new ConsoleLogger());

        mojo.getLog();
        mojo.project = new MavenProject();
        mojo.inputFile = input;
        mojo.output = output;
        mojo.execute();

        Assert.assertTrue(outputYaml.exists());

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
        defaultBuildContext.enableLogging(new ConsoleLogger());

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
