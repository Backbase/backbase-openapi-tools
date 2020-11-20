package com.backbase.oss.sonar.model;

import lombok.Data;

@Data
public class BoatTextRange {

    private String startLine;
    private String endLine;

    private String startColumn;
    private String endColumn;


}
