package com.backbase.oss.boat.quay;

import com.backbase.oss.boat.quay.model.BoatLintReport;
import com.backbase.oss.boat.quay.model.BoatLintRule;
import com.backbase.oss.boat.quay.model.BoatViolation;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.List;
import java.util.Optional;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class BoatLinterTests {

    BoatLinter boatLinter;

    @BeforeEach
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
        Optional<BoatViolation> testDisplay = boatLintReport.getViolations().stream()
            .filter(t -> t.displayString().contains("[B007]")).findFirst();
        assertTrue(testDisplay.isPresent());
        assertEquals("[B007] MUST - Check prefix for paths: Incorrect path prefix: wallet",testDisplay.get().displayString());
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
        List<BoatLintRule> availableRules = boatLinter.getAvailableRules();
        availableRules.forEach(ruleDetails -> {
            System.out.println(ruleDetails.toString());
        });

        assertFalse(availableRules.isEmpty());

    }
}
