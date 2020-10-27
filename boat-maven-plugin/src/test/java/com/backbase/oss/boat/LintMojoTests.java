package com.backbase.oss.boat;

import java.io.File;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.maven.plugin.MojoExecutionException;
import org.junit.Assert;
import org.junit.Test;

@Slf4j
@SuppressWarnings("java:S2699")
public class LintMojoTests {

    @SneakyThrows
    @Test
    public void testNonBreakingChange() {
        LintMojo lintMojo = new LintMojo();
        lintMojo.setInput(getFile("/oas-examples/petstore.yaml"));
        lintMojo.setFailOnWarning(false);
        lintMojo.execute();
    }


    private File getFile(String fileName) {
        return new File(getClass().getResource(fileName).getFile());
    }
}
