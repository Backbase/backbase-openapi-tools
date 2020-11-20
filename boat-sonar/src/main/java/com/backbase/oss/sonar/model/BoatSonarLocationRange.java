package com.backbase.oss.sonar.model;

import lombok.Data;

@Data
public class BoatSonarLocationRange {

    private int startLine;
    private int endLine;
    private int startColumn;
    private int endColumn;

    
}
