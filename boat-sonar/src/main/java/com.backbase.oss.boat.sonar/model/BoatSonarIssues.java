package com.backbase.oss.boat.sonar.model;

import java.util.List;
import lombok.Data;

@Data
public class BoatSonarIssues {
    
    private List<BoatSonarIssue> issues;

    public BoatSonarIssues(List<BoatSonarIssue> issues) {
        this.issues = issues;
    }
}
