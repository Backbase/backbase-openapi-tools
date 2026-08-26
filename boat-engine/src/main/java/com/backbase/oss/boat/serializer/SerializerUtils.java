package com.backbase.oss.boat.serializer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;
import io.swagger.v3.core.util.Json;
import io.swagger.v3.core.util.Yaml;
import io.swagger.v3.oas.models.OpenAPI;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@UtilityClass
public class SerializerUtils {

    public static String toYamlString(OpenAPI openAPI) {
        if (openAPI == null) {
            return null;
        }

        try {
            // For OpenAPI 3.1.x, use JSON serialization as intermediate to preserve type information
            // Swagger-core's Yaml.pretty() omits type fields in 3.1.x mode
            if (openAPI.getOpenapi() != null && openAPI.getOpenapi().startsWith("3.1")) {
                return serializeAs31Yaml(openAPI);
            }
        } catch (Exception e) {
            log.debug("Failed to use custom 3.1 serialization, falling back to default", e);
        }

        return Yaml.pretty(openAPI);
    }

    private static String serializeAs31Yaml(OpenAPI openAPI) throws Exception {
        // Serialize to JSON first to preserve all type information
        String jsonString = Json.mapper().writerWithDefaultPrettyPrinter().writeValueAsString(openAPI);

        // Parse JSON back to JsonNode
        JsonNode jsonNode = Json.mapper().readTree(jsonString);

        // Serialize to YAML using Jackson's YAML mapper
        YAMLFactory yamlFactory = new YAMLFactory()
            .enable(YAMLGenerator.Feature.MINIMIZE_QUOTES)
            .disable(YAMLGenerator.Feature.WRITE_DOC_START_MARKER);

        ObjectMapper yamlMapper = new ObjectMapper(yamlFactory);
        return yamlMapper.writerWithDefaultPrettyPrinter().writeValueAsString(jsonNode);
    }

}
