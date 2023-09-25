package com.backbase.oss.boat;

import java.util.Collection;
import java.util.Set;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;

/**
 * Generating Client using Spring Rest Template.
 */
@Mojo(name = "generate-rest-template-embedded", threadSafe = true)
public class GenerateRestTemplateEmbeddedMojo extends AbstractGenerateMojo {

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        getLog().info("Generating Client using Spring Rest Template");
        execute("java", "resttemplate", true, false, true);
    }

    @Override
    protected Collection<String> getGeneratorSpecificSupportingFiles() {
        return Set.of("BigDecimalCustomSerializer.java");
    }
}
