package com.backbase.oss.boat;

import com.backbase.oss.boat.loader.OpenAPILoaderException;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class RemoveMojoTests {
    @Test
    public void testRemoveDeprecatedMojo() throws MojoFailureException, MojoExecutionException, IOException, OpenAPILoaderException {

        File output = getFile("/readWriteFiles/output.yaml");
        File input = getFile("/oas-examples/petstore-deprecated.yaml");
        RemoveDeprecatedMojo removeDeprecatedMojo = new RemoveDeprecatedMojo();
        removeDeprecatedMojo.setInput(input);
        removeDeprecatedMojo.setOutput(output);
        removeDeprecatedMojo.execute();
        Paths.get(output.getPath());
        String depreciated = String.join( " ", Files.readAllLines(Paths.get(output.getPath())));

        Assert.assertTrue(!depreciated.contains("/pets/{petId}: get:"));

    }

    @Test
    public void testDecomposeMojo() throws MojoFailureException, MojoExecutionException, IOException {
        File output = getFile("/readWriteFiles/output.yaml");
        File input = getFile("/oas-examples/petstore-composed.yaml");
        DecomposeMojo decomposeMojo = new DecomposeMojo();
        decomposeMojo.setInput(input);
        decomposeMojo.setOutput(output);
        // Assert.assertEquals(0, output.length());
        decomposeMojo.execute();
        // Assert.assertTrue(output.length()>0);

    }
    private File getFile(String fileName) {
        return new File(getClass().getResource(fileName).getFile());
    }
}
