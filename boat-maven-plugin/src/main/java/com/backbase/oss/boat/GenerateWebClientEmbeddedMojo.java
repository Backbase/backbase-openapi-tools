package com.backbase.oss.boat;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;

@Mojo(name = "generate-webclient-embedded", threadSafe = true)
public class GenerateWebClientEmbeddedMojo extends AbstractGenerateMojo {

    @Override
    public void execute() throws MojoExecutionException {
        getLog().info("Generating Server Stubs using Web Client Boot");
        execute("java", "webclient", true, true, true);
    }

}
