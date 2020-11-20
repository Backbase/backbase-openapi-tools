package com.backbase.oss.sonar.model;

import lombok.Data;
import org.assertj.core.internal.bytebuddy.asm.Advice;
import org.sonar.api.batch.fs.TextRange;

@Data
public class BoatSonarLocation   {

    private String message;
    private String filePath;
    private BoatSonarLocationRange textRange;


    public BoatSonarLocation at(BoatSonarLocationRange mapLocation) {
        this.textRange = mapLocation;
        return this;
    }

    public BoatSonarLocation message(String description) {
       this.message = description;
       return this;
    }


    public BoatSonarLocation on(String inputPath) {
        this.filePath = inputPath;
        return this;
    }
}
