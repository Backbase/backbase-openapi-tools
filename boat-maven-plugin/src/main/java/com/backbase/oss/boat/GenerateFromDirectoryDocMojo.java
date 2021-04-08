package com.backbase.oss.boat;

import lombok.extern.slf4j.Slf4j;
import org.apache.maven.plugin.MojoExecutionException;

import java.io.File;

/**
 * Allows generate::Doc to accept inputSpec as a directory
 * Output docs will be placed in separate folders for each spec
 */
@Slf4j
public class GenerateFromDirectoryDocMojo extends GenerateMojo {

  @Override
  public void execute() throws MojoExecutionException {

    File inputSpecFile = new File(inputSpec);

    if (inputSpecFile.isDirectory()){
      log.info("inputSpec is being read as a directory");

      File[] inputSpecs;
      File outPutDirectory = output;

      inputSpecs = inputSpecFile.listFiles(pathname -> pathname.getName().endsWith(".yaml"));

      if (inputSpecs == null || inputSpecs.length == 0) {
        throw new MojoExecutionException("No OpenAPI specs found in: " + inputSpec);
      }

      for(File f : inputSpecs){
        inputSpec = f.getPath();
        output = new File(outPutDirectory.getPath(),f.getName().substring(0,f.getName().lastIndexOf(".")).concat("-docs"));

        if(!output.exists()){
          output.mkdir();
        }

        log.info(" Generating docs for spec {} in directory", f.getName());
        super.execute();
      }

    }else {

      log.info("inputSpec being read as a single file");
      super.execute();

    }
  }
}
