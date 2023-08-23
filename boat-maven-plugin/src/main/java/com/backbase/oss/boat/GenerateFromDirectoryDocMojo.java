package com.backbase.oss.boat;

import lombok.extern.slf4j.Slf4j;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Parameter;
import org.codehaus.plexus.util.DirectoryScanner;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Allows generate::Doc to accept inputSpec as a directory
 * Output docs will be placed in separate folders for each spec
 */
@Slf4j
public class GenerateFromDirectoryDocMojo extends GenerateMojo {

    @Parameter(defaultValue = "**/*-api-*.yaml")
    protected String openApiFileFilters;

    @Parameter(property = "markersDirectory", defaultValue = "${project.build.directory}/boat-markers")
    protected File markersDirectory;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        if (inputSpec != null) {
            File inputSpecFile = new File(inputSpec);
            fileInputExecute(inputSpecFile);
        } else {
            log.info("Input read as Artifact");
            super.execute();
        }
    }

    private void fileInputExecute(File inputSpecFile) throws MojoExecutionException, MojoFailureException {

        if (inputSpecFile.isDirectory()) {
            log.info("inputSpec is being read as a directory");

            File[] inputSpecs;
            File outPutDirectory = output;

            inputSpecs = findAllOpenApiSpecs(inputSpecFile);

            if (inputSpecs.length == 0) {
                log.warn("No OpenAPI specs found in: " + inputSpec);
            } else {

                List<File> success = new ArrayList<>();
                List<File> failed = new ArrayList<>();
                for (File inputSpec : inputSpecs) {
                    executeInputFile(outPutDirectory, success, failed, inputSpec);
                }
                writeMarkers(success, failed);
            }
        } else {
            log.info("inputSpec being read as a single file");
            super.execute();
        }
    }

    private void writeMarkers(List<File> success, List<File> failed) throws MojoExecutionException {
        if (markersDirectory != null) {
            try {
                if (!markersDirectory.exists()) {
                    Files.createDirectory(markersDirectory.toPath());
                }

                Files.write(new File(markersDirectory, "success.lst").toPath(),
                        listOfFilesToString(success).getBytes(StandardCharsets.UTF_8),
                        StandardOpenOption.CREATE,
                        StandardOpenOption.APPEND);
                Files.write(new File(markersDirectory, "failed.lst").toPath(),
                        listOfFilesToString(failed).getBytes(StandardCharsets.UTF_8),
                        StandardOpenOption.CREATE,
                        StandardOpenOption.APPEND);

            } catch (IOException | InvalidPathException e) {
                log.error("Failed to write BOAT markers to: {}", markersDirectory, e);
                throw new MojoExecutionException("Failed to write BOAT markers", e);
            }
        }
    }

    private void executeInputFile(File outPutDirectory, List<File> success, List<File> failed, File f) {
        inputSpec = f.getPath();
        output = new File(outPutDirectory.getPath(), f.getName().substring(0, f.getName().lastIndexOf(".")));

        if (!output.exists()) {
            try {
                Files.createDirectory(output.toPath());
            } catch (IOException e) {
                log.error("Failed to create output directory", e);
            }
        }

        log.info(" Generating docs for spec {} in directory", f.getName());
        try {
            super.execute();
            success.add(f);
        } catch (MojoExecutionException | MojoFailureException e) {
            log.error("Failed to generate doc for spec: {}", inputSpec);
            failed.add(f);
        }
    }

    private String listOfFilesToString(List<File> files) {
        return files.stream()
                .map(File::getPath)
                .collect(Collectors.joining("\n"));
    }

    private File[] findAllOpenApiSpecs(File specDirectory) {
        DirectoryScanner directoryScanner = new DirectoryScanner();
        directoryScanner.setBasedir(specDirectory);
        directoryScanner.setIncludes(openApiFileFilters.replace(" ", "").split(","));
        directoryScanner.scan();

        String[] includedFiles = directoryScanner.getIncludedFiles();
        return Arrays.stream(includedFiles).map(pathname -> new File(specDirectory, pathname))
                .collect(Collectors.toList()).toArray(new File[]{});
    }
}
