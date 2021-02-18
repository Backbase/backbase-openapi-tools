package com.backbase.oss.boat;

import lombok.extern.slf4j.Slf4j;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;

@Mojo(name = "doc", threadSafe = true)
@Slf4j
public class GenerateDocMojo  extends GenerateFromDirectoryDocMojo  {

    @Override
    public void execute() throws MojoExecutionException {
        getLog().info("Generating Boat Docs");
        generatorName = "boat-docs";
        super.execute();
    }
}
