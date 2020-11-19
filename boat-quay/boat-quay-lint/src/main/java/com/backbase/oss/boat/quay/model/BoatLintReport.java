package com.backbase.oss.boat.quay.model;

import java.util.List;
import lombok.Data;

@Data
public class BoatLintReport {

    private String title;
    private String version;

    private String openApi;
    private List<BoatViolation> violations;
    private List<BoatLintRule> availableRules;
}
