package com.backbase.oss.sonar.model;

import java.util.List;
import lombok.Data;
import org.sonar.api.batch.rule.Severity;
import org.sonar.api.batch.sensor.issue.IssueLocation;
import org.sonar.api.rules.RuleType;

@Data
public class BoatSonarIssue {

    private String engineId;
    private String ruleId;
    private BoatSonarLocation primaryLocation;
    private RuleType type;
    private Severity severity;

    private Long effortMinutes;
    private List<BoatSonarLocation> secondaryLocations;

}
