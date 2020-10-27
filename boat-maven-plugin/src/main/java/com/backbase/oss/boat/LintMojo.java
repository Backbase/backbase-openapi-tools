package com.backbase.oss.boat;

import com.backbase.oss.boat.quay.BoatLinter;
import com.backbase.oss.boat.quay.configuration.RulesValidatorConfiguration;
import com.typesafe.config.Config;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.jetbrains.annotations.NotNull;
import org.zalando.zally.core.ApiValidator;
import org.zalando.zally.core.DefaultContextFactory;
import org.zalando.zally.core.Result;
import org.zalando.zally.core.RulesManager;

@Mojo(name = "lint", requiresDependencyResolution = ResolutionScope.RUNTIME, threadSafe = true)
@Slf4j
/*
  Lint Specification
 */
public class LintMojo extends AbstractMojo {

    @Parameter(name = "input", required = true)
    private File input;

    @Parameter(name = "failOnWarning", defaultValue = "false")
    private boolean failOnWarning;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        log.info("Linting OpenAPI: {}", input);

        File[] inputFiles;
        if (input.isDirectory()) {
            inputFiles = input.listFiles(pathname -> pathname.getName().endsWith(".yaml"));
            if (inputFiles == null) {
                throw new MojoExecutionException("No OpenAPI specs found in: " + input);
            }
            log.info("Found " + inputFiles.length + " specs to lint.");
            if (failOnWarning) {
                throw new MojoExecutionException("Linting fialed for input files: " + inputFiles);
            }
        } else {
            inputFiles = new File[]{input};
        }

        for (File inputFile : inputFiles) {
            lintOpenAPI(inputFile);
        }

    }

    private void lintOpenAPI(File inputFile) throws MojoExecutionException {
        try {
            BoatLinter boatLinter = getBoatLinter();
            String contents = IOUtils.toString(inputFile.toURI(), Charset.defaultCharset());
            List<Result> lint = boatLinter.lint(contents);
            if (!lint.isEmpty()) {
                log.warn("OpenAPI is not adhering to the rules!");
                lint.forEach(result -> {
                    log.warn("{}", result.toString());
                });
            } else {
                log.info("OpenAPI: {}, is valid!", inputFile);
            }

        } catch (IOException e) {
            throw new MojoExecutionException("Error transforming OpenAPI: " + inputFile, e);
        }
    }

    @NotNull
    private BoatLinter getBoatLinter() {
        RulesValidatorConfiguration rulesValidatorConfiguration = new RulesValidatorConfiguration();
        Config config = rulesValidatorConfiguration.config("boat.conf");
        RulesManager rulesManager = rulesValidatorConfiguration.rulesManager(config);
        ApiValidator apiValidator = rulesValidatorConfiguration.apiValidator(rulesManager, new DefaultContextFactory());
        return new BoatLinter(apiValidator);
    }

    public void setInput(File input) {
        this.input = input;
    }

    public void setFailOnWarning(boolean failOnWarning) {
        this.failOnWarning = failOnWarning;
    }
}
