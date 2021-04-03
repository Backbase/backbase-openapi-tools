package com.backbase.oss.boat;

import static java.util.Arrays.stream;
import static java.util.Collections.singletonMap;
import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

import com.backbase.oss.boat.loader.OpenAPILoader;
import com.backbase.oss.boat.loader.OpenAPILoaderException;
import com.backbase.oss.boat.serializer.SerializerUtils;
import com.backbase.oss.boat.transformers.Bundler;
import com.backbase.oss.boat.transformers.SetVersion;
import com.backbase.oss.boat.transformers.ExtensionFilter;
import io.swagger.v3.oas.models.OpenAPI;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;

@Mojo(name = "bundle", requiresDependencyResolution = ResolutionScope.RUNTIME, threadSafe = true)
@Slf4j
/**
  Bundles all references in the OpenAPI specification into one file.
 */
@Getter
@Setter
public class BundleMojo extends AbstractMojo {

    @Parameter(name = "input", required = true)
    private File input;

    @Parameter(name = "includes", required = false, defaultValue = "*.yaml")
    private String includes;

    @Parameter(name = "output", required = true)
    private File output;

    @Parameter(name = "version", required = false)
    private String version;

    @Parameter(name = "versionFileName", required = false)
    private boolean versionFileName = false;

    @Parameter(name = "removeExtensions", required = false, defaultValue = "")
    private List<String> removeExtensions;

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
            throw new MojoExecutionException("Both input and output need to be either a directory or a file.");
        }

        final File[] inputFiles;
        final File[] outputFiles;
        if (input.isDirectory()) {
            String[] inputs;

            try {
                inputs = Utils.selectInputs(input.toPath(), includes);
            } catch (IOException e) {
                throw new MojoExecutionException("Cannot scan input " + input, e);
            }

            inputFiles = stream(inputs)
                .map(file -> new File(input, file))
                .toArray(File[]::new);
            outputFiles = stream(inputs)
                .map(file -> new File(output, file))
                .toArray(File[]::new);

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

            if (isNotBlank(version)) {
                openAPI = new SetVersion(version)
                    .transform(openAPI);
            }

            openAPI = new Bundler(inputFile)
                .transform(openAPI);

            if (isNotEmpty(removeExtensions)) {
                openAPI = new ExtensionFilter()
                    .transform(openAPI, singletonMap("remove", removeExtensions));
            }

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
        String openApiVersion = openAPI.getInfo() != null ? openAPI.getInfo().getVersion() : null;
        if (openApiVersion == null) {
            throw new MojoExecutionException("Configured to use version in filename, but no version set.");
        }
        String majorFromFileName = originalFileName.replaceAll("^(.*api-v)([0-9]+)(\\.yaml$)", "$2");
        String majorFromVersion = openApiVersion.substring(0, openApiVersion.indexOf("."));
        if (!majorFromFileName.equals(majorFromVersion)) {
            throw new MojoExecutionException("Invalid version " + openApiVersion + " in file " + originalFileName);
        }
        // payment-order-client-api-v2.yaml
        return originalFileName.replaceAll("^(.*api-v)([0-9]+)(\\.yaml$)", "$1" + openApiVersion + "$3");
    }

}
