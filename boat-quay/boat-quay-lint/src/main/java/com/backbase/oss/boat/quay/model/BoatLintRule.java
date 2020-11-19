package com.backbase.oss.boat.quay.model;

import java.net.URI;
import lombok.Data;
import org.zalando.zally.rule.api.Severity;

@Data
public class BoatLintRule {

    private String id;
    private String ruleSet;
    private String title;
    private Severity severity;
    private boolean ignored;
    private URI url;

}
