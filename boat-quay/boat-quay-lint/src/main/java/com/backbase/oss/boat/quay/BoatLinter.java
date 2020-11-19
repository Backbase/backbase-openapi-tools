package com.backbase.oss.boat.quay;

import com.backbase.oss.boat.loader.OpenAPILoader;
import com.backbase.oss.boat.quay.configuration.RulesValidatorConfiguration;
import com.backbase.oss.boat.quay.model.BoatLintReport;
import com.backbase.oss.boat.quay.model.BoatLintRule;
import com.backbase.oss.boat.quay.model.BoatViolation;
import com.typesafe.config.Config;
import io.swagger.v3.oas.models.OpenAPI;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.jetbrains.annotations.NotNull;
import org.zalando.zally.core.ApiValidator;
import org.zalando.zally.core.DefaultContextFactory;
import org.zalando.zally.core.Result;
import org.zalando.zally.core.RulesManager;
import org.zalando.zally.core.RulesPolicy;
import org.zalando.zally.rule.api.Rule;
import org.zalando.zally.rule.api.RuleSet;

public class BoatLinter {

    private final ApiValidator validator;

    private final URI documentationBaseUrl = URI.create("https://backbase.github.io/backbase-openapi-tools/rules.md");
    private final RulesManager rulesManager;
    private final RulesPolicy rulesPolicy;
    private final Map<String, BoatLintRule> availableRules;

    public BoatLinter() {
        this(new String[]{});
    }

    public BoatLinter(String... ignoreRules) {

        RulesValidatorConfiguration rulesValidatorConfiguration = new RulesValidatorConfiguration();
        Config config = rulesValidatorConfiguration.config("boat.conf");
        this.rulesManager = rulesValidatorConfiguration.rulesManager(config);
        this.rulesPolicy = new RulesPolicy(Arrays.asList(ignoreRules));
        this.validator = rulesValidatorConfiguration.apiValidator(rulesManager, new DefaultContextFactory());
        this.availableRules = mapAvailableRules();
    }

    public BoatLintReport lint(String openApiContent) {
        List<Result> validate = validator.validate(openApiContent, rulesPolicy, null);
        List<BoatViolation> violations = validate.stream()
            .map(this::transformResult)
            .collect(Collectors.toList());
        OpenAPI openAPI = OpenAPILoader.parse(openApiContent);

        BoatLintReport boatLintReport = new BoatLintReport();
        boatLintReport.setOpenApi(openApiContent);
        boatLintReport.setAvailableRules(getAvailableRules());
        boatLintReport.setViolations(violations);
        boatLintReport.setTitle(openAPI.getInfo().getTitle());
        boatLintReport.setVersion(openAPI.getInfo().getVersion());

        return boatLintReport;


    }

    private BoatViolation transformResult(Result result) {

        BoatViolation violation = new BoatViolation();
        violation.setLintRule(availableRules.get(result.getId()));
        violation.setDescription(result.getDescription());
        violation.setPointer(result.getPointer());
        violation.setLines(result.getLines());
        violation.setSeverity(result.getViolationType());
        return violation;
    }

    @NotNull
    private URI getUri(String id, String title) {
        String heading = id + ":" + title;
        String ref = heading.toLowerCase().replaceAll("[^a-z0-9]+", "-");
        return documentationBaseUrl.resolve("#" + ref);
    }


    private Map<String, BoatLintRule> mapAvailableRules() {
        Map<String, BoatLintRule> rules = new LinkedHashMap<>();

        rulesManager.rules(rulesPolicy)
            .forEach(ruleDetails -> {
                BoatLintRule boatLintRule = new BoatLintRule();
                Rule rule = ruleDetails.getRule();
                RuleSet ruleSet = ruleDetails.getRuleSet();
                boatLintRule.setId(rule.id());
                boatLintRule.setUrl(getUri(rule.id(), rule.title()));
                boatLintRule.setTitle(rule.title());
                boatLintRule.setSeverity(rule.severity());
                boatLintRule.setIgnored(false);
                boatLintRule.setRuleSet(ruleSet.getId());
                rules.put(rule.id(), boatLintRule);
            });
        return rules;
    }

    public List<BoatLintRule> getAvailableRules() {
        return new ArrayList<>(availableRules.values());
    }
}
