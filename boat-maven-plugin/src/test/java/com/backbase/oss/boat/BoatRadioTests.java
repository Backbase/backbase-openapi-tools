package com.backbase.oss.boat;

import static org.mockserver.integration.ClientAndServer.startClientAndServer;

import java.io.File;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.assertj.core.util.Arrays;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockserver.configuration.ConfigurationProperties;
import org.mockserver.integration.ClientAndServer;

public class BoatRadioTests {
    private ClientAndServer boatBayMockServer;
    private final String sourcekey = "repo-petstore";

    @BeforeEach
    public void setUp(){
        String initializationJsonPath = getClass().getResource("/boat-bay-mock-server/mock-upload-request-response.json").getPath();
        ConfigurationProperties.initializationJsonPath(initializationJsonPath);
        boatBayMockServer = startClientAndServer(8080);


    }
    @Test
    void testLintUpload() throws MojoFailureException, MojoExecutionException {

        LintMojo lintMojo = new LintMojo();
        lintMojo.project  = new MavenProject();
        lintMojo.project.setGroupId("repo.backbase.com");
        lintMojo.project.setArtifactId("petstore");
        lintMojo.project.setVersion("1.0.0-SNAPSHOT");
        lintMojo.project.setFile(getFile("/oas-examples"));
        lintMojo.setSourceKey(sourcekey);
        lintMojo.setBoatBayUrl("http://localhost:8080/");
        lintMojo.output= new File("/Users/sophiej/Documents/Projects/opensauce/fresh-water-boat/backbase-openapi-tools/boat-maven-plugin/src/test/resources/upload-reports");
        lintMojo.setIgnoreRules(Arrays.array("219", "105", "104", "151", "134", "115"));
        lintMojo.setInput(getFile("/oas-examples/petstore-v1.0.0.yaml"));
        lintMojo.setFailOnWarning(false);
        lintMojo.execute();
    }
    private File getFile(String fileName) {
        return new File(getClass().getResource(fileName).getFile());
    }

}
