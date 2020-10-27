package com.backbase.oss.boat.quay.configuration;

import com.backbase.oss.boat.quay.BoatLinter;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigParseOptions;
import com.typesafe.config.impl.ConfigImpl;
import com.typesafe.config.impl.Parseable;
import java.util.Arrays;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zalando.zally.core.ApiValidator;
import org.zalando.zally.core.CompositeRulesValidator;
import org.zalando.zally.core.ContextRulesValidator;
import org.zalando.zally.core.DefaultContextFactory;
import org.zalando.zally.core.JsonRulesValidator;
import org.zalando.zally.core.RuleProcessor;
import org.zalando.zally.core.RulesManager;
import org.zalando.zally.rule.api.Rule;

public class RulesValidatorConfiguration {

    private final Logger log = LoggerFactory.getLogger(BoatLinter.class);

    public static ApiValidator defaultApiValidator() {
        RulesValidatorConfiguration rulesValidatorConfiguration = new RulesValidatorConfiguration();
        Config config = rulesValidatorConfiguration.config("boat.conf");
        RulesManager rulesManager = rulesValidatorConfiguration.rulesManager(config);
        ApiValidator apiValidator = rulesValidatorConfiguration.apiValidator(rulesManager, new DefaultContextFactory());
        return apiValidator;
    }

    public void scanAnnotations() {
        RuleProcessor ruleProcessor = new RuleProcessor();
        Set<String> supportedAnnotationTypes = ruleProcessor.getSupportedAnnotationTypes();

//        RoundEnvironment renv = new JavacRoundEnvironment(true,



        log.info("Scanned for Rules in Classpath: {}", supportedAnnotationTypes);
    }

    public Config config(String ruleSetFile) {
        return defaultReference(this.getClass().getClassLoader(), ruleSetFile);
    }

    public static Config defaultReference(final ClassLoader loader, String file) {
        return ConfigImpl.computeCachedConfig(loader, "boatReference", () -> {
            Config unresolvedResources = Parseable
                .newResources(file,
                    ConfigParseOptions.defaults().setClassLoader(loader))
                .parse().toConfig();
            return ConfigImpl.systemPropertiesAsConfig().withFallback(unresolvedResources).resolve();
        });
    }

    public RulesManager rulesManager(Config config) {
        return RulesManager.Companion.fromClassLoader(config);
    }

    public ApiValidator apiValidator(RulesManager rulesManager, DefaultContextFactory defaultContextFactory) {
        return new CompositeRulesValidator(new ContextRulesValidator(rulesManager, defaultContextFactory), new JsonRulesValidator(rulesManager));
    }

}
