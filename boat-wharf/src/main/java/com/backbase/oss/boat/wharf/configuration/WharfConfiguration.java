package com.backbase.oss.boat.wharf.configuration;

import java.time.LocalDate;
import org.springframework.context.annotation.Bean;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.context.request.async.DeferredResult;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.builders.ResponseBuilder;
import springfox.documentation.schema.ScalarType;
import springfox.documentation.service.ParameterType;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;

public class WharfConfiguration {

    @Bean
    public Docket petApi() {
        return new Docket(DocumentationType.OAS_30)
            .select()
            .apis(RequestHandlerSelectors.basePackage("com.backbase"))
            .paths(PathSelectors.any())
            .build()
            .pathMapping("/")
            .directModelSubstitute(LocalDate.class, String.class)
            .genericModelSubstitutes(ResponseEntity.class)
            .useDefaultResponseMessages(false)
            .enableUrlTemplating(true);
    }

}
