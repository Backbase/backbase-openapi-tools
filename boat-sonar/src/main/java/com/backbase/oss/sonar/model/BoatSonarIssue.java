package com.backbase.oss.sonar.model;

import java.util.List;
import lombok.Data;

@Data
public class BoatSonarIssue {

    private String engineId;
    private String ruleId;
    private BoatSonarLocation primaryLocation;
    private String type;
    private String severity;

    private Long effortMinutes;
    private List<BoatSonarLocation> secondaryLocations;

}
