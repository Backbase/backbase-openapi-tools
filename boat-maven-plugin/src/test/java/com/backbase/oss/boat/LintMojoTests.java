package com.backbase.oss.boat;

import java.io.File;
import lombok.extern.slf4j.Slf4j;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.junit.Test;

@Slf4j
@SuppressWarnings("java:S2699")
public class LintMojoTests {

    @Test
    public void testLintFile() throws MojoFailureException, MojoExecutionException {
        LintMojo lintMojo = new LintMojo();
        lintMojo.setInput(getFile("/oas-examples/petstore.yaml"));
        lintMojo.setFailOnWarning(false);
        lintMojo.execute();
    }


    @Test
    public void testLintDirectory() throws MojoFailureException, MojoExecutionException {
        LintMojo lintMojo = new LintMojo();
        lintMojo.setInput(getFile("/oas-examples/"));
        lintMojo.setFailOnWarning(false);
        lintMojo.execute();
    }

    @Test(expected = MojoExecutionException.class)
    public void testFailOnWarning() throws MojoFailureException, MojoExecutionException {
        LintMojo lintMojo = new LintMojo();
        lintMojo.setInput(getFile("/oas-examples/petstore.yaml"));
        lintMojo.setFailOnWarning(true);
        lintMojo.execute();
    }


    private File getFile(String fileName) {
        return new File(getClass().getResource(fileName).getFile());
    }
}
