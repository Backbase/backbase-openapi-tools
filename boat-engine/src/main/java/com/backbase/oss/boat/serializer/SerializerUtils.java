package com.backbase.oss.boat.serializer;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.core.util.Json;
import io.swagger.v3.core.util.Json31;
import io.swagger.v3.core.util.Yaml;
import io.swagger.v3.core.util.Yaml31;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.SpecVersion;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@UtilityClass
public class SerializerUtils {

    private static final String OPENAPI_31_PREFIX = "3.1";

    /**
     * Whether the document must be treated as OpenAPI 3.1.x rather than 3.0.x.
     *
     * <p>The parser sets {@link OpenAPI#getSpecVersion()} while reading, so that is the primary signal. The
     * {@code openapi} field is used as a fallback because {@code new OpenAPI()} defaults the spec version to
     * {@link SpecVersion#V30}: a document that lost its spec version in an earlier round trip is still
     * recognised from its header.
     *
     * @param openAPI the document to inspect, may be null
     * @return true when the document is OpenAPI 3.1.x, false for 3.0.x and for null
     */
    public static boolean isOpenApi31(OpenAPI openAPI) {
        if (openAPI == null) {
            return false;
        }
        if (openAPI.getSpecVersion() == SpecVersion.V31) {
            return true;
        }
        String version = openAPI.getOpenapi();
        return version != null && version.startsWith(OPENAPI_31_PREFIX);
    }

    public static String toYamlString(OpenAPI openAPI) {
        if (openAPI == null) {
            return null;
        }
        if (isOpenApi31(openAPI)) {
            log.debug("Serializing OpenAPI {} as YAML using the 3.1 mapper", openAPI.getOpenapi());
            return Yaml31.pretty(openAPI);
        }
        return Yaml.pretty(openAPI);
    }

    public static String toJsonString(OpenAPI openAPI) {
        if (openAPI == null) {
            return null;
        }
        if (isOpenApi31(openAPI)) {
            log.debug("Serializing OpenAPI {} as JSON using the 3.1 mapper", openAPI.getOpenapi());
            return Json31.pretty(openAPI);
        }
        return Json.pretty(openAPI);
    }

    /**
     * The YAML mapper matching the document's spec version.
     *
     * <p>Note that swagger-core returns shared singletons here: callers must read from the mapper without
     * reconfiguring it.
     *
     * @param openAPI the document the mapper will be used on, may be null
     * @return the 3.1 mapper for 3.1 documents, the 3.0 mapper otherwise
     */
    public static ObjectMapper yamlMapper(OpenAPI openAPI) {
        return isOpenApi31(openAPI) ? Yaml31.mapper() : Yaml.mapper();
    }

    /**
     * The JSON mapper matching the document's spec version. Shares the singleton caveat of
     * {@link #yamlMapper(OpenAPI)}.
     *
     * @param openAPI the document the mapper will be used on, may be null
     * @return the 3.1 mapper for 3.1 documents, the 3.0 mapper otherwise
     */
    public static ObjectMapper jsonMapper(OpenAPI openAPI) {
        return isOpenApi31(openAPI) ? Json31.mapper() : Json.mapper();
    }

}
