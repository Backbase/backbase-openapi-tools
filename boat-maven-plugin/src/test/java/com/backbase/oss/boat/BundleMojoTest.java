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
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

class BundleMojoTest {

    @Test
    @SneakyThrows
    void
    testInputDirectoryAndOutputFile() {
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
        var outputFolder = "target/test-bundle-folder";

        BundleMojo mojo = new BundleMojo();

        mojo.setInput(new File(getClass().getResource("/bundler/folder").getFile()));
        mojo.setOutput(new File(outputFolder));
        mojo.setIncludes(new String[]{"*-api-v*.yaml"});
        mojo.setVersionFileName(true);
        mojo.execute();

        assertTrue(new File(outputFolder, "one-client-api-v1.3.5.yaml").exists());
        assertTrue(new File(outputFolder, "one-client-api-v2.0.0.yaml").exists());
        assertTrue(new File(outputFolder, "another-client-api-v1.7.9.yaml").exists());
        assertFalse(new File(outputFolder, "test-client-api-v1.1.1.yaml").exists());
    }

    @Test
    @SneakyThrows
    void testBundleFolder_whenExcludesExists() {
        var outputFolder = "target/test-bundle-bundler";

        BundleMojo mojo = new BundleMojo();

        mojo.setInput(new File(getClass().getResource("/bundler").getFile()));
        mojo.setOutput(new File(outputFolder));
        mojo.setIncludes(new String[]{"**/*-api-v*.yaml"});
        mojo.setExcludes(new String[]{"**/folder/**"});
        mojo.setVersionFileName(true);
        mojo.setFlattenOutput(true);
        mojo.execute();

        assertFalse(new File(outputFolder, "one-client-api-v1.3.5.yaml").exists());
        assertFalse(new File(outputFolder, "one-client-api-v2.0.0.yaml").exists());
        assertFalse(new File(outputFolder, "another-client-api-v1.7.9.yaml").exists());
        assertTrue(new File(outputFolder, "test-client-api-v1.1.1.yaml").exists());
    }

    @Test
    @SneakyThrows
    void testBundleFolder_whenNotToBeFlattened_inSubDirStructure() {
        var outputFolder = "target/test-bundle-folder-with-subdir";

        BundleMojo mojo = new BundleMojo();

        mojo.setInput(new File(getClass().getResource("/bundler").getFile()));
        mojo.setOutput(new File(outputFolder));
        mojo.setIncludes(new String[]{"**/*api-v*.yaml"}); // Includes sub-directories by '**/'
        mojo.setVersionFileName(true);
        mojo.setFlattenOutput(false);

        mojo.execute();

        assertTrue(new File(outputFolder, "/folder/one-client-api-v1.3.5.yaml").exists());
        assertTrue(new File(outputFolder, "/folder/one-client-api-v2.0.0.yaml").exists());
        assertTrue(new File(outputFolder, "/folder/another-client-api-v1.7.9.yaml").exists());
    }

    @Test
    @SneakyThrows
    void testBundleFolder_whenToBeFlattened_inSubDirStructure() {
        var outputFolder = "target/test-bundle-folder-flattened";

        BundleMojo mojo = new BundleMojo();

        mojo.setInput(new File(getClass().getResource("/bundler").getFile()));
        mojo.setOutput(new File(outputFolder));
        mojo.setIncludes(new String[]{"**/*api-v*.yaml"}); // Includes sub-directories by '**/'
        mojo.setVersionFileName(true);
        mojo.setFlattenOutput(true);

        mojo.execute();

        assertTrue(new File(outputFolder, "one-client-api-v1.3.5.yaml").exists());
        assertTrue(new File(outputFolder, "one-client-api-v2.0.0.yaml").exists());
        assertTrue(new File(outputFolder, "another-client-api-v1.7.9.yaml").exists());
    }

    @Test
    @SneakyThrows
    void testVersionFileName() {
        BundleMojo mojo = new BundleMojo();

        assertEquals(
            "payment-order-client-api-v2.0.0.yaml",
            mojo.versionFileName("payment-order-client-api-v2.yaml", createOpenApiWithVersion("2.0.0")));
    }

    @Test
    @SneakyThrows
    void testNoInfoInApi() {
        BundleMojo mojo = new BundleMojo();

        assertThrows(MojoExecutionException.class, () ->
            mojo.versionFileName("payment-order-client-api-v2.yaml", new OpenAPI()));
    }

    @Test
    @SneakyThrows
    void testNoVersionInfoInApi() {
        BundleMojo mojo = new BundleMojo();

        assertThrows(MojoExecutionException.class, () ->
            mojo.versionFileName("payment-order-client-api-v2.yaml", createOpenApiWithVersion(null)));
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

    @Test
    @SneakyThrows
    void testInvalidVersionInApi() {
        BundleMojo mojo = new BundleMojo();

        OpenAPI openAPI = new OpenAPI();
        openAPI.setInfo(new Info());
        assertThrowsMojoExecutionExceptionWithMessage(
            () -> mojo.versionFileName("payment-order-client-api-v2.yaml", createOpenApiWithVersion("3.0.0")),
            "Invalid version 3.0.0 in file payment-order-client-api-v2.yaml");
        assertThrowsMojoExecutionExceptionWithMessage(
            () -> mojo.versionFileName("payment-order-client-api-v2.yaml", createOpenApiWithVersion("v2.0")),
            "Version should be semver (or at least have a recognisable major version), but found 'v2.0'");
        assertThrowsMojoExecutionExceptionWithMessage(
            ()  -> mojo.versionFileName("payment-order-client-api-v2.yaml", createOpenApiWithVersion("2dada")),
            "Version should be semver (or at least have a recognisable major version), but found '2dada'");

    }

    @ParameterizedTest
    @ValueSource(strings = { "outputFile.yaml", "/client-api/v1/outputFile.yaml"})
    void testOutputFileName_DefaultBehaviour(String outputFileName) {
        BundleMojo mojo = new BundleMojo();

        var actual = mojo.normalizeOutputFileName(outputFileName);

        assertEquals(outputFileName, actual);
    }

    @Test
    void testOutputFileName_WhenToBeFlattened_SimpleStructure() {
        BundleMojo mojo = new BundleMojo();
        mojo.setFlattenOutput(true);

        var fileName = "outputFile.yaml";

        var expected = fileName;
        var actual = mojo.normalizeOutputFileName(fileName);

        assertEquals(expected, actual);
    }

    @Test
    void testOutputFileName_WhenToBeFlattened_NestedStructure() {
        BundleMojo mojo = new BundleMojo();
        mojo.setFlattenOutput(true);

        var fileName = "/client-api/v1/outputFile.yaml";

        var expected = "outputFile.yaml";
        var actual = mojo.normalizeOutputFileName(fileName);

        assertEquals(expected, actual);
    }

    private void assertThrowsMojoExecutionExceptionWithMessage(Executable executable, String message) {
        MojoExecutionException thrown = assertThrows(MojoExecutionException.class, executable);
        assertTrue(thrown.getMessage().startsWith(message), "Expected message '" + message + "' but got '"
            + thrown.getMessage() + "'");
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
