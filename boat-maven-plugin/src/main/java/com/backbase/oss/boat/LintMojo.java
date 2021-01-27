package com.backbase.oss.boat;

import com.backbase.oss.boat.bay.client.ApiException;
import com.backbase.oss.boat.quay.model.BoatLintReport;
import com.backbase.oss.codegen.lint.BoatLintConfig;
import com.backbase.oss.codegen.lint.BoatLintGenerator;
import java.io.File;
import java.net.URL;
import java.util.List;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import sun.tools.java.Environment;

@SuppressWarnings("FieldMayBeFinal")
@Mojo(name = "lint", requiresDependencyResolution = ResolutionScope.RUNTIME, threadSafe = true)
@Slf4j
/**
 * API lint which provides checks for compliance with many of Backbase's API standards.
 */
public class LintMojo extends AbstractLintMojo {

    /**
     * Output directory for lint reports.
     */
    @Parameter(name = "output", defaultValue = "${project.build.directory}/boat-lint-reports")
    public File output;

    /**
     * Set this to <code>true</code> to generate lint report.
     */
    @Parameter(name = "writeLintReport", defaultValue = "true")
    private boolean writeLintReport;

    @Parameter(readonly = true, required = true, defaultValue = "${project}")
    protected MavenProject project;

    @Parameter(name = "boatBayUrl", defaultValue = "hjhb", property = "boat.maven.plugin.lint.boatBayUrl")
    private String boatBayUrl;

    @Parameter(name = "sourceId", defaultValue = "")
    private String sourceId;

    private BoatBayRadio boatBayRadio;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {


        List<BoatLintReport> boatLintReports = null;
        try {
            if (!sourceId.isEmpty()
                    || !System.getenv("boatBayUrl").isEmpty()
                    || System.getenv("boatBayUrl") != null
                    || !boatBayUrl.isEmpty()) {
                new BoatBayRadio(inputSpec,output,project).upload(sourceId);
              //  System.getenv().containsKey("boatBayUrl");
            }else {
                boatLintReports = lint();
            }
        } catch (MojoExecutionException | ApiException e) {
            if (failOnWarning) {
                throw new MojoExecutionException(e.getMessage());
            }
        }
        if (boatLintReports == null) {
            log.warn("No reports generated for input: {}", inputSpec);
            return;
        }
        boolean isSingleLint = boatLintReports.size() == 1;
        boolean isFailed = false;
        for (BoatLintReport report : boatLintReports) {
            log.info("Lint report for: {}", report.getTitle());

            if (!report.hasViolations()) {
                log.info("OpenAPI: {}, is valid! No warnings!", report.getFilePath());
            } else {
                isFailed = true;
                log.warn("OpenAPI: {} has Linting issues: ", report.getFilePath());
                report.getViolations().forEach(result -> log.warn("{}", result.displayString()));
            }

            if (writeLintReport) {
                generateLintReport(isSingleLint, report);
            }
        }
        if (isFailed && failOnWarning) {
            throw new MojoFailureException("Linting " + inputSpec + " failed. Please correct the found issues and try again");
        }

    }

    private void generateLintReport(boolean isSingleLint, BoatLintReport report) {
        BoatLintConfig config = new BoatLintConfig();
        File reportOutputDir = getOutput();
        config.setOutputDir(reportOutputDir.toString());
        if (!isSingleLint) {

            String lintReportOutput = new File(reportOutputDir, report.getFileName()).toString();
            log.info("Writing LINT Report for: {} to: {}", report.getTitle(), lintReportOutput);
            config.setOutputDir(lintReportOutput);
        }
        BoatLintGenerator boatLintGenerator = new BoatLintGenerator(config);
        boatLintGenerator.generate(report);
    }

    private File getOutput() {
        if (this.output == null) {
            output = new File("./target/boat-lint");
        }
        return this.output;
    }


    public void setSourceId(String sourceId){ this.sourceId = sourceId; }

    public void setBoatBayUrl(String boatBayUrl){this.boatBayUrl = boatBayUrl;}

    public void setWriteLintReport(boolean writeLintReport) { this.writeLintReport = writeLintReport; }
}
