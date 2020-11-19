package com.backbase.oss.codegen.yard.model;

import java.util.List;
import lombok.Data;

@Data
public class YardModel {

    private List<Portal> portals;

    private boolean enableLinting;
    private List<LintRule> globalLntRules;

}
