package com.backbase.oss.boat;

import com.backbase.oss.boat.quay.model.BoatLintReport;
import com.backbase.oss.sonar.SonarReportConvertor;
import com.backbase.oss.sonar.model.BoatSonarReport;
import io.swagger.util.Json;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.shared.utils.io.IOUtil;

@SuppressWarnings("FieldMayBeFinal")
@Mojo(name = "sonar", requiresDependencyResolution = ResolutionScope.RUNTIME, threadSafe = true)
@Slf4j
/*
  Lint Specification
 */
public class SonarMojo extends AbstractLintMojo {

    @Parameter(name = "sonarOutput",
        defaultValue = "${project.build.directory}/sonar-reports")
    protected File sonarOutput;


    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        List<BoatLintReport> boatLintReports = lint();

        for (BoatLintReport lintReport : boatLintReports) {
            String fileName = StringUtils.substringAfterLast(lintReport.getFilePath(), File.separator);
            String jsonOutputFile = StringUtils.substringBeforeLast(fileName, ".") + ".json";
            BoatSonarReport sonarReport = SonarReportConvertor.convert(lintReport);

            try {
                Files.createDirectories(sonarOutput.toPath());
            } catch (IOException e) {
                throw new MojoExecutionException("Cannot create sonar report output directory: " + sonarOutput, e);
            }
            File outputFile = new File(sonarOutput, jsonOutputFile);
            try {
                Files.write(outputFile.getAbsoluteFile().toPath(), Json.pretty(sonarReport).getBytes());
            } catch (IOException e) {
                throw new MojoExecutionException("Cannot write sonar report to: " + outputFile, e);
            }
        }

    }

    public void setSonarOutput(File sonarOutput) {
        this.sonarOutput = sonarOutput;
    }
}
