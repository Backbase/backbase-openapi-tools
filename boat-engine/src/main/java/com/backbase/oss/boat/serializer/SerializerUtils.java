package com.backbase.oss.boat.serializer;

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
        return Yaml.pretty(openAPI);
    }

}
