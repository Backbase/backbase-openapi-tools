package com.backbase.oss.boat.sonar;

import com.backbase.oss.boat.quay.model.BoatLintReport;
import com.backbase.oss.boat.quay.model.BoatLintRule;
import com.backbase.oss.boat.quay.model.BoatViolation;
import com.backbase.oss.boat.sonar.model.BoatSonarIssue;
import com.backbase.oss.boat.sonar.model.BoatSonarIssues;
import com.backbase.oss.boat.sonar.model.BoatSonarLocation;
import com.backbase.oss.boat.sonar.model.BoatSonarLocationRange;
import java.io.StringWriter;
import java.math.BigInteger;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;
import kotlin.ranges.IntRange;
import lombok.NonNull;
import lombok.SneakyThrows;

public class SonarReportConvertor {

    public static Coverage generateCoverage(BoatLintReport boatLintReport) {

        ObjectFactory objectFactory = new ObjectFactory();
        Coverage coverage = objectFactory.createCoverage();
        coverage.version  = BigInteger.ONE;
        coverage.file = Collections.singletonList(boatLintReport.getFilePath()).stream().map(path -> {
            Coverage.File coverageFile = objectFactory.createCoverageFile();
            coverageFile.path = path;
//            Coverage.File.LineToCover coverageFileLineToCover = objectFactory.createCoverageFileLineToCover();
//
//            coverageFileLineToCover.covered = true;
//            coverageFile.lineToCover = Collections.singletonList(coverageFileLineToCover);
            return coverageFile;
        }).collect(Collectors.toList());


        return coverage;
    }

    @SneakyThrows
    public static String generateCoverageXml(BoatLintReport boatLintReport) {


        //Create JAXB Context
        JAXBContext jaxbContext = JAXBContext.newInstance(Coverage.class);

        //Create Marshaller
        Marshaller jaxbMarshaller = jaxbContext.createMarshaller();

        //Required formatting??
        jaxbMarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);

        //Print XML String to Console
        StringWriter sw = new StringWriter();

        //Write XML to StringWriter
        Coverage o = generateCoverage(boatLintReport);
        jaxbMarshaller.marshal(o, sw);

        //Verify XML Content
        String xmlContent = sw.toString();
        return xmlContent;
    }

    public static BoatSonarIssues convert(BoatLintReport boatLintReport) {
        List<BoatSonarIssue> issues = boatLintReport.getViolations().stream()
            .map((BoatViolation boatViolation) -> convertToSonarIssue(boatViolation, boatLintReport))
            .collect(Collectors.toList());

        return new BoatSonarIssues(issues);
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

    private static String mapType(BoatLintRule.Type type) {
        return type.toString();
    }

    private static String mapSeverity(@NonNull org.zalando.zally.rule.api.Severity severity) {
        switch (severity) {
            case MUST: {
                return "BLOCKER";
            }
            case SHOULD: {
                return "CRITICAL";
            }
            case MAY: {
                return "MINOR";
            }
            case HINT: {
                return "INFO";
            }
            default:
                throw new IllegalArgumentException("invalid value: " + severity);
        }
    }

}
