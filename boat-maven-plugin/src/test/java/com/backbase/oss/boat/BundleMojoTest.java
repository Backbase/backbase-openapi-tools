package com.backbase.oss.boat;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import java.io.File;
import lombok.SneakyThrows;
import org.apache.maven.plugin.MojoExecutionException;
import org.junit.Assert;
import org.junit.Test;

public class BundleMojoTest {

    @Test
    @SneakyThrows
    public void testInputDirectoryAndOutputFile() {
        BundleMojo mojo = new BundleMojo();
        mojo.setInput(new File("."));
        mojo.setOutput(new File("target/testInputDirectoryAndOutputFile.yaml"));

        Assert.assertThrows(MojoExecutionException.class, () -> mojo.execute());
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
            Assert.fail("Expecting skip execution but fails on input file not found.");
        }
    }

    @Test
    @SneakyThrows
    public void testBundleFolder() {
        BundleMojo mojo = new BundleMojo();

        mojo.setInput(new File(getClass().getResource("/bundler/folder/one-client-api-v1.yaml").getFile())
            .getParentFile());
        mojo.setOutput(new File("target/test-bundle-folder"));

        mojo.execute();

        Assert.assertTrue(new File("target/test-bundle-folder/one-client-api-v1.3.5.yaml").exists());
        Assert.assertTrue(new File("target/test-bundle-folder/one-client-api-v2.0.0.yaml").exists());
        Assert.assertTrue(new File("target/test-bundle-folder/another-client-api-v1.7.9.yaml").exists());
    }

    @Test
    @SneakyThrows
    public void testVersionFileName() {
        BundleMojo mojo = new BundleMojo();

        Assert.assertEquals(
            "payment-order-client-api-v2.0.0.yaml",
            mojo.versionFileName("payment-order-client-api-v2.yaml", createOpenApiWithVersion("2.0.0")));
    }

    @Test
    @SneakyThrows
    public void testNoInfoInApi() {
        BundleMojo mojo = new BundleMojo();

        Assert.assertThrows(MojoExecutionException.class, () ->
            mojo.versionFileName("payment-order-client-api-v2.yaml", new OpenAPI()));
    }

    @Test
    @SneakyThrows
    public void testNoVersionInfoInApi() {
        BundleMojo mojo = new BundleMojo();

        Assert.assertThrows(MojoExecutionException.class, () ->
            mojo.versionFileName("payment-order-client-api-v2.yaml", createOpenApiWithVersion(null)));
    }

    @Test
    @SneakyThrows
    public void testInvalidVersionInApi() {
        BundleMojo mojo = new BundleMojo();

        OpenAPI openAPI = new OpenAPI();
        openAPI.setInfo(new Info());
        Assert.assertThrows(MojoExecutionException.class, () ->
            mojo.versionFileName("payment-order-client-api-v2.yaml", createOpenApiWithVersion("3.0.0")));
    }


    private OpenAPI createOpenApiWithVersion(String version) {
        OpenAPI openAPI = new OpenAPI();
        openAPI.setInfo(new Info());
        openAPI.getInfo().setVersion(version);
        return openAPI;
    }


}
