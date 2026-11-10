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
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class BoatLinterTests {

    private static final List<String> GUIDELINE_PREVIEW_RULES = Arrays.asList(
        "B004", "B016", "B017", "B018", "B019", "B020", "B021", "B022", "B023", "B024", "M010");

    private static final List<String> GUIDELINE_RULES = Stream.concat(
        GUIDELINE_PREVIEW_RULES.stream(), Stream.of("B014")).collect(Collectors.toList());

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

    @Test
    void previewViolationsAreReportedButNotBlocking() throws IOException, OpenAPILoaderException {
        String openApiContents = IOUtils.resourceToString("/openapi/presentation-client-api/openapi.yaml", Charset.defaultCharset());
        List<String> allRules = boatLinter.getAvailableRules().stream().map(BoatLintRule::getId).collect(Collectors.toList());

        BoatLintReport previewReport = new BoatLinter(Collections.emptyList(), allRules).lint(openApiContents);
        BoatLintReport blockingReport = new BoatLinter(Collections.emptyList(), Collections.emptyList()).lint(openApiContents);

        assertTrue(previewReport.hasViolations());
        assertFalse(previewReport.hasBlockingViolations());
        assertTrue(previewReport.getViolations().stream().allMatch(violation -> violation.getRule().isPreview()));
        assertTrue(previewReport.getViolations().stream().allMatch(violation -> violation.displayString().contains("(preview) - ")));
        assertTrue(blockingReport.hasBlockingViolations());
        assertTrue(blockingReport.getViolations().stream().noneMatch(violation -> violation.getRule().isPreview()));
    }

    /**
     * M010 uses the CaseChecker block in boat.conf: camelCase path parameters, {@code _metadata}, SCREAMING_SNAKE_CASE
     * enum values and the Service SDK headers follow the guideline.
     */
    @ParameterizedTest
    @ValueSource(strings = {"3.0.3", "3.1.0"})
    void caseCheckerFollowsGuidelineNaming(String openApiVersion) throws IOException, OpenAPILoaderException {
        String openApiContents = IOUtils.resourceToString("/openapi/case-checker/openapi.yaml", Charset.defaultCharset())
            .replace("openapi: 3.1.0", "openapi: " + openApiVersion);

        BoatLintReport boatLintReport = boatLinter.lint(openApiContents);

        List<String> caseViolations = boatLintReport.getViolations().stream()
            .filter(violation -> "M010".equals(violation.getRule().getId()))
            .map(BoatViolation::getDescription)
            .collect(Collectors.toList());
        assertEquals(1, caseViolations.size(), caseViolations::toString);
        assertTrue(caseViolations.get(0).startsWith("Path parameter 'payment-order-id' does not match camelCase"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"3.0.3", "3.1.0"})
    void specFollowingTheGuidelineHasNoGuidelineViolations(String openApiVersion) throws IOException, OpenAPILoaderException {
        BoatLintReport boatLintReport = guidelineLinter().lint(guidelineSpec("follows-guideline.yaml", openApiVersion));

        List<String> violations = boatLintReport.getViolations().stream()
            .filter(violation -> GUIDELINE_RULES.contains(violation.getRule().getId()))
            .map(BoatViolation::displayString)
            .collect(Collectors.toList());
        assertTrue(violations.isEmpty(), violations::toString);
    }

    @ParameterizedTest
    @ValueSource(strings = {"3.0.3", "3.1.0"})
    void specViolatingTheGuidelineIsReportedByEveryGuidelineRule(String openApiVersion) throws IOException, OpenAPILoaderException {
        BoatLintReport boatLintReport = guidelineLinter().lint(guidelineSpec("violates-guideline.yaml", openApiVersion));

        List<BoatViolation> violations = guidelineViolations(boatLintReport);
        Set<String> violatedRules = violations.stream().map(violation -> violation.getRule().getId()).collect(Collectors.toSet());
        assertEquals(new HashSet<>(GUIDELINE_PREVIEW_RULES), violatedRules);
        assertTrue(violations.stream().allMatch(violation -> violation.getRule().isPreview()));
    }

    @Test
    void guidelineRulesReportTheSameViolationsOnOpenApi30And31() throws IOException, OpenAPILoaderException {
        List<String> openApi30 = guidelineViolations(guidelineLinter().lint(guidelineSpec("violates-guideline.yaml", "3.0.3")))
            .stream().map(violation -> violation.getPointer() + " " + violation.displayString()).collect(Collectors.toList());
        List<String> openApi31 = guidelineViolations(guidelineLinter().lint(guidelineSpec("violates-guideline.yaml", "3.1.0")))
            .stream().map(violation -> violation.getPointer() + " " + violation.displayString()).collect(Collectors.toList());

        assertEquals(openApi30, openApi31);
    }

    private BoatLinter guidelineLinter() {
        return new BoatLinter(Collections.emptyList(), GUIDELINE_PREVIEW_RULES);
    }

    private List<BoatViolation> guidelineViolations(BoatLintReport report) {
        return report.getViolations().stream()
            .filter(violation -> GUIDELINE_PREVIEW_RULES.contains(violation.getRule().getId()))
            .collect(Collectors.toList());
    }

    /**
     * The fixtures follow and violate the "RESTful API in Backbase v3" guideline.
     */
    private static String guidelineSpec(String name, String openApiVersion) throws IOException {
        return IOUtils.resourceToString("/guideline/" + name, Charset.defaultCharset())
            .replace("openapi: 3.1.0", "openapi: " + openApiVersion);
    }

    private boolean hasViolationOfRule(BoatLintReport report, String ruleId) {
        return report.getViolations().stream()
            .map(BoatViolation::getRule)
            .filter(java.util.Objects::nonNull)
            .anyMatch(rule -> ruleId.equals(rule.getId()));
    }
}
