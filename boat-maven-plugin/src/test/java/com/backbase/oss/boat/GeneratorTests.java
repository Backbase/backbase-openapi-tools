package com.backbase.oss.boat;

import java.io.File;
import lombok.extern.slf4j.Slf4j;
import org.apache.maven.DefaultArtifactFilterManager;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.logging.console.ConsoleLogger;
import org.junit.Test;
import org.sonatype.plexus.build.incremental.DefaultBuildContext;

@Slf4j
public class GeneratorTests {


    @Test
    public void testHTML2() throws MojoExecutionException {
        CodeGenMojo mojo = new CodeGenMojo();

        String inputFile = System.getenv("input");
        if(inputFile == null) {
            log.info("skipping testing as input system property is not specified");
            return ;
        }

        File input = new File(inputFile);

        File output = new File("target");
        if(!output.exists())
            output.mkdirs();

        DefaultBuildContext defaultBuildContext = new DefaultBuildContext();
        defaultBuildContext.enableLogging(new ConsoleLogger());

        mojo.getLog();
        mojo.buildContext = defaultBuildContext;
        mojo.project = new MavenProject();
        mojo.generatorName = "html2";
        mojo.inputSpec = input.getAbsolutePath();
        mojo.output = output;
        mojo.skip = false;
        mojo.skipIfSpecIsUnchanged = false;
        mojo.execute();

    }
}
