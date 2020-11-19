package com.backbase.oss.boat.quay.model;

import com.fasterxml.jackson.core.JsonPointer;
import kotlin.ranges.IntRange;
import lombok.Data;
import org.zalando.zally.rule.api.Severity;

@Data
public class BoatViolation {

    private BoatLintRule lintRule;
    private String description;
    private Severity severity;
    private IntRange lines;
    private JsonPointer pointer;


    public String displayString() {
        return "[" + lintRule.getId() + "] " + severity + " - " + lintRule.getTitle() + ": " + description;
    }

}
