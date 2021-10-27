package com.backbase.oss.boat.quay;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.backbase.oss.boat.loader.OpenAPILoaderException;
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

class BoatLinterTests {

    BoatLinter boatLinter;

    @BeforeEach
    void setupBoatLinter() {
        boatLinter = new BoatLinter();
    }

    @Test
    void testRules() throws IOException, OpenAPILoaderException {
        String openApiContents = IOUtils.resourceToString("/openapi/presentation-client-api/openapi.yaml", Charset.defaultCharset());
        BoatLintReport boatLintReport = boatLinter.lint(openApiContents);

        assertTrue(boatLintReport.hasViolations());
    }

    @Test
    void testBoatViolationDisplay() throws IOException, OpenAPILoaderException {
        String openApiContents = IOUtils.resourceToString("/openapi/presentation-client-api/openapi.yaml", Charset.defaultCharset());
        BoatLintReport boatLintReport = boatLinter.lint(openApiContents);
        Optional<BoatViolation> testDisplay = boatLintReport.getViolations().stream()
            .filter(t -> t.displayString().contains("[B007]")).findFirst();
        assertTrue(testDisplay.isPresent());
        assertEquals("[B007] MUST - Check prefix for paths: Incorrect path prefix: wallet. Correct values are [client-api, service-api, integration-api]", testDisplay.get().displayString());
    }

    @Test
    void testRulesWithFile() throws IOException, OpenAPILoaderException {
        // Can't ret relative file from class path resources. Copy into new file
        String openApiContents = IOUtils.resourceToString("/openapi/presentation-client-api/openapi.yaml", Charset.defaultCharset());

        File inputFile = new File("target/openapi.yaml");
        Files.write(inputFile.toPath(), openApiContents.getBytes());

        BoatLintReport boatLintReport = boatLinter.lint(inputFile);

        assertTrue(boatLintReport.hasViolations());
    }

    @Test
    void testRulesWithFile_absolutePath() throws IOException, OpenAPILoaderException {
        // Can't ret relative file from class path resources. Copy into new file
        String openApiContents = IOUtils.resourceToString("/openapi/presentation-client-api/openapi.yaml", Charset.defaultCharset());

        File inputFile = new File("target/openapi.yaml").getAbsoluteFile();
        Files.write(inputFile.toPath(), openApiContents.getBytes());

        BoatLintReport boatLintReport = boatLinter.lint(inputFile);

        assertTrue(boatLintReport.hasViolations());
    }

    @Test
    void ruleManager() {
        List<BoatLintRule> availableRules = boatLinter.getAvailableRules();


        assertFalse(availableRules.isEmpty());

    }
}
