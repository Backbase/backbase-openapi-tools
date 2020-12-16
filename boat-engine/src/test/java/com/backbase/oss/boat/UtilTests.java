package com.backbase.oss.boat;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;

import javax.validation.constraints.AssertFalse;

import static org.junit.Assert.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;

 class UtilTests {

    @SneakyThrows
    @Test
     void testUtils() {
        URL url = new URL("file://test.json");
        BiMap<String, String> referenceNames = HashBiMap.create();
        String actual = Utils.getSchemaNameFromReference(url, "test", referenceNames);
        assertEquals("Test",actual);
    }

    @SneakyThrows
    @Test
     void testUtils2() {
        URL url = new URL("file://test.json");
        URL other = new URL("file://other.json");
        URL unusual = new URL("file://unusual.json");
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
        referenceNames.put("file://unusual.json", "differentToUrl");
        actual = Utils.getSchemaNameFromReference(unusual, "unusual", referenceNames);
        assertEquals("UnusualUnusual",actual);
    }

    @Test
    void testUtilsDirectory() throws MalformedURLException {
       URL url = new URL("file://test.json");
       assertFalse(Utils.isDirectory(url,"test"));

    }



}
