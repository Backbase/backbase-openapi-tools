package com.backbase.oss.codegen.yard.model;

import com.backbase.oss.boat.quay.model.BoatLintRule;
import java.util.List;
import lombok.Data;

@Data
public class Portal {


    private String key;
    private String title;
    private String subTitle;
    private String navTitle;
    private String logoUrl;
    private String logoLink;
    private String version;

    private String content;

    private List<Capability> capabilities;

    private List<BoatLintRule> lintRules;

    private String defaultSpecUrl;

    private boolean enableLinting;


}
