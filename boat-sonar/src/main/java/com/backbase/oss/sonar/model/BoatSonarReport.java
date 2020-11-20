package com.backbase.oss.sonar.model;

import java.util.List;
import lombok.Data;

@Data
public class BoatSonarReport {
    
    private List<BoatSonarIssue> issues;

    public BoatSonarReport(List<BoatSonarIssue> issues) {
        this.issues = issues;
    }
}
