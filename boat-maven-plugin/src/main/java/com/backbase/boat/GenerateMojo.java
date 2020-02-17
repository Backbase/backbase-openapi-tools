package com.backbase.boat;

import java.io.File;
import java.util.Optional;
import org.apache.commons.lang3.StringUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

@Mojo(name = "generate", threadSafe = true)
public class GenerateMojo extends AbstractRamlToOpenApi {

    /**
     * Source directory to scan for raml files. Use location relative to the project.baseDir.
     * Default is 'src/main/resources'.
     */
    @Parameter(property = "input", defaultValue = "src/main/resources")
    private File input;

    /**
     * Target directory for generated code. Use location relative to the project.baseDir.
     * Default value is "target/openapi".
     */
    @Parameter(property = "output", defaultValue = "target/openapi")
    private File output;

    /**
     * Fails on errors by default, but can be set to ignore.
     */
    @Parameter(property = "failOnError", defaultValue = "true")
    private boolean failOnError = true;

    @Override
    public void execute() throws MojoExecutionException {
        if (!input.exists()) {
            String msg = "Input does not exist: " + input.getAbsolutePath();
            getLog().error(msg);
            if (failOnError) {
                throw new MojoExecutionException(msg);
            }
        }

        File[] files = input.listFiles(this::isRamlSpec);
        if (files == null) {
            String msg = "Failed to find raml files in " + input.getAbsolutePath();
            getLog().error(msg);
            if (failOnError) {
                throw new MojoExecutionException(msg);
            } else {
                // Avoid the NPE
                return;
            }
        }

        for (File file : files) {
            String ramlName = StringUtils.substringBeforeLast(file.getName(), ".");
            try {
                File outputDirectory = new File(output, ramlName);
                String name = project.getArtifactId() + ":" + project.getVersion() + ":" + ramlName;
                export(name, Optional.empty(), file, outputDirectory);
                getLog().info("Exported RAML Spec: " + ramlName);
            } catch (Throwable e) {
                failed.put(ramlName, e.getMessage());
                String msg = "Failed to export RAML Spec: " + file.getName()
                    + " due to: [" + e.getClass() + "] " + e.getMessage();
                getLog().error(msg);
                if (failOnError) {
                    throw new MojoExecutionException(msg, e);
                }
            }
        }

        writeSummary("Converted RAML Specs to OpenAPI Summary");

        if (!success.isEmpty()) {
            writeSwaggerUrls(output);
        }
    }

    public File getOutput() {
        return output;
    }

    public void setOutput(File output) {
        this.output = output;
    }

    public File getInput() {
        return input;
    }

    public void setInput(File input) {
        this.input = input;
    }

    public boolean isFailOnError() {
        return failOnError;
    }

    public void setFailOnError(boolean failOnError) {
        this.failOnError = failOnError;
    }

}
