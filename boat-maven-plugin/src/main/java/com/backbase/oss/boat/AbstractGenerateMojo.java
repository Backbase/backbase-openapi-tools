package com.backbase.oss.boat;

import java.util.HashMap;
import java.util.Map;
import org.apache.maven.plugin.MojoExecutionException;

public abstract class AbstractGenerateMojo extends GenerateMojo {

    public void execute(String generatorName, String library, boolean isEmbedded, boolean reactive, boolean generateSupportingFiles)
        throws MojoExecutionException {
        Map<String, String> options = new HashMap<>();
        options.put("library", library);
        options.put("java8", "true");
        options.put("dateLibrary", "java8");
        options.put("reactive", Boolean.valueOf(reactive).toString());
        options.put("performBeanValidation", "true");
        options.put("skipDefaultInterface", "true");
        options.put("interfaceOnly", "true");

        this.generatorName = generatorName;
        this.generateSupportingFiles = generateSupportingFiles;
        this.generateApiTests = !isEmbedded;
        this.generateApiDocumentation = !isEmbedded;
        this.generateModelDocumentation = !isEmbedded;
        this.generateModelTests = !isEmbedded;
        this.skipOverwrite = true;
        this.configOptions = options;
        super.execute();
    }
}

