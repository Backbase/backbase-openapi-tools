package com.backbase.oss.boat.quay;

import com.fasterxml.jackson.core.JsonPointer;
import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import kotlin.ranges.IntRange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zalando.zally.core.ApiValidator;
import org.zalando.zally.core.Result;
import org.zalando.zally.core.RulesPolicy;
import org.zalando.zally.rule.api.Severity;

public class BoatLinter {

    private final Logger log = LoggerFactory.getLogger(BoatLinter.class);
    private final ApiValidator validator;

    private URI documentationBaseUrl = URI.create("https://backbase.github.io/backbase-openapi-tools/rules.md");

    public BoatLinter(ApiValidator validator) {
        this.validator = validator;
    }

    public List<Result> lint(String openApiContent) {
        RulesPolicy rulesPolicy = new RulesPolicy(Arrays.asList("219", "105","M008", " M009", " M010", " M011", " H001", " H002", " S005", " S006", " S007"));
        List<Result> validate = validator.validate(openApiContent, rulesPolicy, null);

        return validate.stream().map(this::transformResult).collect(Collectors.toList());
    }

    private Result transformResult(Result result) {

        String id = result.getId();
        String title = result.getTitle();
        String description = result.getDescription();
        Severity violationType = result.getViolationType();
        JsonPointer pointer = result.getPointer();
        IntRange lines = result.getLines();

        String heading= id + ":" + title;
        String ref = heading.toLowerCase().replaceAll("[^a-z0-9]+","-");

        URI resolve = documentationBaseUrl.resolve("#" + ref);

        return new Result(id, resolve, title, description, violationType, pointer, lines);

    }

}
