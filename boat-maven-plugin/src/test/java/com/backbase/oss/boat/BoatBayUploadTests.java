package com.backbase.oss.boat;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.assertj.core.util.Arrays;
import org.junit.jupiter.api.Test;

import java.io.File;

public class BoatBayUploadTests {
  @Test
  void testLintUpload() throws MojoFailureException, MojoExecutionException {
    LintMojo lintMojo = new LintMojo();
    lintMojo.project  = new MavenProject();
    lintMojo.project.setGroupId("com.backbase.oss.boat.petstore");
    lintMojo.project.setArtifactId("petstore-spec");
    lintMojo.project.setVersion("1.0.0");
    lintMojo.project.setFile(getFile("/boat-doc-oas-examples/petstore.yaml"));
    lintMojo.setSourceKey("repo-petstore");
    lintMojo.setBoatBayUrl("http://localhost:8080");
    lintMojo.setIgnoreRules(Arrays.array("219", "105", "104", "151", "134", "115"));
    lintMojo.setInput(getFile("/boat-doc-oas-examples/petstore.yaml"));
    lintMojo.setFailOnWarning(true);
    lintMojo.execute();
  }

  private File getFile(String fileName) {
    return new File(getClass().getResource(fileName).getFile());
  }

}
