package com.backbase.oss.sonar;

import com.backbase.oss.boat.quay.BoatLinter;
import com.backbase.oss.boat.quay.model.BoatLintReport;
import com.backbase.oss.boat.sonar.Coverage;
import com.backbase.oss.boat.sonar.SonarReportConvertor;
import com.backbase.oss.boat.sonar.model.BoatSonarIssues;
import io.swagger.util.Json;
import java.io.IOException;
import java.nio.charset.Charset;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.junit.Assert;
import org.junit.Before;

@Slf4j
public class SonarReportConvertorTest {

    private BoatLinter boatLinter;

    @Before
    public void setupBoatLinter() {
        boatLinter = new BoatLinter();
    }

    @org.junit.Test
    public void convert() throws IOException {

        String filePath = "/openapi/presentation-client-api/openapi.yaml";
        String openApiContents = IOUtils.resourceToString(filePath, Charset.defaultCharset());
        BoatLintReport boatLintReport = boatLinter.lint(openApiContents);
        boatLintReport.setFilePath(filePath);

        BoatSonarIssues boatSonarIssues = SonarReportConvertor.convert(boatLintReport);
        Coverage coverage = SonarReportConvertor.generateCoverage(boatLintReport);
        log.info("\n{}", Json.pretty(boatSonarIssues));
        log.info("\n{}", SonarReportConvertor.generateCoverageXml(boatLintReport));
        Assert.assertEquals(boatSonarIssues.getIssues().size(), boatLintReport.getViolations().size());

    }
}