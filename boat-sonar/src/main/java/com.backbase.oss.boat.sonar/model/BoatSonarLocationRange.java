package com.backbase.oss.boat.sonar.model;

import lombok.Data;

@Data
public class BoatSonarLocationRange {

    private int startLine;
    private int endLine;
    private int startColumn;
    private int endColumn;

    
}
