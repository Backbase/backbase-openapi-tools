package com.backbase.oss.boat;

import lombok.Getter;
import lombok.Setter;


@Getter
@Setter
public class InputArtifact {
  private String groupId;
  private String artifactId;
  private String version;
  private String type;
  private String classifier;
  private String fileName;
  private boolean isProcessed;
  private boolean isUnzipped;
}
