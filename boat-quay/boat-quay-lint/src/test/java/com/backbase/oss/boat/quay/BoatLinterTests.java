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

    /**
     * Zally's rule 219 validates every OpenAPI 3 document against the OAS 3.0 JSON schema, so on a 3.1
     * document it reports violations that are false positives by construction. They must not reach the
     * report.
     */
    @Test
    void doesNotReportSchemaViolationsThatCannotApplyToOpenApi31() throws IOException, OpenAPILoaderException {
        String openApiContents = IOUtils.resourceToString("/openapi/openapi-3-1/openapi.yaml", Charset.defaultCharset());

        BoatLintReport boatLintReport = boatLinter.lint(openApiContents);

        assertFalse(hasViolationOfRule(boatLintReport, "219"),
            "Rule 219 cannot validate a 3.1 document and must be skipped for one.");
        assertFalse(hasViolationOfRule(boatLintReport, "M0012"),
            "3.1.x must be an accepted OpenAPI version.");
    }

    @Test
    void stillReportsSchemaViolationsForOpenApi30() throws IOException, OpenAPILoaderException {
        String openApiContents = IOUtils.resourceToString("/openapi/presentation-client-api/openapi.yaml", Charset.defaultCharset());

        BoatLintReport boatLintReport = boatLinter.lint(openApiContents);

        assertTrue(boatLintReport.hasViolations());
        // 3.0 documents keep going through every rule, rule 219 included.
        assertTrue(boatLintReport.getAvailableRules().stream().anyMatch(rule -> "219".equals(rule.getId())),
            "Rule 219 must remain registered for 3.0 documents.");
    }

    private boolean hasViolationOfRule(BoatLintReport report, String ruleId) {
        return report.getViolations().stream()
            .map(BoatViolation::getRule)
            .filter(java.util.Objects::nonNull)
            .anyMatch(rule -> ruleId.equals(rule.getId()));
    }
}
