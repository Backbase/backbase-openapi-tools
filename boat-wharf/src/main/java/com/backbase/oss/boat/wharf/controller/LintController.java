package com.backbase.oss.boat.wharf.controller;

import com.backbase.oss.boat.diff.compare.OpenApiDiff;
import com.backbase.oss.boat.diff.model.ChangedOpenApi;
import com.fasterxml.jackson.core.JsonProcessingException;
import io.swagger.v3.parser.OpenAPIV3Parser;
import io.swagger.v3.parser.core.models.SwaggerParseResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/lint")
@Slf4j
public class LintController {

    private final OpenAPIV3Parser openAPIV3Parser = new OpenAPIV3Parser();

}
