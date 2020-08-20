package com.backbase.oss.boat;

import lombok.extern.slf4j.Slf4j;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;

@Mojo(name = "generate-webclient-embedded", threadSafe = true)
@Slf4j
public class GenerateWebClientEmbeddedMojo extends AbstractGenerateMojo {

    @Override
    public void execute() throws MojoExecutionException {
        getLog().info("Generating Server Stubs using Web Client Boot");
        execute("java", "webclient", true, true, true);
    }

}
