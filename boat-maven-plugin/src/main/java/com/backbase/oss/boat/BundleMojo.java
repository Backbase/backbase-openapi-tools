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

import com.google.common.annotations.VisibleForTesting;
import io.swagger.v3.oas.models.OpenAPI;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import io.swagger.v3.oas.models.info.Info;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.codehaus.plexus.util.DirectoryScanner;

@Mojo(name = "bundle", requiresDependencyResolution = ResolutionScope.RUNTIME, threadSafe = true)
@Slf4j
/**
  Bundles all references in the OpenAPI specification into one file.
 */
@Getter
@Setter
public class BundleMojo extends AbstractMojo {

    @Parameter(name = "input", required = true, defaultValue = "${project.basedir}/src/main/resources")
    private File input;

    @Parameter(name = "includes", defaultValue = "**/openapi.yaml, **/*api*.yaml")
    protected String[] includes;

    @Parameter(name = "output", required = true, defaultValue = "${project.build.directory}/openapi")
    private File output;

    @Parameter(name = "flattenOutput", required = false)
    private boolean flattenOutput = false;

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
            DirectoryScanner directoryScanner = new DirectoryScanner();
            directoryScanner.setBasedir(input);
            directoryScanner.setIncludes(includes);
            directoryScanner.scan();

            String[] includedFiles = directoryScanner.getIncludedFiles();
            inputFiles = stream(includedFiles)
                .map(file -> new File(input, file))
                .collect(Collectors.toList())
                .toArray(File[]::new);
            outputFiles = stream(includedFiles)
                .map(this::normalizeOutputFileName)
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
            log.info("Bundling {} into a single OpenAPI file: {}", inputFile, outputFile);
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

            Files.write(outputFile.toPath(), SerializerUtils.toYamlString(openAPI).getBytes(StandardCharsets.UTF_8), StandardOpenOption.CREATE);
            log.info("Bundled: {} into: {}", inputFile, outputFile);
        } catch (OpenAPILoaderException | IOException e) {
            throw new MojoExecutionException("Error transforming OpenAPI: {}" + inputFile, e);
        }
    }

    @VisibleForTesting
    String versionFileName(String originalFileName, OpenAPI openAPI) throws MojoExecutionException {
        String openApiVersion = Optional.ofNullable(openAPI.getInfo())
            .map(Info::getVersion)
            .orElseThrow(() -> new MojoExecutionException("Configured to use version in filename, but no version set."));

        if (!openApiVersion.matches("^\\d\\..*")) {
            throw new MojoExecutionException(
                "Version should be semver (or at least have a recognisable major version), but found '" + openApiVersion
                    + "' (string starts with number and dot: 2.0.0, 2.blabla, 2.3.4.5.6.234234)");
        }
        String majorFromVersion = openApiVersion.substring(0, openApiVersion.indexOf("."));
        String majorFromFileName = originalFileName.replaceAll("^(.*api-v)([0-9]+)(\\.yaml$)", "$2");
        if (!majorFromFileName.equals(majorFromVersion)) {
            throw new MojoExecutionException("Invalid version " + openApiVersion + " in file " + originalFileName);
        }
        // payment-order-client-api-v2.yaml
        return originalFileName.replaceAll("^(.*api-v)([0-9]+)(\\.yaml$)", "$1" + openApiVersion + "$3");
    }

    @VisibleForTesting
    String normalizeOutputFileName(String outputFileName) {
        return flattenOutput ? new File(outputFileName).getName() : outputFileName;
    }
}
