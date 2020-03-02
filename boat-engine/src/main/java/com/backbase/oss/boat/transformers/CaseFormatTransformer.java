package com.backbase.oss.boat.transformers;

import com.backbase.oss.boat.Exporter;
import com.backbase.oss.boat.serializer.SerializerUtils;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.CaseFormat;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.parameters.QueryParameter;
import io.swagger.v3.oas.models.parameters.RequestBody;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CaseFormatTransformer {
    private static final Logger log = LoggerFactory.getLogger(Exporter.class);

    public static String transformSnakeCaseToCamelCase(OpenAPI openAPI) {
        return transform(openAPI, CaseFormat.LOWER_UNDERSCORE, CaseFormat.LOWER_CAMEL, new HashMap<>());
    }


    public static String transformCamelCaseToSnakeCase(OpenAPI openAPI) {
        Map<String, String> overrides = new HashMap<>();
        overrides.put("u_r_i", "uri");
        overrides.put("u_r_l", "url");
        overrides.put("i_b_a_n", "iban");
        overrides.put("b_b_a_n", "bban");
        overrides.put("b_i_c", "bic");
        return transform(openAPI, CaseFormat.LOWER_UNDERSCORE, CaseFormat.LOWER_CAMEL, new HashMap<>());
    }

    public static String transform(OpenAPI openAPI, CaseFormat caseFormatFrom, CaseFormat caseFormatTo, Map<String, String> overrides) {

        openAPI.getPaths().forEach((s, pathItem) -> {

            pathItem.readOperations().forEach(operation -> {

                List<Parameter> filtered = operation.getParameters().stream()
                    .peek(parameter -> {
                        if (parameter instanceof QueryParameter) {
                            parameter.setName(transform(parameter.getName(), caseFormatFrom, caseFormatTo, overrides));
                        }
                    }).collect(Collectors.toList());

                operation.setParameters(filtered);

                RequestBody requestBody = operation.getRequestBody();
                if (requestBody != null && requestBody.getContent() != null) {
                    Collection<MediaType> values = new ArrayList<>(requestBody.getContent().values());
                    for (MediaType value : values) {
                        transform(value.getSchema(), caseFormatFrom, caseFormatTo, overrides);

                        if (value.getExample() != null && value.getExample() instanceof String) {
                            value.setExample(snakeExample(value.getExample().toString(), caseFormatFrom, caseFormatTo, overrides));
                        }
                    }
                }

                operation.getResponses().forEach((s1, apiResponse) -> {
                    if (apiResponse.getContent() != null) {
                        Collection<MediaType> values = new ArrayList<>(apiResponse.getContent().values());
                        for (MediaType value : values) {

                            if (value.getSchema() != null) {
                                transform(value.getSchema(), caseFormatFrom, caseFormatTo, overrides);
                                if (value.getExample() != null && value.getExample() instanceof String) {
                                    value.setExample(snakeExample(value.getExample().toString(), caseFormatFrom, caseFormatTo, overrides));
                                }
                            }
                        }
                    }
                });
            });
        });


        openAPI.getComponents().getSchemas().forEach(
            (s, schema) -> transform(schema, caseFormatFrom, caseFormatTo, overrides)
        );

        return SerializerUtils.toYamlString(openAPI);
    }

    private static void transform(Schema schema, CaseFormat from, CaseFormat to, Map<String, String> overrides) {
        if (schema.getProperties() != null) {
            log.debug("Processing schema: {}", schema.getName());
            Map<String, Schema> properties = new LinkedHashMap<>();
            schema.getProperties().forEach((propertyNameObject, propertySchemaObject) -> {
                String propertyName = (String) propertyNameObject;
                Schema propertySchema = (Schema) propertySchemaObject;
                String newName = transform(propertyName, from, to, overrides);
                log.debug("Renaming property: {} to {}", propertyName, newName);
                properties.put(newName, propertySchema);
                transform(propertySchema, from, to, overrides);
            });
            schema.setProperties(properties);
        }

        if (schema.getExample() != null && schema.getExample() instanceof String) {
            String fixedExample = snakeExample(schema.getExample().toString(), from, to, overrides);
            schema.setExample(fixedExample);
        }
    }

    static String transform(String name, CaseFormat from, CaseFormat to, Map<String, String> overrides) {
        String result = from.to(to, name);
        for (Map.Entry<String, String> entry : overrides.entrySet()) {
            String fromKeyword = entry.getKey();
            String toKeyword = entry.getValue();
            result = result.replaceAll(fromKeyword, toKeyword);
        }
        return result;
    }

    private static ObjectMapper objectMapper = new ObjectMapper();

    private static String snakeExample(String example, CaseFormat from, CaseFormat to, Map<String, String> overrides) {
        try {
            JsonNode jsonNode = objectMapper.readTree(example);
            transform(jsonNode, from, to, overrides);
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(jsonNode);
        } catch (IOException e) {
            log.debug("not valid json. nothing to rename");
        }
        return example;
    }

    private static void transform(JsonNode json, CaseFormat from, CaseFormat to, Map<String, String> overrides) {
        if (json.isObject()) {
            ObjectNode objectNode = (ObjectNode) json;
            Set<String> toBeRemoved = new HashSet<>();
            Map<String, JsonNode> renamedFields = new LinkedHashMap<>();

            objectNode.fields().forEachRemaining(field -> {
                String newName = transform(field.getKey(), from, to, overrides);
//                objectNode.set(newName, field.getValue());

                if (field.getValue().isObject()) {
                    transform(((ObjectNode) field.getValue()), from, to, overrides);
                }
                if (field.getValue().isArray()) {
                    ((ArrayNode) field.getValue()).forEach(jsonNode -> {
                        transform(jsonNode, from, to, overrides);
                    });
                }
                renamedFields.put(newName, field.getValue());
                toBeRemoved.add(field.getKey());
            });
            objectNode.remove(toBeRemoved);
            objectNode.setAll(renamedFields);
        }

        if (json.isArray()) {
            json.forEach(jsonNode -> {
                transform(jsonNode, from, to, overrides);
            });
        }
    }

}
