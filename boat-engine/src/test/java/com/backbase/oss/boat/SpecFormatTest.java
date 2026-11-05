package com.backbase.oss.boat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class SpecFormatTest {

    private static final File SHARED_EVENTS_DIR =
        new File("../boat-trail-resources/src/main/resources/asyncapi/shared-events");

    @TempDir
    Path dir;

    @Test
    void detectsRootKeyInYaml() {
        assertEquals(SpecFormat.ASYNCAPI, SpecFormat.detect("asyncapi: 3.0.0\ninfo:\n  title: x\n"));
        assertEquals(SpecFormat.ASYNCAPI, SpecFormat.detect("# comment\n\"asyncapi\": '3.0.0'\n"));
        assertEquals(SpecFormat.OPENAPI, SpecFormat.detect("openapi: 3.0.3\ninfo:\n  title: x\n"));
        assertEquals(SpecFormat.OPENAPI, SpecFormat.detect("swagger: '2.0'\n"));
        assertEquals(SpecFormat.UNKNOWN, SpecFormat.detect("type: object\nproperties:\n  asyncapi:\n    type: string\n"));
    }

    @Test
    void detectsRootKeyInJson() {
        assertEquals(SpecFormat.ASYNCAPI, SpecFormat.detect("{\"info\": {}, \"asyncapi\": \"3.0.0\"}"));
        assertEquals(SpecFormat.OPENAPI, SpecFormat.detect("{\"openapi\": \"3.1.0\"}"));
        assertEquals(SpecFormat.UNKNOWN, SpecFormat.detect("{\"type\": \"object\"}"));
    }

    @Test
    void detectsFiles() throws IOException {
        assertEquals(SpecFormat.ASYNCAPI, SpecFormat.detect(new File(SHARED_EVENTS_DIR, "asyncapi.yaml")));
        assertEquals(SpecFormat.OPENAPI, SpecFormat.detect(new File(SHARED_EVENTS_DIR, "openapi.yaml")));
        assertEquals(SpecFormat.UNKNOWN, SpecFormat.detect(new File(SHARED_EVENTS_DIR, "lib/schemas/order.yaml")));
        assertEquals(SpecFormat.UNKNOWN, SpecFormat.detect(dir.resolve("missing.yaml").toFile()));

        Path json = dir.resolve("events.json");
        Files.write(json, "{\"asyncapi\": \"3.0.0\"}".getBytes(StandardCharsets.UTF_8));
        assertTrue(SpecFormat.detect(json.toFile()).isAsyncApi());
        assertFalse(SpecFormat.OPENAPI.isAsyncApi());
    }
}
