package com.backbase.oss.codegen.yard.model;

import java.util.List;
import lombok.Data;
import org.zalando.zally.core.Result;

@Data
public class LintRuleReport {

    private String title;
    private String version;

    private String openApi;
    private List<Result> results;
}
