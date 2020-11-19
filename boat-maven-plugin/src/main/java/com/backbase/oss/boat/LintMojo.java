package com.backbase.oss.boat;

import com.backbase.oss.boat.quay.BoatLinter;
import com.backbase.oss.boat.quay.model.BoatLintReport;
import com.backbase.oss.boat.quay.model.BoatViolation;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;

@SuppressWarnings("FieldMayBeFinal")
@Mojo(name = "lint", requiresDependencyResolution = ResolutionScope.RUNTIME, threadSafe = true)
@Slf4j
/*
  Lint Specification
 */
public class LintMojo extends AbstractMojo {

    @Parameter(name = "inputSpec", property = "inputSpec", required = true)
    private File inputSpec;

    @Parameter(name = "failOnWarning", defaultValue = "false")
    private boolean failOnWarning;

    @Parameter(name = "ignoreRules")
    private String[] ignoreRules = new String[]{"219", "105", "M008", " M009", " M010", " M011", " H001", " H002", " S005", " S006", " S007"};


    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        log.info("Linting OpenAPI: {}", inputSpec);

        File[] inputFiles;
        if (inputSpec.isDirectory()) {
            inputFiles = inputSpec.listFiles(pathname -> pathname.getName().endsWith(".yaml"));
            if (inputFiles == null) {
                throw new MojoExecutionException("No OpenAPI specs found in: " + inputSpec);
            }
            log.info("Found " + inputFiles.length + " specs to lint.");
        } else {
            inputFiles = new File[]{inputSpec};
        }

        for (File inputFile : inputFiles) {
            lintOpenAPI(inputFile);
        }

    }

    private void lintOpenAPI(File inputFile) throws MojoExecutionException {
        try {
            BoatLinter boatLinter = new BoatLinter(ignoreRules);
            String contents = IOUtils.toString(inputFile.toURI(), Charset.defaultCharset());

            BoatLintReport boatLintReport =  boatLinter.lint(contents);

            if (boatLintReport.getViolations().isEmpty()) {
                log.info("OpenAPI: {}, is valid! No warnings!", inputFile);
                return;
            }

            log.warn("OpenAPI: {} has Linting issues: ", inputFile);
            boatLintReport.getViolations().forEach(result -> log.warn("{}", result.toString()));
            if (failOnWarning) {
                throw new MojoExecutionException("Linting failed for input file: " + inputFile);
            }

        } catch (IOException e) {
            throw new MojoExecutionException("Error transforming OpenAPI: " + inputFile, e);
        }
    }




    public void setInput(File input) {
        this.inputSpec = input;
    }

    public void setFailOnWarning(boolean failOnWarning) {
        this.failOnWarning = failOnWarning;
    }

    public void setIgnoreRules(String[] ignoreRules) {
        this.ignoreRules = ignoreRules;
    }
}
