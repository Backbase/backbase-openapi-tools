package com.backbase.oss.boat;

import java.io.File;
import org.apache.commons.lang3.StringUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

/**
 * Converts a RAML spec to an OpenAPI spec.
 */
@Mojo(name = "export", threadSafe = true)
public class ExportMojo extends AbstractRamlToOpenApi {

    /**
     * Source directory to scan for raml files. Use location relative to the project.baseDir. Default is
     * 'src/main/resources'.
     */
    @Parameter(property = "input", defaultValue = "src/main/resources")
    protected File input;


    /**
     * Explicit RAML File location
     */
    @Parameter(property = "inputFile")
    protected File inputFile;

    /**
     * Fails on errors by default, but can be set to ignore.
     */
    @Parameter(property = "failOnError", defaultValue = "true")
    private boolean failOnError = true;

    @Override
    public void execute() throws MojoExecutionException {
        File[] files;
        if (inputFile != null) {
            getLog().info("Converting RAML Input File: " + inputFile);
            files = new File[]{inputFile};
        } else {
            getLog().info("Converting RAML specs from Input Directory: " + input);
            files = getFilesFromInputDirectoru();
            if(files == null)
                return;
        }

        for (File file : files) {
            String ramlName = StringUtils.substringBeforeLast(file.getName(), ".");
            try {
                File outputDirectory = new File(output, ramlName);
                export(null, file, outputDirectory);
                getLog().info("Exported RAML Spec: " + ramlName);
            } catch (Exception e) {
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
    }


    private File[] getFilesFromInputDirectoru() throws MojoExecutionException {
        if (!input.exists()) {
            String msg = "Input does not exist: " + input.getAbsolutePath();
            getLog().error(msg);
            if (failOnError) {
                throw new MojoExecutionException(msg);
            }
        }

        File[] files = input.listFiles(this::isRamlSpec);
        if (files == null || files.length == 0) {
            String msg = "Failed to find raml files in " + input.getAbsolutePath();
            getLog().error(msg);
            if (failOnError) {
                throw new MojoExecutionException(msg);
            }
            return new File[0];
        }
        return files;
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
