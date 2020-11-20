package com.backbase.oss.sonar;

import com.backbase.oss.boat.quay.BoatLinter;
import com.backbase.oss.boat.quay.model.BoatLintReport;
import com.backbase.oss.sonar.model.BoatSonarReport;
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

        BoatSonarReport boatSonarReport = SonarReportConvertor.convert(boatLintReport);
        log.info("\n{}",Json.pretty(boatSonarReport));
        Assert.assertEquals(boatSonarReport.getIssues().size(), boatLintReport.getViolations().size());

    }
}