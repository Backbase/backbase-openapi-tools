package com.backbase.oss.boat.transformers;

import static java.util.Collections.singleton;
import static java.util.Collections.singletonMap;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.backbase.oss.boat.loader.OpenAPILoader;
import com.backbase.oss.boat.serializer.SerializerUtils;

import java.io.File;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.SpecVersion;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

@Slf4j
class ExtensionFilterTests {

    @Test
    void run() throws Throwable {
        Transformer trn = new ExtensionFilter();

        OpenAPI api1 = OpenAPILoader.load(new File("src/test/resources/openapi/extension-filter/openapi.yaml"));
        OpenAPI api2 = trn.transform(api1, singletonMap("remove", singleton("x-remove")));

        assertNotNull(api2);

        final String s = SerializerUtils.toYamlString(api2);

        assertThat(s, containsString("x-keep"));
        assertThat(s, not(containsString("x-remove")));
    }

    /**
     * The filter round-trips the whole document through a Jackson mapper. Done with the 3.0 mapper, that
     * silently strips every 3.1-only construct and resets the spec version, so a 3.1 spec came out of the
     * filter downgraded even though the extension filtering itself looked correct.
     */
    @Test
    void retainsOpenApi31Constructs() throws Throwable {
        Transformer trn = new ExtensionFilter();

        OpenAPI api1 = OpenAPILoader.load(new File("src/test/resources/openapi/openapi-3-1/openapi.yaml"));
        OpenAPI api2 = trn.transform(api1, singletonMap("remove", singleton("x-remove")));

        assertNotNull(api2);
        assertEquals(SpecVersion.V31, api2.getSpecVersion());
        assertNotNull(api2.getWebhooks());
        assertThat(api2.getWebhooks().keySet(), hasItem("thingChanged"));

        final String s = SerializerUtils.toYamlString(api2);

        assertThat(s, containsString("x-keep"));
        assertThat(s, not(containsString("x-remove")));

        assertThat(s, containsString("openapi: 3.1.0"));
        assertThat(s, containsString("webhooks:"));
        assertThat(s, containsString("const: thing"));
        assertThat(s, containsString("exclusiveMinimum: 0"));
        assertThat(s, containsString("- \"null\""));
        assertThat(s, not(containsString("nickname: {}")));
    }

}

