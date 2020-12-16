package com.backbase.oss.boat;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import lombok.extern.slf4j.Slf4j;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Slf4j
public class RemoveMojoTests {

    @Test
    void testRemoveDeprecatedMojo() throws MojoFailureException, MojoExecutionException, IOException {

        File output = getFile("/readWriteFiles/output.yaml");
        File input = getFile("/bundler/petstore-deprecated.yaml");
        RemoveDeprecatedMojo removeDeprecatedMojo = new RemoveDeprecatedMojo();
        removeDeprecatedMojo.setInput(input);
        removeDeprecatedMojo.setOutput(output);
        removeDeprecatedMojo.execute();
        Paths.get(output.getPath());
        String depreciated = String.join( " ", Files.readAllLines(Paths.get(output.getPath())));

        assertFalse(depreciated.contains("/pets/{petId}: get:"));

    }

    @Test
    void testDecomposeMojo() throws MojoFailureException, MojoExecutionException, IOException {
        File output = getFile("/readWriteFiles/output.yaml");
        File input = getFile("/oas-examples/petstore-composed.yaml");
        DecomposeMojo decomposeMojo = new DecomposeMojo();
        decomposeMojo.setInput(input);
        decomposeMojo.setOutput(output);
        decomposeMojo.execute();
        String decomposed = String.join( " ", Files.readAllLines(Paths.get(output.getPath())));
        assertFalse(decomposed.contains("allOf:"));

    }
    private File getFile(String fileName) {
        return new File(getClass().getResource(fileName).getFile());
    }
}
