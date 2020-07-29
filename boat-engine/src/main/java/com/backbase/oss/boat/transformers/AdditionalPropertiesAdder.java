package com.backbase.oss.boat.transformers;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.media.ObjectSchema;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;

@Slf4j
@RequiredArgsConstructor
public class AdditionalPropertiesAdder implements Transformer {

    private final List<String> schemaNames;
    private final String additionsType;

    @Override
    public void transform(OpenAPI openAPI, Map<String, Object> options) {
        openAPI.getComponents().getSchemas().entrySet().stream()
            .filter(stringSchemaEntry -> schemaNames.contains(stringSchemaEntry.getKey()))
            .forEach(stringSchemaEntry -> {
                log.info("Adding property: \"additions\" to Schema: {}", stringSchemaEntry.getKey());
                ObjectSchema additionsTypeSchema = new ObjectSchema();
                additionsTypeSchema.setType(additionsType);
                ObjectSchema propertiesItem = new ObjectSchema();
                propertiesItem.setType("object");
                propertiesItem.setAdditionalProperties(additionsTypeSchema);
                stringSchemaEntry.getValue().addProperties("additions", propertiesItem);
            });
    }
}
