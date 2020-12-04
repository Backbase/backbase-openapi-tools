package com.backbase.oss.boat.transformers;

import static java.util.Collections.singleton;
import static java.util.Collections.singletonMap;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertNotNull;

import com.backbase.oss.boat.loader.OpenAPILoader;
import com.backbase.oss.boat.serializer.SerializerUtils;

import java.io.File;

import io.swagger.v3.oas.models.OpenAPI;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;

@Slf4j
public class VendorExtensionFilterTest {

    @Test
    public void run() throws Throwable {
        Transformer trn = new VendorExtensionFilter();

        OpenAPI api1 = OpenAPILoader.load(new File("src/test/resources/openapi/extension-filter/openapi.yaml"));
        OpenAPI api2 = trn.transform(api1, singletonMap("remove", singleton("x-remove")));

        assertNotNull(api2);

        final String s = SerializerUtils.toYamlString(api2);

        assertThat(s, containsString("x-keep"));
        assertThat(s, not(containsString("x-remove")));
    }

}

