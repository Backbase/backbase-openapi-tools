package com.backbase.oss.boat;

import java.util.Collection;
import java.util.Set;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;

/**
 * Generating Server Stubs using Spring Boot.
 */
@Mojo(name = "generate-spring-boot-embedded", threadSafe = true)
public class GenerateSpringBootEmbeddedMojo extends AbstractGenerateMojo {

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        getLog().info("Generating Server Stubs using Spring Boot");
        execute("spring", "spring-boot", true, false, false);
    }

    @Override
    protected Collection<String> getGeneratorSpecificSupportingFiles() {
        return Set.of("BigDecimalCustomSerializer.java");
    }
}
