package com.backbase.oss.boat;

import java.util.Collection;
import java.util.Set;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;

/**
 * Generating Server Stubs using Web Client Boot.
 */
@Mojo(name = "generate-webclient-embedded", threadSafe = true)
public class GenerateWebClientEmbeddedMojo extends AbstractGenerateMojo {

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        getLog().info("Generating Server Stubs using Web Client Boot");
        execute("java", "webclient", true, true, true);
    }

    @Override
    protected Collection<String> getGeneratorSpecificSupportingFiles() {
        return Set.of("BigDecimalCustomSerializer.java");
    }
}
