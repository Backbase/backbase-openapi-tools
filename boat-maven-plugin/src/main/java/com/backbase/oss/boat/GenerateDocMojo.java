package com.backbase.oss.boat;

import lombok.extern.slf4j.Slf4j;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;

@Mojo(name = "doc", threadSafe = true)
@Slf4j
public class GenerateDocMojo extends GenerateFromDirectoryDocMojo {

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        getLog().info("Generating Boat Docs");
        if (generatorName == null) {
            generatorName = "boat-docs";
        }
        super.execute();
    }
}
