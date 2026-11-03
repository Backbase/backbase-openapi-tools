package com.backbase.oss.boat.asyncapi;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.backbase.oss.boat.asyncapi.SchemaFormats.Draft;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

class SchemaFormatsTest {

    @ParameterizedTest
    @CsvSource({
        "application/schema+json;version=draft-04, DRAFT_04",
        "application/schema+yaml;version=draft-04, DRAFT_04",
        "application/schema+json;version=draft-06, DRAFT_06",
        "application/schema+yaml;version=draft-06, DRAFT_06",
        "application/schema+json;version=draft-07, DRAFT_07",
        "application/schema+yaml;version=draft-07, DRAFT_07",
        "application/schema+json;version=draft-2019-09, DRAFT_2019_09",
        "application/schema+yaml;version=draft-2019-09, DRAFT_2019_09",
        "application/schema+json;version=draft-2020-12, DRAFT_2020_12",
        "application/schema+yaml;version=draft-2020-12, DRAFT_2020_12",
        "'Application/Schema+YAML; version=draft-2020-12', DRAFT_2020_12",
    })
    void acceptsJsonSchemaDrafts(String format, Draft draft) {
        assertTrue(SchemaFormats.isSupported(format));
        assertEquals(Optional.of(draft), SchemaFormats.draft(format));
    }

    @ParameterizedTest
    @ValueSource(strings = {"application/schema+json", "application/schema+yaml"})
    void acceptsJsonSchemaWithoutVersion(String format) {
        assertTrue(SchemaFormats.isSupported(format));
        assertEquals(Optional.empty(), SchemaFormats.draft(format));
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "application/vnd.aai.asyncapi;version=3.0.0",
        "application/vnd.aai.asyncapi+json;version=3.0.0",
        "application/vnd.aai.asyncapi+yaml;version=3.0.0",
        "application/vnd.oai.openapi;version=3.0.0",
        "application/vnd.oai.openapi+json;version=3.0.0",
        "application/vnd.oai.openapi+yaml;version=3.0.0"
    })
    void acceptsAsyncApiAndOpenApiSchemas(String format) {
        assertTrue(SchemaFormats.isSupported(format));
        assertEquals(Optional.empty(), SchemaFormats.draft(format));
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "application/schema+json;version=draft-05",
        "application/schema+json;version=2020-12",
        "application/vnd.apache.avro;version=1.9.0",
        "application/raml+yaml;version=1.0",
        "application/vnd.aai.asyncapi;version=2.6.0"
    })
    void rejectsOtherFormats(String format) {
        assertFalse(SchemaFormats.isSupported(format));
        assertEquals(Optional.empty(), SchemaFormats.draft(format));
    }

    @Test
    void absentFormatIsTheAsyncApiDefault() {
        assertTrue(SchemaFormats.isSupported(null));
        assertEquals(Optional.empty(), SchemaFormats.draft(null));
    }
}
