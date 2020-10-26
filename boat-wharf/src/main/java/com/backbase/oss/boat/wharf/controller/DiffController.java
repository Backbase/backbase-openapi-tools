package com.backbase.oss.boat.wharf.controller;

import com.backbase.oss.boat.diff.compare.OpenApiDiff;
import com.backbase.oss.boat.diff.model.ChangedOpenApi;
import com.fasterxml.jackson.core.JsonProcessingException;
import io.swagger.v3.parser.OpenAPIV3Parser;
import io.swagger.v3.parser.core.models.SwaggerParseResult;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/diff")
public class DiffController {

    private final OpenAPIV3Parser openAPIV3Parser = new OpenAPIV3Parser();

    private static final Logger log = LoggerFactory.getLogger(DiffController.class);

    @PostMapping("/compare")
    public Mono<ResponseEntity<ChangedOpenApi>> diff(Mono<String> oldVersion, Mono<String> newVersion) throws JsonProcessingException {
        return Mono.zip(
            oldVersion.map(openAPIV3Parser::readContents)
                .map(SwaggerParseResult::getOpenAPI)
                .doOnNext(spec -> log.info("Read oldVersion: {}", spec.getInfo().getTitle())),
            newVersion.map(openAPIV3Parser::readContents)
                .map(SwaggerParseResult::getOpenAPI)
                .doOnNext(spec -> log.info("Read oldVersion: {}", spec.getInfo().getTitle())))
            .map(specs -> OpenApiDiff.compare(specs.getT1(), specs.getT2()))
            .map(ResponseEntity::ok);
    }

}
