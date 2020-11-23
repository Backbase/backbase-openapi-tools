package com.backbase.oss.codegen.yard.model;

import com.backbase.oss.boat.quay.model.BoatLintRule;
import java.util.List;
import lombok.Data;

@Data
public class YardModel {

    private List<Portal> portals;

    private boolean enableLinting;
    private List<BoatLintRule> globalLntRules;

}
