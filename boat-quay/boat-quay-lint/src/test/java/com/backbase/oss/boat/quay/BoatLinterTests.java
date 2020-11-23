package com.backbase.oss.boat.quay;

import com.backbase.oss.boat.quay.model.BoatLintReport;
import com.backbase.oss.boat.quay.model.BoatViolation;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.List;
import org.apache.commons.io.IOUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zalando.zally.core.RuleDetails;

public class BoatLinterTests {

    BoatLinter boatLinter;

    @Before
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
        Assert.assertTrue(boatLintReport.hasViolations());
    }

    @Test
    public void ruleManager() {
        boatLinter.getAvailableRules().forEach(ruleDetails -> {
            System.out.println(ruleDetails.toString());
        });

    }
}
