package com.backbase.oss.boat;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;

/**
 * Generating Server Stubs using Spring Boot.
 */
@Mojo(name = "generate-spring-boot-embedded", threadSafe = true)
public class GenerateSpringBootEmbeddedMojo extends AbstractGenerateMojo {

    @Override
    public void execute() throws MojoExecutionException {
        getLog().info("Generating Server Stubs using Spring Boot");
        execute("spring", "spring-boot", true, false, false);
    }

}
