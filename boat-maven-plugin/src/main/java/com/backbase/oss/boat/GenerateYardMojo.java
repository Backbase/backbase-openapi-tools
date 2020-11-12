package com.backbase.oss.boat;

import com.backbase.oss.codegen.yard.BoatYardConfig;
import com.backbase.oss.codegen.yard.BoatYardGenerator;
import java.io.File;
import lombok.extern.slf4j.Slf4j;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

@Mojo(name = "yard", threadSafe = true)
@Slf4j
public class GenerateYardMojo extends AbstractMojo {

    @Parameter(name = "model", required = true)
    File model;

    @Parameter(name = "output", required = true, defaultValue = "${project.build.directory}/boat-yard")
    File output;

    @Parameter(name = "specsBase")
    File specsBaseDir;

    @Override
    public void execute() throws MojoExecutionException {
        log.info("Generating Boat YARD");
        BoatYardConfig config = new BoatYardConfig();
        config.setInputSpec(model.getAbsolutePath());
        config.setOutputDir(output.getAbsolutePath());
        config.setSpecsBaseDir(specsBaseDir);
        config.setTemplateDir("boat-yard");
        new BoatYardGenerator(config).generate();
    }
}
