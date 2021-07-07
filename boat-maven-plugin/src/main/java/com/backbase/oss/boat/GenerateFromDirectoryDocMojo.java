package com.backbase.oss.boat;

import lombok.extern.slf4j.Slf4j;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Parameter;
import org.codehaus.plexus.util.DirectoryScanner;

import java.io.File;
import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * Allows generate::Doc to accept inputSpec as a directory
 * Output docs will be placed in separate folders for each spec
 */
@Slf4j
public class GenerateFromDirectoryDocMojo extends GenerateMojo {

  @Parameter(defaultValue = "**/*-api-*.yaml")
  protected String openApiFileFilters;


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
        throw new MojoExecutionException("No OpenAPI specs found in: " + inputSpec);
      }

      for (File f : inputSpecs) {
        inputSpec = f.getPath();
        output = new File(outPutDirectory.getPath(), f.getName().substring(0, f.getName().lastIndexOf(".")));

        if (!output.exists()) {
          output.mkdir();
        }

        log.info(" Generating docs for spec {} in directory", f.getName());
        try {
          super.execute();
        } catch (MojoExecutionException | MojoFailureException e) {
          log.error("Failed to generate doc for spec: {}", inputSpec);
        }
      }

    } else {

      log.info("inputSpec being read as a single file");
      super.execute();

    }
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
