package com.backbase.oss.boat.transformers;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.core.util.Yaml;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.media.ArraySchema;
import io.swagger.v3.oas.models.media.Schema;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

@Slf4j
@UtilityClass
public class Exploder {

    public static void explode(OpenAPI openAPI, File target) {
        if (target.exists()) {
            try {
                Files.delete(target.toPath());
            } catch (IOException e) {
                throw new ExplodeException(e);
            }
        }

        target.mkdirs();

        Set<String> exploded = new HashSet<>();

        log.info("Exploding Open API: {}", openAPI.getInfo().getTitle());

        File componentsDir = new File(target, "components/schemas");
        File examplesDir = new File(target, "components/examples");
        componentsDir.mkdirs();
        examplesDir.mkdirs();

        openAPI.getComponents().getSchemas()
            .forEach((name, schema) -> explode(openAPI, exploded, componentsDir, name, schema));


    }

    @SuppressWarnings("java:S3776")
    private static void explode(OpenAPI openAPI, Set<String> exploded, File componentsDir, String name, Schema schema) {
        Map<String, Schema> properties = schema.getProperties();
        if (properties != null) {
            properties.forEach((propertyName, propertySchema) -> {

                if (propertySchema.get$ref() != null) {
                    String refName = StringUtils.substringAfterLast(propertySchema.get$ref(), "/");
                    Schema componentSchema = openAPI.getComponents().getSchemas().get(refName);
                    if (componentSchema != null && !exploded.contains(refName)) {
                        propertySchema.set$ref("../openapi.yaml#/components/schemas/" + refName);
                        write(componentsDir, refName, propertySchema);
                        exploded.add(refName);
                    }
                }
            });
        }
        if (schema instanceof ArraySchema) {
            String ref = ((ArraySchema) schema).getItems().get$ref();
            if (ref != null) {
                String refName = StringUtils.substringAfterLast(ref, "/");
                Schema componentSchema = openAPI.getComponents().getSchemas().get(refName);
                if (componentSchema != null && !exploded.contains(refName)) {
                    ((ArraySchema) schema).getItems().set$ref("../openapi.yaml#/components/schemas/" + refName);
                    write(componentsDir, refName, componentSchema);
                    exploded.add(refName);
                }
            }

        }

        write(componentsDir, name, schema);


    }

    static ObjectMapper mapper = Yaml.mapper();


    private static void write(File output, String name, Schema schema) {
        try {
            String schemaAsYaml;
            schemaAsYaml = mapper.writeValueAsString(schema);
            Files.write(new File(output, name + ".yaml").toPath(), schemaAsYaml.getBytes());
        } catch (IOException e) {
            log.error("Failed to write: {} to: {}", output.getName(), output);
            throw new ExplodeException(e);
        }
    }
}
