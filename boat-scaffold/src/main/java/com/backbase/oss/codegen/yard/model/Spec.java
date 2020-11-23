package com.backbase.oss.codegen.yard.model;

import io.swagger.v3.oas.models.OpenAPI;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.Data;
import org.zalando.zally.core.Result;

@Data
public class Spec {

    private String key;
    private String title;
    private String openApiUrl;
    private String boatDocUrl;

    private String openAPI;
    private Map<String, OpenAPI> transformedOpenAPIs = new LinkedHashMap<>();

    private List<Result> results;

    public void addOpenAPI(String transformerKey, OpenAPI openAPI) {
        transformedOpenAPIs.put(transformerKey, openAPI);
    }
}
