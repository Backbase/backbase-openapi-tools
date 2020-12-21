package com.backbase.oss.boat;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;

/**
 * Generating Client using Spring Rest Template.
 */
@Mojo(name = "generate-rest-template-embedded", threadSafe = true)
public class GenerateRestTemplateEmbeddedMojo extends AbstractGenerateMojo {

    @Override
    public void execute() throws MojoExecutionException {
        getLog().info("Generating Client using Spring Rest Template");
        execute("java", "resttemplate", true, false, true);
    }
}
