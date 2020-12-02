package com.backbase.oss.boat;

import javatools.administrative.D;
import org.apache.maven.plugin.Mojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.logging.console.ConsoleLogger;
import org.junit.Assert;
import org.junit.Test;
import org.sonatype.plexus.build.incremental.DefaultBuildContext;

import java.io.File;
import java.io.IOException;

public class ValidateMojoTests {

    @Test
    public void testValidation() throws MojoFailureException {

        String spec = System.getProperty("spec", getClass().getResource("/oas-examples/petstore.yaml").getFile());

        File input = new File(spec);

        ValidateMojo mojo = new ValidateMojo();
        mojo.setInput(input);
        mojo.setFailOnWarning(true);

        mojo.execute();

    }

    @Test
    public void testValidationCatches() throws MojoFailureException, MojoExecutionException{


        ValidateMojo mojo = new ValidateMojo();
        mojo.setInput(new File("bad.yaml"));
        mojo.setFailOnWarning(true);

        Assert.assertThrows(MojoFailureException.class, ()-> mojo.execute());


    }

    @Test
    public void testRemoveDeprecatedMojo() throws MojoFailureException, MojoExecutionException, IOException {

        File output = new File("src/test/resources/ReadWriteFiles/output.yaml");
        File input = new File("src/test/resources/oas-examples/petstore.yaml");
        RemoveDeprecatedMojo removeDeprecatedMojo = new RemoveDeprecatedMojo();
        removeDeprecatedMojo.setInput(input);
        removeDeprecatedMojo.setOutput(output);
        Assert.assertEquals(0, output.length());
        removeDeprecatedMojo.execute();
        Assert.assertTrue(output.length()>0);
        output.delete();
        output.createNewFile();
    }

    @Test
    public void testDecomposeMojo() throws MojoFailureException, MojoExecutionException, IOException {
        File output = new File("src/test/resources/ReadWriteFiles/output.yaml");
        File input = new File("src/test/resources/oas-examples/petstore.yaml");
        DecomposeMojo decomposeMojo = new DecomposeMojo();
        decomposeMojo.setInput(input);
        decomposeMojo.setOutput(output);
        Assert.assertEquals(0, output.length());
        decomposeMojo.execute();
        Assert.assertTrue(output.length()>0);
        output.createNewFile();
        output.delete();
    }
}

