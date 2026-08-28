package com.backbase.oss.boat.quay;

import com.backbase.oss.boat.loader.OpenAPILoader;
import com.backbase.oss.boat.loader.OpenAPILoaderException;
import com.backbase.oss.boat.quay.configuration.RulesValidatorConfiguration;
import com.backbase.oss.boat.quay.model.BoatLintReport;
import com.backbase.oss.boat.quay.model.BoatLintRule;
import com.backbase.oss.boat.quay.model.BoatViolation;
import com.backbase.oss.boat.serializer.SerializerUtils;
import com.typesafe.config.Config;
import io.swagger.v3.oas.models.OpenAPI;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.jetbrains.annotations.NotNull;
import org.zalando.zally.core.ApiValidator;
import org.zalando.zally.core.DefaultContextFactory;
import org.zalando.zally.core.Result;
import org.zalando.zally.core.RulesManager;
import org.zalando.zally.core.RulesPolicy;
import org.zalando.zally.rule.api.Rule;
import org.zalando.zally.rule.api.RuleSet;

@Slf4j
public class BoatLinter {

    /**
     * Rules that cannot be evaluated against an OpenAPI 3.1 document, and whose violations are therefore
     * dropped for such documents.
     *
     * <p>Zally's {@code 219} ({@code UseOpenApiRule}) validates every OpenAPI 3 document against the OAS
     * <b>3.0</b> JSON schema, and only one schema can be configured at a time. On a 3.1 document every
     * violation it reports is a false positive.
     */
    private static final Set<String> RULES_WITHOUT_OPENAPI_31_SUPPORT = Collections.singleton("219");

    private final ApiValidator validator;

    private final URI documentationBaseUrl = URI.create("https://backbase.github.io/backbase-openapi-tools/rules.md");
    private final RulesManager rulesManager;
    private final RulesPolicy rulesPolicy;
    private final Map<String, BoatLintRule> availableRules;
    private final Config config;

    public BoatLinter(String... ignoreRules) {
        RulesValidatorConfiguration rulesValidatorConfiguration = new RulesValidatorConfiguration();
        this.config = rulesValidatorConfiguration.config("boat.conf");
        this.rulesManager = rulesValidatorConfiguration.rulesManager(config);
        this.rulesPolicy = new RulesPolicy(Arrays.asList(ignoreRules));
        this.validator = rulesValidatorConfiguration.apiValidator(rulesManager, new DefaultContextFactory());
        this.availableRules = mapAvailableRules();
    }

    public BoatLintReport lint(File inputFile) throws IOException, OpenAPILoaderException {
        Path relativePath = getFilePath(inputFile);
        log.info("Linting: {}", inputFile);
        String contents = IOUtils.toString(inputFile.toURI(), Charset.defaultCharset());
        BoatLintReport boatLintReport = lint(contents);
        boatLintReport.setFilePath(relativePath.toString());
        return boatLintReport;
    }

    @NotNull
    private Path getFilePath(File inputFile) {
        File workingDirectory = new File(".");

        if (inputFile.isAbsolute()) {
            workingDirectory = workingDirectory.getAbsoluteFile();
        }

        Path relativize;
        try {
            relativize = workingDirectory.toPath().relativize(inputFile.toPath());
        } catch (RuntimeException exception) {
            log.warn("Failed to get relative path for: {} in working directory: {}", inputFile, workingDirectory);
            return inputFile.toPath();
        }
        return relativize;
    }

    public BoatLintReport lint(String openApiContent) throws OpenAPILoaderException {
        // OpenAPILoader.parse throws rather than returning null; stating that here keeps the document
        // non-null for the null-tolerant SerializerUtils calls below.
        OpenAPI openAPI = Objects.requireNonNull(OpenAPILoader.parse(openApiContent));
        boolean openApi31 = SerializerUtils.isOpenApi31(openAPI);

        List<Result> validate = validator.validate(openApiContent, rulesPolicy, null);
        List<BoatViolation> violations = validate.stream()
            .filter(result -> !(openApi31 && RULES_WITHOUT_OPENAPI_31_SUPPORT.contains(result.getId())))
            .map(this::transformResult)
            .collect(Collectors.toList());

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
        violation.setRule(availableRules.get(result.getId()));
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

        Config extraRuleAnnotations = config.getConfig("ExtraRuleAnnotations");
        Config defaultConfig = extraRuleAnnotations.getConfig("default");
        rulesManager.rules(rulesPolicy)
            .forEach(ruleDetails -> {
                long effortInMinutes = getEffortInMinutes(extraRuleAnnotations, defaultConfig, ruleDetails);
                BoatLintRule.Type type = getType(extraRuleAnnotations, defaultConfig, ruleDetails);
                BoatLintRule boatLintRule = new BoatLintRule();
                Rule rule = ruleDetails.getRule();
                RuleSet ruleSet = ruleDetails.getRuleSet();
                boatLintRule.setId(rule.id());
                boatLintRule.setUrl(getUri(rule.id(), rule.title()));
                boatLintRule.setTitle(rule.title());
                boatLintRule.setSeverity(rule.severity());
                boatLintRule.setIgnored(false);
                boatLintRule.setRuleSet(ruleSet.getId());
                boatLintRule.setType(type);
                boatLintRule.setEffortMinutes(effortInMinutes);
                rules.put(rule.id(), boatLintRule);
            });
        return rules;
    }

    private BoatLintRule.Type getType(Config extraRuleAnnotations, Config defaultConfig, org.zalando.zally.core.RuleDetails ruleDetails) {
        String defaultPath = ruleDetails.getRule().severity() + ".type";
        BoatLintRule.Type defaultType = defaultConfig.getEnum(BoatLintRule.Type.class, defaultPath);
        String path = "rules." + ruleDetails.getRule().id() + ".type";
        return extraRuleAnnotations.hasPath(path)
            ? extraRuleAnnotations.getEnum(BoatLintRule.Type.class, path)
            : defaultType;
    }

    private long getEffortInMinutes(Config extraRuleAnnotations, Config defaultConfig, org.zalando.zally.core.RuleDetails ruleDetails) {
        long defaultEffortMinutes = defaultConfig.getLong(ruleDetails.getRule().severity() + ".effortMinutes");
        String path = "rules." + ruleDetails.getRule().id() + ".effortMinutes";
        return extraRuleAnnotations.hasPath(path)
            ? extraRuleAnnotations.getLong(path)
            : defaultEffortMinutes;
    }

    public List<BoatLintRule> getAvailableRules() {
        return new ArrayList<>(availableRules.values());
    }


}
