package com.backbase.oss.boat;

import com.backbase.oss.boat.loader.OpenAPILoader;
import com.backbase.oss.boat.loader.OpenAPILoaderException;
import com.backbase.oss.boat.serializer.SerializerUtils;
import com.backbase.oss.boat.transformers.Bundler;
import io.swagger.v3.oas.models.OpenAPI;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Collections;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;

@Mojo(name = "bundle", requiresDependencyResolution = ResolutionScope.RUNTIME, threadSafe = true)
@Slf4j
/*
  Bundles all references in the OpenAPI specification into one file.
 */
public class BundleMojo extends AbstractMojo {

    @Parameter(name = "input", required = true)
    private File input;

    @Parameter(name = "output", required = true)
    private File output;

    @Parameter(name = "versionFileName", required = false)
    private boolean versionFileName = true;

    /**
     * Skip the execution.
     */
    @Parameter(name = "skip", property = "bundle.skip", defaultValue = "false", alias = "codegen.skip")
    private boolean skip;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        if (skip) {
            getLog().info("Skipping OpenAPI bundle.");

            return;
        }

        log.info("Bundling OpenAPI: {} to: {}", input, output);

        if (input.isDirectory() && output.getName().endsWith(".yaml")) {
            throw new MojoFailureException("Both input and output need to be either a directory or a file.");
        }

        File[] inputFiles;
        File[] outputFiles;
        if (input.isDirectory()) {
            inputFiles = input.listFiles(new FileFilter() {
                @Override
                public boolean accept(File pathname) {
                    return pathname.getName().endsWith(".yaml");
                }
            });
            outputFiles = new File[inputFiles.length];
            for (int i = 0; i < inputFiles.length; i++) {
                outputFiles[i] = new File(output, inputFiles[i].getName());
            }
            log.info("Found " + inputFiles.length + " specs to bundle.");
        } else {
            inputFiles = new File[] {input};
            outputFiles = new File[] {output};
        }

        for (int i = 0; i < inputFiles.length; i++) {
            bundleOpenAPI(inputFiles[i], outputFiles[i]);
        }

    }

    private void bundleOpenAPI(File inputFile, File outputFile) throws MojoExecutionException {
        try {
            OpenAPI openAPI = OpenAPILoader.load(inputFile);
            new Bundler(inputFile).transform(openAPI, Collections.emptyMap());

            File directory = outputFile.getParentFile();
            if (!directory.exists()) {
                directory.mkdirs();
            }
            if (versionFileName) {
                String versionedFileName = versionFileName(outputFile.getAbsolutePath(), openAPI);
                outputFile = Paths.get(versionedFileName).toFile();

            }
            Files.write(outputFile.toPath(), SerializerUtils.toYamlString(openAPI).getBytes(), StandardOpenOption.CREATE);
        } catch (OpenAPILoaderException | IOException e) {
            throw new MojoExecutionException("Error transforming OpenAPI: {}" + inputFile, e);
        }
    }

    String versionFileName(String originalFileName, OpenAPI openAPI) throws MojoExecutionException {
        String version = openAPI.getInfo() != null ? openAPI.getInfo().getVersion() : null;
        if (version == null) {
            throw new MojoExecutionException("Configured to use version in filename, but no version set.");
        }
        String majorFromFileName = originalFileName.replaceAll("^(.*api-v)([0-9]+)(\\.yaml$)", "$2");
        String majorFromVersion = version.substring(0, version.indexOf("."));
        if (!majorFromFileName.equals(majorFromVersion)) {
            throw new MojoExecutionException("Invalid version " + version + " in file " + originalFileName);
        }
        // payment-order-client-api-v2.yaml
        return originalFileName.replaceAll("^(.*api-v)([0-9]+)(\\.yaml$)", "$1" + version + "$3");
    }

}
