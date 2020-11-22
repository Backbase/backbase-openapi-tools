package com.backbase.oss.boat;

import com.backbase.oss.boat.quay.model.BoatLintReport;
import com.backbase.oss.boat.sonar.SonarReportConvertor;
import com.backbase.oss.boat.sonar.model.BoatSonarIssues;
import io.swagger.util.Json;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.jetbrains.annotations.NotNull;

@SuppressWarnings("FieldMayBeFinal")
@Mojo(name = "sonar", requiresDependencyResolution = ResolutionScope.RUNTIME, threadSafe = true)
@Slf4j
public class SonarMojo extends AbstractLintMojo {

    public static final String SONAR_SOURCES = "sonar.sources";
    public static final String SONAR_EXTERNAL_ISSUES_REPORT_PATHS = "sonar.externalIssuesReportPaths";
    public static final String SONAR_COVERAGE_REPORT_PATHS = "sonar.coverageReportPaths";
    @Parameter(name = "sonarOutput",
        defaultValue = "${project.build.directory}/sonar-reports")
    protected File sonarOutput;

    @Parameter(defaultValue = "${project}", required = true, readonly = false)
    protected MavenProject project;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        List<BoatLintReport> boatLintReports = lint();
        List<String> sonarIssueReports = new ArrayList<>();
        List<String> sonarCoverageReports = new ArrayList<>();


        for (BoatLintReport lintReport : boatLintReports) {
            String fileName = StringUtils.substringAfterLast(lintReport.getFilePath(), File.separator);
            String jsonOutputFile = StringUtils.substringBeforeLast(fileName, ".") + ".json";
            String coverageOutputfile = StringUtils.substringBeforeLast(fileName, ".") + ".xml";
            BoatSonarIssues sonarReport = SonarReportConvertor.convert(lintReport);
            try {
                Files.createDirectories(sonarOutput.toPath());
            } catch (IOException e) {
                throw new MojoExecutionException("Cannot create sonar report output directory: " + sonarOutput, e);
            }
            File issueReport = new File(sonarOutput, jsonOutputFile);
            File coverageReport = new File(sonarOutput,  coverageOutputfile);
            try {
                Files.write(issueReport.getAbsoluteFile().toPath(), Json.pretty(sonarReport).getBytes());
                Files.write(coverageReport.getAbsoluteFile().toPath(), SonarReportConvertor.generateCoverageXml(lintReport).getBytes());
                sonarIssueReports.add(getRelativePath(issueReport.getAbsoluteFile()));
                sonarCoverageReports.add(getRelativePath(coverageReport));
            } catch (IOException e) {
                throw new MojoExecutionException("Cannot write sonar report to: " + issueReport, e);
            }
        }
        Properties properties = getProperties();
        String[] existingSonarSources = getExistingSonarSources();

        // Create CSV for all lint reports
        File specDirectory = inputSpec.isDirectory() ? inputSpec : inputSpec.getParentFile();
        List<String> lintReportSources = Stream.of(specDirectory)
            .map(this::getRelativePath)
            .collect(Collectors.toList());

        // Append existing sources to sonar.sources
        lintReportSources.addAll(Arrays.asList(existingSonarSources));

        String sonarSources = String.join(",", lintReportSources);
        log.info("Set {} property with: {}", SONAR_SOURCES, sonarSources);
        properties.setProperty(SONAR_SOURCES, sonarSources);

        String issueReportPaths = String.join(",", sonarIssueReports);
        String coverageReportPaths = String.join(",", sonarCoverageReports);

        log.info("Set {} property with: {}", SONAR_EXTERNAL_ISSUES_REPORT_PATHS, issueReportPaths);
        properties.setProperty(SONAR_EXTERNAL_ISSUES_REPORT_PATHS, issueReportPaths);

        log.info("Set {} property with: {}", SONAR_COVERAGE_REPORT_PATHS, coverageReportPaths);
        properties.setProperty(SONAR_COVERAGE_REPORT_PATHS, coverageReportPaths);
    }

    private String getRelativePath(File specDir) {
        return project.getBasedir().toPath().relativize(specDir.toPath()).toString();
    }

    @NotNull
    private String[] getExistingSonarSources() {
        return getProperties().containsKey(SONAR_SOURCES) ?
            getProperties().getOrDefault(SONAR_SOURCES, "").toString().split(",") :
            new String[]{};
    }

    private Properties getProperties() {
        if (project.getProperties() == null) {
            log.warn("can this happen?");
            Properties properties = new Properties();
            project.getModel().setProperties(properties);
            return properties;
        }
        return project.getProperties();
    }

    public void setSonarOutput(File sonarOutput) {
        this.sonarOutput = sonarOutput;
    }
}
