package com.backbase.oss.boat.transformers;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.media.ComposedSchema;
import io.swagger.v3.oas.models.media.Schema;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

@Slf4j
@SuppressWarnings("java:S3740")
public class Decomposer implements Transformer {

    public void transform(OpenAPI openAPI, Map<String, Object> options) {

        List<Schema> composedSchemas = openAPI.getComponents().getSchemas().values().stream()
            .filter(schema -> schema instanceof ComposedSchema)
            .collect(Collectors.toList());

        composedSchemas.forEach(composedSchema -> mergeComposedSchema(openAPI, composedSchema));

        for (Schema composedSchema : composedSchemas) {
            ((ComposedSchema) composedSchema).setAllOf(null);
        }
    }

    private void mergeComposedSchema(OpenAPI openAPI, Schema composedSchema) {
        ((ComposedSchema) composedSchema).getAllOf().stream()
            .map(schemaReference -> {
                String key = StringUtils.substringAfterLast(schemaReference.get$ref(), "/");
                Schema schema = openAPI.getComponents().getSchemas().get(key);
                if (schema == null) {
                    log.info("huh??");
                }
                return schema;
            })
            .forEach(schema -> {
                log.debug("Merging properties from referenced Schema: {}", schema.getName());
                if (schema instanceof ComposedSchema)
                    mergeComposedSchema(openAPI, schema);

                mergeSchemas(composedSchema, schema);
            });
    }

    private void mergeSchemas(Schema composedSchema, Schema schema) {
        if (composedSchema.getProperties() == null) {
            composedSchema.setProperties(new LinkedHashMap<>());
        }
        if (composedSchema.getRequired() == null && schema.getRequired() != null) {
            composedSchema.required(new ArrayList<>());
        }
        if (schema.getRequired() != null) {
            List<String> required = new ArrayList(composedSchema.getRequired());
            required.addAll(schema.getRequired());
            required = required.stream().distinct().collect(Collectors.toList());
            composedSchema.required(required);
        }
        composedSchema.getProperties().putAll(schema.getProperties());
    }


}
