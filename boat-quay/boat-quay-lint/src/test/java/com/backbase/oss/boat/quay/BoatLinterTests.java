package com.backbase.oss.boat.quay;

import com.backbase.oss.boat.quay.model.BoatLintReport;
import com.backbase.oss.boat.quay.model.BoatViolation;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class BoatLinterTests {

    BoatLinter boatLinter;

    @BeforeAll
    public void setupBoatLinter() {
        boatLinter = new BoatLinter();
    }

    @Test
    public void testRules() throws IOException {
        String openApiContents = IOUtils.resourceToString("/openapi/presentation-client-api/openapi.yaml", Charset.defaultCharset());
        BoatLintReport boatLintReport = boatLinter.lint(openApiContents);

        for (BoatViolation result : boatLintReport.getViolations()) {
            System.out.println(result.toString());
        }
        assertTrue(boatLintReport.hasViolations());
    }

    @Test
    public void testBoatViolationDisplay() throws IOException {
        String openApiContents = IOUtils.resourceToString("/openapi/presentation-client-api/openapi.yaml", Charset.defaultCharset());
        BoatLintReport boatLintReport = boatLinter.lint(openApiContents);
        BoatViolation testDisplay = boatLintReport.getViolations().get(0);
        if (!testDisplay.displayString().contains("[219]")){
            testDisplay = boatLintReport.getViolations().get(2);
        }
        assertEquals("[219] MUST - Provide API Audience: API Audience must be provided",testDisplay.displayString());
    }

    @Test
    public void testRulesWithFile() throws IOException {
        // Can't ret relative file from class path resources. Copy into new file
        String openApiContents = IOUtils.resourceToString("/openapi/presentation-client-api/openapi.yaml", Charset.defaultCharset());

        File inputFile = new File("target/openapi.yaml");
        Files.write(inputFile.toPath(), openApiContents.getBytes());

        BoatLintReport boatLintReport = boatLinter.lint(inputFile);

        for (BoatViolation result : boatLintReport.getViolations()) {
            System.out.println(result.toString());
        }
        assertTrue(boatLintReport.hasViolations());
    }

    @Test
    public void ruleManager() {
        boatLinter.getAvailableRules().forEach(ruleDetails -> {
            System.out.println(ruleDetails.toString());
        });


    }
}
