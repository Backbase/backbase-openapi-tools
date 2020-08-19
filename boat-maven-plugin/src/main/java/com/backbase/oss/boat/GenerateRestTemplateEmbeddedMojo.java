package com.backbase.oss.boat;

import lombok.extern.slf4j.Slf4j;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;

@Mojo(name = "generate-rest-template-embedded", threadSafe = true)
@Slf4j
public class GenerateRestTemplateEmbeddedMojo extends AbstractGenerateMojo {

    @Override
    public void execute() throws MojoExecutionException {
        getLog().info("Generating Client using Spring Rest Template");
        execute("java", "resttemplate", true, false, false);
    }
}
