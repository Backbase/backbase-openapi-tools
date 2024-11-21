package com.backbase.oss.boat;

import com.google.common.io.Files;
import java.io.File;
import lombok.extern.slf4j.Slf4j;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.assertj.core.util.Arrays;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertThrows;

@Slf4j
@SuppressWarnings("java:S2699")
class LintMojoTests {

    @Test
    void testFailOnWarningNoWarnings() throws MojoFailureException, MojoExecutionException {
        LintMojo lintMojo = new LintMojo();
        lintMojo.setIgnoreRules(Arrays.array("219", "105", "104", "151", "134", "115","M0012", "224", "B013", "B014", "B007U", "B009U"));
        lintMojo.setInput(getFile("/oas-examples/no-lint-warnings.yaml"));
        lintMojo.setFailOnWarning(true);
        lintMojo.execute();
    }


    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void testsFailOnWarningWithReport(boolean report) {
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
    void testsLintFile(boolean report, boolean fail, String fileName)  {
        LintMojo lintMojo = new LintMojo();
        lintMojo.setInput(getFile(fileName));
        lintMojo.setFailOnWarning(fail);
        lintMojo.setWriteLintReport(report);
        if(fail)
            assertThrows(MojoExecutionException.class, lintMojo::execute);
        else {
            assertDoesNotThrow(lintMojo::execute);
        }
    }

    @Test
    void testExceptionsNotExistingFile() {
        LintMojo lintMojo = new LintMojo();
        lintMojo.setInput(new File("I DO NOT EXIST"));
        lintMojo.failOnWarning = true;
        assertThrows(MojoExecutionException.class, lintMojo::execute);

    }

    @Test
    void testEmptyDirectory() {
        File empty = Files.createTempDir();
        LintMojo lintMojo = new LintMojo();
        lintMojo.setInput(empty);
        assertDoesNotThrow(lintMojo::execute);
    }

    @Test
    void testExceptionsWithInvalidFile() {
        LintMojo lintMojo = new LintMojo();
        lintMojo.setInput(getFile("/oas-examples/unable-to-parse.yaml"));
        lintMojo.showIgnoredRules = true;
        assertDoesNotThrow(lintMojo::execute);

    }


    private File getFile(String fileName) {
        return new File(getClass().getResource(fileName).getFile());
    }
}
