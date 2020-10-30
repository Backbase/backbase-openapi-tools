package com.backbase.oss.boat.transformers;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

import com.backbase.oss.boat.loader.OpenAPILoader;
import com.backbase.oss.boat.loader.OpenAPILoaderException;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.media.ArraySchema;
import io.swagger.v3.oas.models.media.Schema;
import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.junit.Test;

public class DereferenceComponentsPropertiesTransformerTest {

    private static final String APPLICATION_JSON = "application/json";
    private static org.hamcrest.Matcher<String> isComponentExample = new BaseMatcher<String>() {

        @Override
        public boolean matches(Object item) {
            return String.valueOf(item).startsWith("#/components/examples/");
        }

        @Override
        public void describeTo(Description description) {
            description.appendText(" should start with '#/components/examples/'");
        }
    };

    @Test
    public void testDereferenceComponentsPropertiesApi() throws OpenAPILoaderException {

        File input = new File("src/test/resources/openapi/decomposer-test-api/openapi.yaml");
        OpenAPI openAPI = OpenAPILoader.load(input);

        new DereferenceComponentsPropertiesTransformer().transform(openAPI, Collections.emptyMap());

        assertThat("SingleReference is not affected.",
            openAPI.getComponents().getSchemas().get("SingleReference").get$ref(),
            is("#/components/schemas/direct"));

        assertThat("DirectReference is not affected, despite being reference by something else.",
            openAPI.getComponents().getSchemas().get("DirectReference").get$ref(),
            is("#/components/schemas/direct"));

        assertThat("DoubleReference is not affected, despite being reference by something else.",
            openAPI.getComponents().getSchemas().get("DoubleReference").get$ref(),
            is("#/components/schemas/DirectReference"));

        assertThat("ReferencingProperties properties have been dereferenced.",
            getProperty(openAPI, "direct", "ReferencingProperties").getDescription(), is("direct"));
        assertThat("ReferencingProperties properties have been dereferenced.",
            getProperty(openAPI, "doubleReference", "ReferencingProperties").getDescription(), is("direct"));
        assertThat("ReferencingProperties properties have been dereferenced.",
            getProperty(openAPI, "myOwn", "ReferencingProperties").getDescription(), is("myOwn"));

        assertThat("Composite properties have been merged.",
            getProperty(openAPI, "ofMyOwn", "Composite").getType(), is("boolean"));
        assertThat("Composite properties have been merged.",
            getProperty(openAPI, "id", "Composite").getType(), is("string"));
        assertThat("Composite properties have been merged.",
            getProperty(openAPI, "code", "Composite").getType(), is("string"));
        assertThat("Composite required properties have been merged.",
            openAPI.getComponents().getSchemas().get("Composite").getRequired(),
            is(Arrays.asList("ofMyOwn", "id", "code")));

        assertThat("Array items are dereferenced",
            openAPI.getComponents().getSchemas().get("Array"),
            instanceOf(ArraySchema.class));
        assertThat("Array items are dereferenced",
            ((ArraySchema)openAPI.getComponents().getSchemas().get("Array")).getItems().get$ref(),
            nullValue());

    }

    private Schema getProperty(OpenAPI openAPI, String direct, String component) {
        return (Schema) openAPI.getComponents().getSchemas().get(component).getProperties().get(direct);
    }

}
