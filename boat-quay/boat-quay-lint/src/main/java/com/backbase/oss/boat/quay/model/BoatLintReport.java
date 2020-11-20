package com.backbase.oss.boat.quay.model;

import java.util.ArrayList;
import java.util.List;
import lombok.Data;

@Data
public class BoatLintReport {

    private String title;
    private String version;

    private String filePath;

    private String openApi;
    private List<BoatViolation> violations = new ArrayList<>();
    private List<BoatLintRule> availableRules = new ArrayList<>();

    public boolean hasViolations() {
        return !violations.isEmpty();
    }
}
