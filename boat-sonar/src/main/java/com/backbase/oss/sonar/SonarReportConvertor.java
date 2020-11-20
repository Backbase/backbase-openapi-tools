package com.backbase.oss.sonar;

import com.backbase.oss.boat.quay.model.BoatLintReport;
import com.backbase.oss.boat.quay.model.BoatLintRule;
import com.backbase.oss.boat.quay.model.BoatViolation;
import com.backbase.oss.sonar.model.BoatSonarIssue;
import com.backbase.oss.sonar.model.BoatSonarLocation;
import com.backbase.oss.sonar.model.BoatSonarLocationRange;
import com.backbase.oss.sonar.model.BoatSonarReport;
import java.util.List;
import java.util.stream.Collectors;
import kotlin.ranges.IntRange;
import lombok.NonNull;
import lombok.experimental.UtilityClass;
import org.sonar.api.batch.rule.Severity;
import org.sonar.api.rules.RuleType;

@UtilityClass
public class SonarReportConvertor {

    public static BoatSonarReport convert(BoatLintReport boatLintReport) {
        List<BoatSonarIssue> issues = boatLintReport.getViolations().stream()
            .map((BoatViolation boatViolation) -> convertToSonarIssue(boatViolation, boatLintReport))
            .collect(Collectors.toList());

        return new BoatSonarReport(issues);
    }

    private static BoatSonarIssue convertToSonarIssue(BoatViolation boatViolation, BoatLintReport lintReport) {
        BoatLintRule rule = boatViolation.getRule();

        BoatSonarIssue issue = new BoatSonarIssue();
        issue.setEngineId("boat");
        issue.setRuleId(rule.getId());
        issue.setSeverity(mapSeverity(boatViolation.getSeverity()));
        issue.setType(mapType(boatViolation.getRule().getType()));
        issue.setEffortMinutes(boatViolation.getRule().getEffortMinutes());
        issue.setPrimaryLocation(mapLocation(boatViolation, lintReport));
        return issue;
    }

    private static BoatSonarLocation mapLocation(BoatViolation violation, BoatLintReport lintReport) {
        return new BoatSonarLocation()
            .at(mapLocation(violation.getLines()))
            .message(violation.getDescription())
            .on(lintReport.getFilePath());
    }

    private static BoatSonarLocationRange mapLocation(IntRange lines) {
        BoatSonarLocationRange range = new BoatSonarLocationRange();
        range.setStartLine(lines.getStart());
        range.setStartColumn(0);
        range.setEndLine(lines.getLast());
        range.setStartColumn(0);
        return range;

    }

    private static RuleType mapType(BoatLintRule.Type type) {
        return RuleType.valueOf(type.name());
    }

    private static Severity mapSeverity(@NonNull org.zalando.zally.rule.api.Severity severity) {
        switch (severity) {
            case MUST: {
                return Severity.BLOCKER;
            }
            case SHOULD: {
                return Severity.CRITICAL;
            }
            case MAY: {
                return Severity.MINOR;
            }
            case HINT: {
                return Severity.INFO;
            }
            default:
                throw new IllegalArgumentException("invalid value: " + severity);
        }
    }

}
