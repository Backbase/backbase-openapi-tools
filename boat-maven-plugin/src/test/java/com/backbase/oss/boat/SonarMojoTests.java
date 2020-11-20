package com.backbase.oss.boat;

import java.io.File;
import lombok.extern.slf4j.Slf4j;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.assertj.core.util.Arrays;
import org.junit.Test;

@Slf4j
@SuppressWarnings("java:S2699")
public class SonarMojoTests {

    @Test
    public void testSonarFile() throws MojoFailureException, MojoExecutionException {
        SonarMojo mojo = new SonarMojo();
        mojo.setInput(getFile("/oas-examples/petstore.yaml"));
        mojo.setFailOnWarning(false);
        mojo.setSonarOutput(new File("target/sonar-reports"));
        mojo.execute();
    }

    @Test
    public void testSonarDirectory() throws MojoFailureException, MojoExecutionException {
        SonarMojo mojo = new SonarMojo();
        mojo.setInput(getFile("/oas-examples/"));
        mojo.setFailOnWarning(false);
        mojo.setSonarOutput(new File("target/sonar-reports"));
        mojo.execute();
    }

    private File getFile(String fileName) {
        return new File(getClass().getResource(fileName).getFile());
    }
}
