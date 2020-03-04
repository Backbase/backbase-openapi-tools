package com.backbase.oss.boat.transformers;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.media.ObjectSchema;
import java.util.HashMap;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class AdditionalPropertiesAdder implements Transformer {

    private final List<String> schemaNames;

    public AdditionalPropertiesAdder(List<String> schemaNames) {
        this.schemaNames = schemaNames;
    }

    @Override
    public void transform(OpenAPI openAPI, HashMap<String, Object> options) {
        openAPI.getComponents().getSchemas().values().stream()
            .filter(schema -> schemaNames.contains(schema.getName()))
            .forEach(schema -> {
                log.info("Adding property: \"additions\" to Schema: {}", schema.getName());
                ObjectSchema propertiesItem = new ObjectSchema();
                propertiesItem.setAdditionalProperties(true);
                schema.addProperties("additions", propertiesItem);
            });
    }
}
