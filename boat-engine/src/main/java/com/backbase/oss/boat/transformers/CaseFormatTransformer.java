package com.backbase.oss.boat.transformers;

import com.backbase.oss.boat.serializer.SerializerUtils;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.CaseFormat;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.parameters.QueryParameter;
import io.swagger.v3.oas.models.parameters.RequestBody;
import io.swagger.v3.oas.models.responses.ApiResponse;
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
import lombok.extern.slf4j.Slf4j;

@Slf4j
@SuppressWarnings("java:S3740")
public class CaseFormatTransformer implements Transformer {

    @Override
    public OpenAPI transform(OpenAPI openAPI, Map<String, Object> options) {
        CaseFormat caseFormatFrom = null;
        CaseFormat caseFormatTo = null;
        Map<String, String> overrides = new HashMap<>();
        if (options != null) {
            caseFormatFrom = (CaseFormat) options.get("caseFormatFrom");
            caseFormatTo = (CaseFormat) options.get("caseFormatTo");
            overrides = (Map<String, String>) options.get("overrides");

        }
        if (caseFormatFrom == null && caseFormatTo == null) {
            transformCamelCaseToSnakeCase(openAPI);
        } else {
            if (overrides == null) {
                overrides = new HashMap<>();
            }
            transform(openAPI, caseFormatFrom, caseFormatTo, overrides);
        }

        return openAPI;
    }

    public String transformCamelCaseToSnakeCase(OpenAPI openAPI) {
        Map<String, String> overrides = new HashMap<>();
        overrides.put("u_r_i", "uri");
        overrides.put("u_r_l", "url");
        overrides.put("i_b_a_n", "iban");
        overrides.put("b_b_a_n", "bban");
        overrides.put("b_i_c", "bic");
        return transform(openAPI, CaseFormat.LOWER_CAMEL, CaseFormat.LOWER_UNDERSCORE, new HashMap<>());
    }

    @SuppressWarnings("SimplifyStreamApiCallChains")
    public String transform(OpenAPI openAPI, CaseFormat caseFormatFrom, CaseFormat caseFormatTo, final
    Map<String, String> overrides) {

        openAPI.getPaths().forEach((s, pathItem) -> pathItem.readOperations()
            .stream().filter(operation -> operation.getParameters() != null)
            .forEach(operation -> {
                List<Parameter> filtered = operation.getParameters().stream()
                    .map(parameter -> transformParameter(caseFormatFrom, caseFormatTo, overrides, parameter))
                    .collect(Collectors.toList());

                operation.setParameters(filtered);

                RequestBody requestBody = operation.getRequestBody();
                if (requestBody != null && requestBody.getContent() != null) {
                    transformRequestBody(caseFormatFrom, caseFormatTo, overrides, requestBody);
                }

                operation.getResponses().forEach((s1, apiResponse) -> {
                    if (apiResponse.getContent() != null) {
                        transformResponse(caseFormatFrom, caseFormatTo, overrides, apiResponse);
                    }
                });
            }));

        openAPI.getComponents().getSchemas().forEach(
            (s, schema) -> transform(schema, caseFormatFrom, caseFormatTo, overrides)
        );

        return SerializerUtils.toYamlString(openAPI);
    }

    private void transformResponse(CaseFormat caseFormatFrom, CaseFormat caseFormatTo, Map<String, String> overrides,
        ApiResponse apiResponse) {
        Collection<MediaType> values = new ArrayList<>(apiResponse.getContent().values());
        for (MediaType value : values) {

            if (value.getSchema() != null) {
                transform(value.getSchema(), caseFormatFrom, caseFormatTo, overrides);
                if (value.getExample() instanceof String) {
                    value.setExample(
                        snakeExample(value.getExample().toString(), caseFormatFrom, caseFormatTo,
                            overrides));
                }
            }
        }
    }

    private void transformRequestBody(CaseFormat caseFormatFrom, CaseFormat caseFormatTo, Map<String, String> overrides,
        RequestBody requestBody) {
        Collection<MediaType> values = new ArrayList<>(requestBody.getContent().values());
        for (MediaType value : values) {
            transform(value.getSchema(), caseFormatFrom, caseFormatTo, overrides);

            if (value.getExample() instanceof String) {
                value.setExample(
                    snakeExample(value.getExample().toString(), caseFormatFrom, caseFormatTo, overrides));
            }
        }
    }

    private Parameter transformParameter(CaseFormat caseFormatFrom, CaseFormat caseFormatTo,
        Map<String, String> overrides, Parameter parameter) {
        if (parameter instanceof QueryParameter) {
            parameter.setName(transform(parameter.getName(), caseFormatFrom, caseFormatTo, overrides));
        }
        return parameter;
    }

    private void transform(Schema schema, CaseFormat from, CaseFormat to, Map<String, String> overrides) {
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

        if (schema.getExample() instanceof String) {
            String fixedExample = snakeExample(schema.getExample().toString(), from, to, overrides);
            schema.setExample(fixedExample);
        }
    }

    String transform(String name, CaseFormat from, CaseFormat to, Map<String, String> overrides) {
        String result = from.to(to, name);
        for (Map.Entry<String, String> entry : overrides.entrySet()) {
            String fromKeyword = entry.getKey();
            String toKeyword = entry.getValue();
            result = result.replaceAll(fromKeyword, toKeyword);
        }
        return result;
    }

    private ObjectMapper objectMapper = new ObjectMapper();

    private String snakeExample(String example, CaseFormat from, CaseFormat to, Map<String, String> overrides) {
        try {
            JsonNode jsonNode = objectMapper.readTree(example);
            transform(jsonNode, from, to, overrides);
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(jsonNode);
        } catch (IOException e) {
            log.debug("not valid json. nothing to rename");
        }
        return example;
    }

    private void transform(JsonNode json, CaseFormat from, CaseFormat to, Map<String, String> overrides) {
        if (json.isObject()) {
            ObjectNode objectNode = (ObjectNode) json;
            Set<String> toBeRemoved = new HashSet<>();
            Map<String, JsonNode> renamedFields = new LinkedHashMap<>();

            objectNode.fields().forEachRemaining(field -> {
                String newName = transform(field.getKey(), from, to, overrides);

                if (field.getValue().isObject()) {
                    transform(field.getValue(), from, to, overrides);
                }
                if (field.getValue().isArray()) {
                    field.getValue().forEach(jsonNode -> transform(jsonNode, from, to, overrides));
                }
                renamedFields.put(newName, field.getValue());
                toBeRemoved.add(field.getKey());
            });
            objectNode.remove(toBeRemoved);
            objectNode.setAll(renamedFields);
        }

        if (json.isArray()) {
            json.forEach(jsonNode -> transform(jsonNode, from, to, overrides));
        }
    }


}
