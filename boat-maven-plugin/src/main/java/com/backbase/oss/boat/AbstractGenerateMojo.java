package com.backbase.oss.boat;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
public abstract class AbstractGenerateMojo extends GenerateMojo {

    private static final Collection<String> EMBEDDED_SUPPORTING_FILES = List.of(
        "ApiClient.java", "BeanValidationException.java", "RFC3339DateFormat.java", "ServerConfiguration.java",
        "ServerVariable.java", "StringUtil.java", "Authentication.java", "HttpBasicAuth.java", "HttpBearerAuth.java",
        "ApiKeyAuth.java", "JavaTimeFormatter.java"
    );

    public void execute(String generatorName, String library, boolean isEmbedded, boolean reactive,
        boolean generateSupportingFiles) throws MojoExecutionException, MojoFailureException {

        Map<String, String> options = new HashMap<>();
        options.put("library", library);
        options.put("java8", "true");
        options.put("dateLibrary", "java8");
        options.put("reactive", Boolean.toString(reactive));
        options.put("performBeanValidation", "true");
        options.put("skipDefaultInterface", "true");
        options.put("interfaceOnly", "true");
        options.put("useTags", "true");
        options.put("useBeanValidation", "true");
        options.put("useClassLevelBeanValidation", "false");
        options.put("useOptional", "false");
        options.put("useJakartaEe", "true");
        options.put("useSpringBoot3", "true");
        options.put("containerDefaultToNull", "false");

        this.generatorName = generatorName;
        this.generateSupportingFiles = generateSupportingFiles;
        this.generateApiTests = !isEmbedded;
        this.generateApiDocumentation = !isEmbedded;
        this.generateModelDocumentation = !isEmbedded;
        this.generateModelTests = !isEmbedded;
        this.skipOverwrite = true;
        if (this.configOptions == null) {
            this.configOptions = options;
        } else {
            this.configOptions = mergeOptions(options, this.configOptions);
        }
        log.debug("Using configOptions={}", this.configOptions);

        Stream<String> supportingFiles = Optional.ofNullable(getGeneratorSpecificSupportingFiles())
            .filter(CollectionUtils::isNotEmpty)
            .map(Collection::stream)
            .orElse(Stream.empty());

        if (isEmbedded) {
            supportingFiles = Stream.concat(supportingFiles, EMBEDDED_SUPPORTING_FILES.stream());
        }

        this.supportingFilesToGenerate = supportingFiles
            .map(String::trim)
            .filter(StringUtils::isNotEmpty)
            .distinct()
            .sorted()
            .collect(Collectors.joining(","));

        super.execute();
    }

    protected Collection<String> getGeneratorSpecificSupportingFiles() {
        return Collections.emptySet();
    }

    private static Map<?, ?> mergeOptions(Map<String, String> defaultOptions, Map<?, ?> overrides) {
        var merged = new HashMap<>();
        merged.putAll(defaultOptions);
        merged.putAll(overrides);
        return merged;
    }
}

