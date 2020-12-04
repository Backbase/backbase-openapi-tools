package com.backbase.oss.boat.transformers;

import com.backbase.oss.boat.Utils;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.examples.Example;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.parameters.RequestBody;
import io.swagger.v3.oas.models.responses.ApiResponse;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class Normaliser implements Transformer {


    @Override
    public OpenAPI transform(OpenAPI openAPI, Map<String, Object> options) {

        openAPI.getPaths().forEach((s, pathItem) ->
            pathItem.readOperations().forEach(operation -> {
                    RequestBody requestBody = operation.getRequestBody();
                    normalizeRequest(s, requestBody);
                    operation.getResponses().forEach(this::normalizeResponse);
                }
            ));
        openAPI.getComponents().getRequestBodies().forEach(this::normalizeRequest);
        openAPI.getComponents().getResponses().forEach(this::normalizeResponse);

        return openAPI;
    }

    private void normalizeResponse(String s, ApiResponse apiResponse) {
        if (apiResponse != null && apiResponse.getContent() != null) {
            log.debug("Normalizing Response Examples in: {}", s);
            apiResponse.getContent().forEach((key, value) -> normalizeExampleNames(value));
        }
    }

    private void normalizeRequest(String s, RequestBody requestBody) {
        if (requestBody != null && requestBody.getContent() != null) {
            log.debug("Normalizing Request Examples in: {}", s);
            requestBody.getContent().forEach((key, value) -> normalizeExampleNames(value));
        }
    }

    public void normalizeExampleNames(MediaType mediaType) {
        Map<String, Example> examples = mediaType.getExamples();
        if (mediaType.getExamples() != null) {
            Map<String, Example> normalizedExamples = new LinkedHashMap<>();
            examples.forEach((name, example) -> {

                String normalizedName = Utils.normalizeSchemaName(name);
                log.debug("Normalizing name: {} to: {}", name, normalizedName);
                normalizedExamples.put(normalizedName, example);
            });
            mediaType.setExamples(normalizedExamples);
        }
    }
}
