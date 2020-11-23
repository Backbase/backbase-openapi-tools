package com.backbase.oss.boat;

import com.backbase.oss.boat.quay.model.BoatLintReport;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.ResolutionScope;

@SuppressWarnings("FieldMayBeFinal")
@Mojo(name = "lint", requiresDependencyResolution = ResolutionScope.RUNTIME, threadSafe = true)
@Slf4j
/*
  Lint Specification
 */
public class LintMojo extends AbstractLintMojo {

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        List<BoatLintReport> boatLintReports = lint();

        boolean isFailed = false;
        for (BoatLintReport report : boatLintReports) {
            log.info("Lint report for: {}", report.getTitle());
            if (!report.hasViolations()) {
                log.info("OpenAPI: {}, is valid! No warnings!", report.getFilePath());
            } else {
                isFailed =  true;
                log.warn("OpenAPI: {} has Linting issues: ", report.getFilePath());
                report.getViolations().forEach(result -> log.warn("{}", result.displayString()));
            }
        }

        if (isFailed && failOnWarning) {
            throw new MojoFailureException("Linting " + inputSpec + " failed. Please correct the found issues and try again");
        }

    }

}
