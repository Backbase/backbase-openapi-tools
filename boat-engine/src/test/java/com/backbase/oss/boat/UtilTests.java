package com.backbase.oss.boat;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import java.net.URL;
import lombok.SneakyThrows;
import org.junit.Test;

public class UtilTests {

    @SneakyThrows
    @Test
    public void testUtils() {
        URL url = new URL("file://test.json");
        BiMap<String, String> referenceNames = HashBiMap.create();
        Utils.getSchemaNameFromReference(url, "test", referenceNames);
    }

    @SneakyThrows
    @Test
    public void testUtils2() {
        URL url = new URL("file://test.json");
        URL other = new URL("file://other.json");
        BiMap<String, String> referenceNames = HashBiMap.create();
        Utils.getSchemaNameFromReference(url, "test", referenceNames);
        Utils.getSchemaNameFromReference(other, "test", referenceNames);
        Utils.getSchemaNameFromReference(url, "other", referenceNames);
        Utils.getSchemaNameFromReference(other, "other", referenceNames);
    }


}
