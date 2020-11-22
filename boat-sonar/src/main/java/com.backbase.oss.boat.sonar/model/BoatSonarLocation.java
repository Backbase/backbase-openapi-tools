package com.backbase.oss.boat.sonar.model;

import lombok.Data;

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
