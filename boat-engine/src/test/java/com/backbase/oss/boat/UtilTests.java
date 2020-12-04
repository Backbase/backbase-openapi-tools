package com.backbase.oss.boat;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import java.net.URL;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class UtilTests {

    @SneakyThrows
    @Test
    public void testUtils() {
        URL url = new URL("file://test.json");
        BiMap<String, String> referenceNames = HashBiMap.create();
        String actual = Utils.getSchemaNameFromReference(url, "test", referenceNames);
        assertEquals("Test",actual);
    }

    @SneakyThrows
    @Test
    public void testUtils2() {
        URL url = new URL("file://test.json");
        URL other = new URL("file://other.json");
        BiMap<String, String> referenceNames = HashBiMap.create();
        String actual;
        actual = Utils.getSchemaNameFromReference(url, "test", referenceNames);
        assertEquals("Test",actual);
        actual = Utils.getSchemaNameFromReference(other, "test", referenceNames);
        assertEquals("Other", actual);
        actual = Utils.getSchemaNameFromReference(url, "other", referenceNames);
        assertEquals("Test", actual);
        actual = Utils.getSchemaNameFromReference(other, "other", referenceNames);
        assertEquals("Other",actual);
    }


}
