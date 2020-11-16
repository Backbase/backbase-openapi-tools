package com.backbase.oss.boat.quay;

import com.backbase.oss.boat.quay.configuration.RulesValidatorConfiguration;
import com.typesafe.config.Config;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.List;
import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Test;
import org.zalando.zally.core.ApiValidator;
import org.zalando.zally.core.DefaultContextFactory;
import org.zalando.zally.core.Result;
import org.zalando.zally.core.RulesManager;

public class BoatLinterTests {

    BoatLinter boatLinter;
    private RulesManager rulesManager;

    @Before
    public void setupBoatLinter() {

        RulesValidatorConfiguration rulesValidatorConfiguration = new RulesValidatorConfiguration();
        Config config = rulesValidatorConfiguration.config("boat.conf");
        rulesManager = rulesValidatorConfiguration.rulesManager(config);
        ApiValidator apiValidator = rulesValidatorConfiguration.apiValidator(rulesManager, new DefaultContextFactory());
        boatLinter = new BoatLinter(apiValidator);
    }

    @Test
    public void testRules() throws IOException {
        String openApiContents = IOUtils.resourceToString("/openapi/presentation-client-api/openapi.yaml", Charset.defaultCharset());
        List<Result> lint = boatLinter.lint(openApiContents);

        for (Result result : lint) {
            System.out.println(result.toString());

        }

    }

    @Test
    public void ruleManager() {

        rulesManager.getRules().forEach(ruleDetails -> {
            System.out.println(ruleDetails.toString());
        });
    }
}
