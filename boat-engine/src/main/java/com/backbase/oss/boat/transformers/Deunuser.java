package com.backbase.oss.boat.transformers;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.media.ArraySchema;
import io.swagger.v3.oas.models.media.ComposedSchema;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;

import java.util.HashMap;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

@Slf4j
public class Deunuser implements Transformer {

    public void transform(OpenAPI openAPI, HashMap<String, Object> options) {

        Set<Schema> requestSchemas = openAPI.getPaths().values().stream()
            .flatMap(pathItem -> pathItem.readOperations().stream())
            .filter(operation -> operation.getRequestBody() != null && operation.getRequestBody().getContent() != null)
            .map(operation -> operation.getRequestBody().getContent())
            .flatMap(content -> content.values().stream())
            .map(MediaType::getSchema)
            .filter(Objects::nonNull)
//                .flatMap(schema -> resolveSchemas(schema, openAPI.getComponents()))
            .collect(Collectors.toSet());

        Set<Schema> responseSchemas = openAPI.getPaths().values().stream()
            .flatMap(pathItem -> pathItem.readOperations().stream())
            .filter(operation -> operation.getRequestBody() != null && operation.getRequestBody().getContent() != null)
            .flatMap(operation -> operation.getResponses().values().stream())
            .filter(apiResponse -> !apiResponse.getContent().isEmpty())
            .flatMap(apiResponse -> apiResponse.getContent().values().stream())
            .map(MediaType::getSchema)
            .filter(Objects::nonNull)
//                .flatMap(schema -> resolveSchemas(schema, openAPI.getComponents()))
            .collect(Collectors.toSet());

        log.debug("Request Schemas: {}", requestSchemas.stream().map(schema -> schema.getName()).collect(Collectors.toList()));
        log.debug("Response Schemas: {}", responseSchemas.stream().map(schema -> schema.getName()).collect(Collectors.toList()));


    }

    private static Stream<Schema> resolveSchemas(Schema schema, Components components) {
        String $ref = schema.get$ref();
        if ($ref != null)
            return Stream.of(getSchemaByRef($ref, components));
        if (schema instanceof ArraySchema)
            return resolveSchemas(((ArraySchema) schema).getItems(), components);
        if (schema instanceof ComposedSchema)
            return ((ComposedSchema) schema).getAllOf().stream().flatMap(allOfRef -> resolveSchemas(getSchemaByRef($ref, components), components));

        return Stream.of(schema);
    }

    private static Schema getSchemaByRef(String $ref, Components components) {
        return components.getSchemas().get(StringUtils.substringAfterLast($ref, "/"));
    }

}
