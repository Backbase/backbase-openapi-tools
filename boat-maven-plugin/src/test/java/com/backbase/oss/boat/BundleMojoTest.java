package com.backbase.oss.boat;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;

import lombok.SneakyThrows;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

class BundleMojoTest {

    @Test
    @SneakyThrows
    void testInputDirectoryAndOutputFile() {
        BundleMojo mojo = new BundleMojo();
        mojo.setInput(new File("."));
        mojo.setOutput(new File("target/testInputDirectoryAndOutputFile.yaml"));

        assertThrows(MojoExecutionException.class, mojo::execute);
    }

    @Test
    @SneakyThrows
    void testSkip() {
        BundleMojo mojo = new BundleMojo();
        mojo.setSkip(true);
        mojo.setInput(new File("target/testInputDirectoryAndOutputFile.yaml"));

        try {
            mojo.execute();
        } catch (MojoExecutionException e) {
            fail("Expecting skip execution but fails on input file not found.");
        }
    }

    @Test
    @SneakyThrows
    void testBundleFolder() {
        BundleMojo mojo = new BundleMojo();

        mojo.setInput(new File(getClass().getResource("/bundler/folder/one-client-api-v1.yaml").getFile())
            .getParentFile());
        mojo.setOutput(new File("target/test-bundle-folder"));
        mojo.setIncludes("*-api-v*.yaml");
        mojo.setVersionFileName(true);
        mojo.execute();

        assertTrue(new File("target/test-bundle-folder/one-client-api-v1.3.5.yaml").exists());
        assertTrue(new File("target/test-bundle-folder/one-client-api-v2.0.0.yaml").exists());
        assertTrue(new File("target/test-bundle-folder/another-client-api-v1.7.9.yaml").exists());
    }

    @Test
    void testSetVersionAttribute() throws MojoFailureException, MojoExecutionException, IOException {
        File output = getFile("/readWriteFiles/output.yaml");
        BundleMojo mojo = new BundleMojo();
        mojo.setVersion("3.0.0");
        mojo.setInput(getFile("/bundler/folder/another-client-api-v1.yaml"));
        mojo.setOutput(output);
        mojo.execute();

        String outputApi = String.join( " ", Files.readAllLines(Paths.get(output.getPath())));
        assertTrue(outputApi.contains("version: 3.0.0"));
    }


    private OpenAPI createOpenApiWithVersion(String version) {
        OpenAPI openAPI = new OpenAPI();
        openAPI.setInfo(new Info());
        openAPI.getInfo().setVersion(version);
        return openAPI;
    }
    protected File getFile(String name) {
        URL resource = getClass().getResource(name);
        assert resource != null;
        return new File(resource.getFile());
    }


}
