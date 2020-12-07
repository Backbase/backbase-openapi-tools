package com.backbase.oss.boat;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import java.io.File;
import lombok.SneakyThrows;
import org.apache.maven.plugin.MojoExecutionException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

public class BundleMojoTest {

    @Test
    @SneakyThrows
    public void testInputDirectoryAndOutputFile() {
        BundleMojo mojo = new BundleMojo();
        mojo.setInput(new File("."));
        mojo.setOutput(new File("target/testInputDirectoryAndOutputFile.yaml"));

        assertThrows(MojoExecutionException.class, mojo::execute);
    }

    @Test
    @SneakyThrows
    public void testSkip() {
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
    public void testBundleFolder() {
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
    @SneakyThrows
    public void testVersionFileName() {
        BundleMojo mojo = new BundleMojo();

        assertEquals(
            "payment-order-client-api-v2.0.0.yaml",
            mojo.versionFileName("payment-order-client-api-v2.yaml", createOpenApiWithVersion("2.0.0")));
    }

    @Test
    @SneakyThrows
    public void testNoInfoInApi() {
        BundleMojo mojo = new BundleMojo();

        assertThrows(MojoExecutionException.class, () ->
            mojo.versionFileName("payment-order-client-api-v2.yaml", new OpenAPI()));
    }

    @Test
    @SneakyThrows
    public void testNoVersionInfoInApi() {
        BundleMojo mojo = new BundleMojo();

        assertThrows(MojoExecutionException.class, () ->
            mojo.versionFileName("payment-order-client-api-v2.yaml", createOpenApiWithVersion(null)));
    }

    @Test
    @SneakyThrows
    public void testInvalidVersionInApi() {
        BundleMojo mojo = new BundleMojo();

        OpenAPI openAPI = new OpenAPI();
        openAPI.setInfo(new Info());
        assertThrows(MojoExecutionException.class, () ->
            mojo.versionFileName("payment-order-client-api-v2.yaml", createOpenApiWithVersion("3.0.0")));
    }


    private OpenAPI createOpenApiWithVersion(String version) {
        OpenAPI openAPI = new OpenAPI();
        openAPI.setInfo(new Info());
        openAPI.getInfo().setVersion(version);
        return openAPI;
    }


}
