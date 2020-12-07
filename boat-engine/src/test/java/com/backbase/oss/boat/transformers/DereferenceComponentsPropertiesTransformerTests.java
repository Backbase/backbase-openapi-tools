package com.backbase.oss.boat.transformers;

import com.backbase.oss.boat.loader.OpenAPILoader;
import com.backbase.oss.boat.loader.OpenAPILoaderException;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.media.ArraySchema;
import io.swagger.v3.oas.models.media.Schema;
import java.io.File;
import java.util.Arrays;
import org.junit.jupiter.api.Test;

import static java.util.Collections.emptyMap;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.fail;

public class DereferenceComponentsPropertiesTransformerTests {

    private static final String APPLICATION_JSON = "application/json";

    @Test
    public void testDereferenceComponentsPropertiesApi() throws OpenAPILoaderException {

        File input = new File("src/test/resources/openapi/decomposer-test-api/openapi.yaml");
        OpenAPI openAPI = OpenAPILoader.load(input);

        new DereferenceComponentsPropertiesTransformer().transform(openAPI, emptyMap());

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

        assertThat("Referencing another schema's property schema also works",
            ((Schema) openAPI.getComponents().getSchemas().get("ReferencingOtherComponentsProperty").getProperties()
                .get("myCode")).getDescription(),
                is("directors code"));
        assertThat("Referencing another schema's property properties schema also works",
            ((Schema) openAPI.getComponents().getSchemas().get("ReferencingOtherComponentsProperty").getProperties()
                .get("myNested")).getDescription(),
            is("nested id"));
        assertThat("Referencing another schema's items schema also works",
            ((Schema) openAPI.getComponents().getSchemas().get("ReferencingOtherComponentsProperty").getProperties()
                .get("myDirect")).getDescription(),
            is("direct"));

    }

    @Test
    public void testNullComponentsShouldNotFailWithNPE() {
        OpenAPI openAPI = new OpenAPI();
        try {
            new DereferenceComponentsPropertiesTransformer().transform(openAPI, emptyMap());
        } catch (NullPointerException e) {
            fail("Unexpected NullPointerException");
        }

    }

    @Test
    public void testNullComponentSchemasShouldNotFailWithNPE() {
        OpenAPI openAPI = new OpenAPI();
        openAPI.setComponents(new Components());
        try {
            new DereferenceComponentsPropertiesTransformer().transform(openAPI, emptyMap());
        } catch (NullPointerException e) {
            fail("Unexpected NullPointerException");
        }
    }

    private Schema getProperty(OpenAPI openAPI, String direct, String component) {
        return (Schema) openAPI.getComponents().getSchemas().get(component).getProperties().get(direct);
    }

}
