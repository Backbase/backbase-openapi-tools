package com.backbase.oss.boat.serializer;

import static com.fasterxml.jackson.databind.MapperFeature.SORT_PROPERTIES_ALPHABETICALLY;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator.Feature;

import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;

import io.swagger.v3.core.util.ObjectMapperFactory;
import io.swagger.v3.core.util.Yaml;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.media.Schema;
import lombok.Lombok;
import lombok.SneakyThrows;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class SerializerUtils {

    public static String toYamlString(OpenAPI openAPI) {
        if (openAPI == null) {
            return null;
        }

        return Yaml.pretty(openAPI);

    }

    public static String toYamlString(Schema<?> schema) {
        return toYamlString(schema, false);
    }

    public static String toYamlString(Schema<?> schema, boolean sorted) {
        SimpleModule module = new SimpleModule("OpenAPISchemaModule");
        module.addSerializer(Schema.class, new SchemaSerializer());
        try {
            return Yaml.mapper()
                .registerModule(module)
                .configure(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY, sorted)
                .writeValueAsString(schema)
                .replace("\r\n", "\n");
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize open Api", e);
        }
        return null;
    }

    @SneakyThrows
    public static void write(Path target, String content) {
        final Path parent = target.getParent();

        if (parent != null) {
            Files.createDirectories(parent);
        }

        Files.write(target,
            content.getBytes(Charset.forName("UTF-8")));
    }

    @SneakyThrows
    public static void write(Path target, Object content) {
        final Path parent = target.getParent();

        if (parent != null) {
            Files.createDirectories(parent);
        }

        Files.write(target, Yaml.mapper().disable(SORT_PROPERTIES_ALPHABETICALLY).writeValueAsBytes(content));
    }
}
