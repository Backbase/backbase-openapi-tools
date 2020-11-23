package com.backbase.oss.boat.quay.model;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;

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

    public String getFileName() {
        return StringUtils.substringBeforeLast(StringUtils.substringAfterLast(filePath, File.separator), ".");
    }
}
