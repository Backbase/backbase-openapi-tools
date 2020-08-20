package com.backbase.oss.boat.transformers;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.parameters.RequestBody;

import java.util.*;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@SuppressWarnings("java:S3740")
public class Deprecator implements Transformer {

    @Override
    public void transform(OpenAPI openAPI, Map<String, Object> options) {

        openAPI.getPaths().forEach((s, pathItem) -> {

            if (isGetDeprecated(pathItem)) {
                pathItem.setGet(null);
            }
            if (pathItem.getDelete() != null && pathItem.getDelete().getDeprecated() != null && pathItem.getDelete().getDeprecated().booleanValue()) {
                pathItem.setDelete(null);
            }
            if (pathItem.getPost() != null && pathItem.getPost().getDeprecated() != null && pathItem.getPost().getDeprecated().booleanValue()) {
                pathItem.setPost(null);
            }
            if (pathItem.getPut() != null && pathItem.getPut().getDeprecated() != null && pathItem.getPut().getDeprecated().booleanValue()) {
                pathItem.setPut(null);
            }
            if (pathItem.getPatch() != null && pathItem.getPatch().getDeprecated() != null && pathItem.getPatch().getDeprecated().booleanValue()) {
                pathItem.setPatch(null);
            }


            pathItem.readOperations().forEach(operation -> {
                if (operation.getParameters() != null) {
                    operation.setParameters(operation.getParameters().stream()
                        .filter(parameter -> !Boolean.TRUE.equals(parameter.getDeprecated()))
                        .collect(Collectors.toList()));

                }
                RequestBody requestBody = operation.getRequestBody();
                if (requestBody != null && requestBody.getContent() != null) {
                    Content content = requestBody.getContent();
                    removeDeprecatedContent(content);
                }

                operation.getResponses().forEach((s1, apiResponse) -> {
                    if (apiResponse.getContent() != null) {
                        Content content = apiResponse.getContent();
                        removeDeprecatedContent(content);
                    }
                });
            });


        });

        openAPI.getComponents().getSchemas().values().stream().forEach(Deprecator::removeDeprecatedProperties);
    }

    private boolean isGetDeprecated(PathItem pathItem) {
        return pathItem.getGet() != null && pathItem.getGet().getDeprecated() != null && pathItem.getGet()
            .getDeprecated();
    }

    private static void removeDeprecatedContent(Content content) {
        Collection<MediaType> values = new ArrayList<>(content.values());
        for (MediaType value : values) {
            removeDeprecatedProperties(value.getSchema());
        }
    }

    private static void removeDeprecatedProperties(Schema schema) {
        if (schema == null) {
            log.warn("Some weird stuff is going on...");
            return;
        }

        if (schema.getProperties() != null) {
            Map<String, Schema> properties = new LinkedHashMap<>();

            schema.getProperties().forEach((propertyNameObject, propertySchemaObject) -> {
                String propertyName = (String) propertyNameObject;
                Schema propertySchema = (Schema) propertySchemaObject;

                if (Boolean.TRUE.equals(propertySchema.getDeprecated())) {
                    log.debug("Property: {} is deprecated", propertyName);
                } else {
                    properties.put(propertyName, propertySchema);
                    removeDeprecatedProperties(propertySchema);
                }
            });
            schema.setProperties(properties);
        }
    }


}
