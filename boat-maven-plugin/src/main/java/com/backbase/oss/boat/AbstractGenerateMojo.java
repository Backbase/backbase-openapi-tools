package com.backbase.oss.boat;

import java.util.HashMap;
import java.util.Map;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;

public abstract class AbstractGenerateMojo extends GenerateMojo {

    public void execute(String generatorName, String library, boolean isEmbedded, boolean reactive, boolean generateSupportingFiles)
            throws MojoExecutionException, MojoFailureException {
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
        options.put("containerDefaultToNull", "true");

        this.generatorName = generatorName;
        this.generateSupportingFiles = generateSupportingFiles;
        this.generateApiTests = !isEmbedded;
        this.generateApiDocumentation = !isEmbedded;
        this.generateModelDocumentation = !isEmbedded;
        this.generateModelTests = !isEmbedded;
        this.skipOverwrite = true;
        this.configOptions = options;

        if(isEmbedded) {
            this.supportingFilesToGenerate = "ApiClient.java,BeanValidationException.java,RFC3339DateFormat.java,ServerConfiguration.java,ServerVariable.java,StringUtil.java,Authentication.java,HttpBasicAuth.java,HttpBearerAuth.java,ApiKeyAuth.java,JavaTimeFormatter.java";
        }
        super.execute();
    }
}

