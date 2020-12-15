package com.backbase.oss.boat;

import java.io.File;
import lombok.extern.slf4j.Slf4j;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.assertj.core.util.Arrays;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertThrows;

@Slf4j
@SuppressWarnings("java:S2699")
public class LintMojoTests {

    @Test
    public void testFailOnWarningNoWarnings() throws MojoFailureException, MojoExecutionException {
        LintMojo lintMojo = new LintMojo();
        lintMojo.setIgnoreRules(Arrays.array("219", "105", "104", "151"));
        lintMojo.setInput(getFile("/oas-examples/no-lint-warnings.yaml"));
        lintMojo.setFailOnWarning(true);
        lintMojo.execute();
    }


    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    public void testsFailOnWarningWithReport(boolean report) throws MojoFailureException, MojoExecutionException {
        LintMojo lintMojo = new LintMojo();
        lintMojo.setIgnoreRules(new String[]{"219", "105", "M008", "M009", "M010", "M011", "H001", "H002",
            "S005", "S006", "S007"});
        lintMojo.setInput(getFile("/oas-examples/petstore.yaml"));
        lintMojo.setFailOnWarning(true);
        lintMojo.setWriteLintReport(report);

        assertThrows(MojoFailureException.class, lintMojo::execute);
    }


    @ParameterizedTest
    @CsvSource({
        "false, false, /oas-examples/petstore.yaml",
        "false, false, /oas-examples/",
        "true, false, /oas-examples/ "
    })
    public void testsLintFile(boolean report, boolean fail, String fileName) throws MojoFailureException, MojoExecutionException {
        LintMojo lintMojo = new LintMojo();
        lintMojo.setInput(getFile(fileName));
        lintMojo.setFailOnWarning(fail);
        lintMojo.setWriteLintReport(report);
        lintMojo.execute();
    }


    private File getFile(String fileName) {
        return new File(getClass().getResource(fileName).getFile());
    }
}
