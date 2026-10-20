package com.backbase.oss.boat.serializer;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.backbase.oss.boat.loader.OpenAPILoader;
import com.backbase.oss.boat.loader.OpenAPILoaderException;
import io.swagger.v3.core.util.Yaml;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.SpecVersion;
import java.io.File;
import org.junit.jupiter.api.Test;

class SerializerUtilsTests {

    private static final String OPENAPI_31 = "src/test/resources/openapi/openapi-3-1/openapi.yaml";
    private static final String OPENAPI_30 = "src/test/resources/openapi/extension-filter/openapi.yaml";

    private OpenAPI load(String path) throws OpenAPILoaderException {
        return OpenAPILoader.load(new File(path));
    }

    @Test
    void detectsVersionFromTheParsedSpecVersion() throws OpenAPILoaderException {
        assertTrue(SerializerUtils.isOpenApi31(load(OPENAPI_31)));
        assertFalse(SerializerUtils.isOpenApi31(load(OPENAPI_30)));
    }

    @Test
    void detectsVersionFromTheOpenapiFieldWhenSpecVersionWasLost() {
        // A round trip through a 3.0 mapper resets specVersion to the V30 default while leaving the header
        // intact; the document must still be recognised as 3.1.
        OpenAPI openAPI = new OpenAPI();
        openAPI.setSpecVersion(SpecVersion.V30);
        openAPI.setOpenapi("3.1.0");

        assertTrue(SerializerUtils.isOpenApi31(openAPI));
    }

    @Test
    void treatsNullAndVersionlessDocumentsAsNotOpenApi31() {
        assertFalse(SerializerUtils.isOpenApi31(null));
        assertFalse(SerializerUtils.isOpenApi31(new OpenAPI()));
    }

    @Test
    void keepsTheNullInNullOutContract() {
        assertNull(SerializerUtils.toYamlString(null));
        assertNull(SerializerUtils.toJsonString(null));
    }

    /**
     * The regression this class exists for: serialized through the 3.0 mapper, a 3.1 document keeps its
     * {@code openapi: 3.1.0} header but loses {@code webhooks} entirely and degrades every 3.1-only schema
     * keyword, producing an invalid hybrid document.
     */
    @Test
    void yamlRetainsOpenApi31Constructs() throws OpenAPILoaderException {
        String yaml = SerializerUtils.toYamlString(load(OPENAPI_31));

        assertThat(yaml, containsString("openapi: 3.1.0"));
        assertThat(yaml, containsString("webhooks:"));
        assertThat(yaml, containsString("thingChanged:"));
        assertThat(yaml, containsString("jsonSchemaDialect:"));
        assertThat(yaml, containsString("const: thing"));
        assertThat(yaml, containsString("exclusiveMinimum: 0"));
        assertThat(yaml, containsString("contentMediaType: application/octet-stream"));
        // the "type: [string, \"null\"]" array, which the 3.0 mapper cannot represent
        assertThat(yaml, containsString("- \"null\""));
        // the 3.0 mapper collapses these properties to "nickname: {}" / "kind: {}" / "payload: {}"
        assertThat(yaml, not(containsString("nickname: {}")));
        assertThat(yaml, not(containsString("kind: {}")));
        assertThat(yaml, not(containsString("payload: {}")));
    }

    @Test
    void jsonRetainsOpenApi31Constructs() throws OpenAPILoaderException {
        String json = SerializerUtils.toJsonString(load(OPENAPI_31));

        assertThat(json, containsString("\"webhooks\""));
        assertThat(json, containsString("\"const\" : \"thing\""));
        assertThat(json, containsString("\"jsonSchemaDialect\""));
    }

    @Test
    void leavesOpenApi30SerializationUntouched() throws OpenAPILoaderException {
        OpenAPI openAPI = load(OPENAPI_30);

        assertEquals(Yaml.pretty(openAPI), SerializerUtils.toYamlString(openAPI));
    }
}
