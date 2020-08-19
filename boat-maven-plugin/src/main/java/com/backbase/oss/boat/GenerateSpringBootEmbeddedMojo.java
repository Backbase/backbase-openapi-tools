package com.backbase.oss.boat;

import lombok.extern.slf4j.Slf4j;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;

@Mojo(name = "generate-spring-boot-embedded", threadSafe = true)
@Slf4j
public class GenerateSpringBootEmbeddedMojo extends AbstractGenerateMojo {

    @Override
    public void execute() throws MojoExecutionException {
        getLog().info("Generating Server Stubs using Spring Boot");
        execute("spring", "spring-boot", true, false, false);
    }

}
