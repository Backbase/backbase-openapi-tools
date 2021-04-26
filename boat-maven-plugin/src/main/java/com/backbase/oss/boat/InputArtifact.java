package com.backbase.oss.boat;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;


@Getter
@Setter
@Slf4j
public class InputArtifact {
  private String groupId;
  private String artifactId;
  private String version;
  private String type;
  private String classifier;
  private String fileName;
  private boolean overWrite;
  private boolean overWriteIfNewer;
  private boolean needsProcessing;

  public boolean isNeedsProcessing(File destFile, File originFile){
    needsProcessing = overWrite || !destFile.exists() || (overWriteIfNewer && isNewer(destFile,originFile));
    return needsProcessing;
  }

  private boolean isNewer( File destFile, File originFile )  {
    try {
      long destMod = Files.getLastModifiedTime( destFile.toPath() ).toMillis();
      long originMod = Files.getLastModifiedTime( originFile.toPath() ).toMillis();
      return  originMod > destMod;
    } catch (IOException e) {
      log.debug("Assuming artifact was not modified since artifact was last downloaded, cannot last read modified time");
      return false;
    }

  }
}
