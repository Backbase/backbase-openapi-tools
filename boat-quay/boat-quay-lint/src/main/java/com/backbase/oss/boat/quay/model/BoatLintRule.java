package com.backbase.oss.boat.quay.model;

import java.net.URI;
import lombok.Data;
import org.zalando.zally.rule.api.Severity;

@Data
public class BoatLintRule {

    public enum Type {
        BUG, CODE_SMELL, VULNERABILITY, SECURITY_HOTSPOT
    }
    private String id;
    private String ruleSet;
    private String title;
    private Severity severity;
    private boolean ignored;
    private URI url;

    private Long effortMinutes;
    private Type type;

}
