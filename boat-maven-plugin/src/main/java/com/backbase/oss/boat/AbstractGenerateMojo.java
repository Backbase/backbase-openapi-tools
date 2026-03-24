package com.backbase.oss.boat;

import lombok.extern.slf4j.Slf4j;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.openapitools.codegen.languages.SpringCodegen;

@Slf4j
public abstract class AbstractGenerateMojo extends GenerateMojo {

    private static final Collection<String> EMBEDDED_SUPPORTING_FILES = List.of(
        "ApiClient.java", "BeanValidationException.java", "RFC3339DateFormat.java", "ServerConfiguration.java",
        "ServerVariable.java", "StringUtil.java", "Authentication.java", "HttpBasicAuth.java", "HttpBearerAuth.java",
        "ApiKeyAuth.java", "JavaTimeFormatter.java"
    );
    private static final String FALSE = "false";
    private static final String TRUE = "true";

    public void execute(String generatorName, String library, boolean isEmbedded, boolean reactive,
        boolean generateSupportingFiles) throws MojoExecutionException, MojoFailureException {

        Map<String, String> options = new HashMap<>();
        options.put("library", library);
        options.put("java8", TRUE);
        options.put("dateLibrary", "java8");
        options.put("reactive", Boolean.toString(reactive));
        options.put("performBeanValidation", TRUE);
        options.put("skipDefaultInterface", TRUE);
        options.put("interfaceOnly", TRUE);
        options.put("useTags", TRUE);
        options.put("useBeanValidation", TRUE);
        options.put("useOptional", FALSE);
        options.put("useJakartaEe", TRUE);
        options.put("useSpringBoot3", TRUE);
        options.put("containerDefaultToNull", FALSE);

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

        if (isEmbedded) {
            this.supportingFilesToGenerate = uniqueJoin(EMBEDDED_SUPPORTING_FILES);
        }

        super.execute();
    }

    private static Map<?, ?> mergeOptions(Map<String, String> defaultOptions, Map<?, ?> overrides) {
        var merged = new HashMap<>();
        merged.putAll(defaultOptions);
        merged.putAll(overrides);
        boolean sb3 = propertyToBool(merged.get(SpringCodegen.USE_SPRING_BOOT3));
        boolean sb4 = propertyToBool(merged.get(SpringCodegen.USE_SPRING_BOOT4));
        if (sb3 && sb4) {
            merged.put(SpringCodegen.USE_SPRING_BOOT3, FALSE);
        }
        return merged;
    }

    private static boolean propertyToBool(Object value) {
        if (value instanceof Boolean) {
            return (Boolean) value;
        } else if (value instanceof String) {
            return Boolean.parseBoolean((String) value);
        }
        return false;
    }
}

