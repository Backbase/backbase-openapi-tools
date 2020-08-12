package com.backbase.oss.boat;

import java.util.HashMap;
import java.util.Map;
import org.apache.maven.plugin.MojoExecutionException;

public abstract class AbstractGenerateMojo extends GenerateMojo {

    public void execute() throws MojoExecutionException {
        generatorName = "java";
        generateSupportingFiles = isGenerateSupportingFiles();
        generateApiTests = !isEmbedded();
        generateApiDocumentation = !isEmbedded();
        generateModelDocumentation = !isEmbedded();
        generateModelTests = !isEmbedded();
        skipOverwrite = true;
        Map<String, String> options = new HashMap<>();
        options.put("library", getLibrary());
        options.put("java8", "true");
        options.put("dateLibrary", "java8");
        options.put("reactive", isReactive().toString());
        configOptions = options;
        super.execute();
    }

    protected abstract boolean isGenerateSupportingFiles();

    protected abstract String getLibrary();

    protected abstract boolean isEmbedded();

    protected abstract Boolean isReactive();
}
