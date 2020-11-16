package com.backbase.oss.boat.quay.configuration;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigParseOptions;
import com.typesafe.config.impl.ConfigImpl;
import com.typesafe.config.impl.Parseable;
import org.zalando.zally.core.ApiValidator;
import org.zalando.zally.core.CompositeRulesValidator;
import org.zalando.zally.core.ContextRulesValidator;
import org.zalando.zally.core.DefaultContextFactory;
import org.zalando.zally.core.JsonRulesValidator;
import org.zalando.zally.core.RulesManager;

public class RulesValidatorConfiguration {


    public Config config(String ruleSetFile) {
        return defaultReference(this.getClass().getClassLoader(), ruleSetFile)
            .withFallback(ConfigFactory.defaultReference());
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
