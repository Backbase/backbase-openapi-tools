package com.backbase.oss.boat.serializer;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.core.util.Json;
import io.swagger.v3.core.util.Json31;
import io.swagger.v3.core.util.Yaml;
import io.swagger.v3.core.util.Yaml31;
import io.swagger.v3.oas.models.OpenAPI;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@UtilityClass
public class SerializerUtils {

    /**
     * Serializes the specification with the mapper matching its OpenAPI version.
     *
     * <p>A 3.1 document is parsed into {@code Schema#types} (the JSON Schema {@code type} keyword became a
     * union in 3.1) and {@code Schema#type} is left null. The 3.0 mapper behind {@link Yaml#pretty} only
     * knows about {@code type}, so serializing a 3.1 document with it silently drops every type declaration.
     * {@link Yaml31} writes {@code types} back out as {@code type}.
     */
    public static String toYamlString(OpenAPI openAPI) {
        if (openAPI == null) {
            return null;
        }
        return isOpenAPI31(openAPI) ? Yaml31.pretty(openAPI) : Yaml.pretty(openAPI);
    }

    /**
     * The JSON mapper matching the specification's OpenAPI version. Round-tripping a 3.1 document through
     * {@link Json#mapper()} loses all type information; {@link Json31#mapper()} preserves it.
     */
    public static ObjectMapper jsonMapperFor(OpenAPI openAPI) {
        return openAPI != null && isOpenAPI31(openAPI) ? Json31.mapper() : Json.mapper();
    }

    private static boolean isOpenAPI31(OpenAPI openAPI) {
        return openAPI.getOpenapi() != null && openAPI.getOpenapi().startsWith("3.1");
    }

}
