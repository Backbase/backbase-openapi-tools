package com.backbase.oss.boat.quay;

import java.util.Arrays;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zalando.zally.core.ApiValidator;
import org.zalando.zally.core.Result;
import org.zalando.zally.core.RulesPolicy;

public class BoatLinter {

    private final Logger log = LoggerFactory.getLogger(BoatLinter.class);
    private final ApiValidator validator;

    public BoatLinter(ApiValidator validator) {
        this.validator = validator;
    }

    public List<Result> lint(String openApiContent) {
        RulesPolicy rulesPolicy = new RulesPolicy(Arrays.asList("219", "105","M008", " M009", " M010", " M011", " H001", " H002", " S005", " S006", " S007"));
        return validator.validate(openApiContent, rulesPolicy, null);
    }

}
