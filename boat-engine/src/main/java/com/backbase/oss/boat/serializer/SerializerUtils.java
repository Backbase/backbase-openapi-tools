package com.backbase.oss.boat.serializer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator.Feature;
import io.swagger.v3.core.util.ObjectMapperFactory;
import io.swagger.v3.core.util.Yaml;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.media.Schema;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@UtilityClass
public class SerializerUtils {

    public static String toYamlString(OpenAPI openAPI) {
        if (openAPI == null) {
            return null;
        }

        return Yaml.pretty(openAPI);

    }

    public static String toYamlString(Schema schema) {
        SimpleModule module = new SimpleModule("OpenAPISchemaModule");
        module.addSerializer(Schema.class, new SchemaSerializer());
        try {
            return Yaml.mapper()
                .registerModule(module)
                .configure(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY, false)
                .writeValueAsString(schema)
                .replace("\r\n", "\n");
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize open Api", e);
        }
        return null;
    }
}
