package com.backbase.oss.boat.serializer;

import static org.junit.jupiter.api.Assertions.assertTrue;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.media.IntegerSchema;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class SerializerUtilsTest {

    @Test
    void preservesPropertyTypesInOpenAPI31YamlSerialization() {
        // A 3.1 document carries the type keyword in Schema#types, which is what the parser populates and
        // what only the 3.1 mapper writes back out. setType() would fill Schema#type instead, a field the
        // 3.1 mapper ignores.
        Schema<?> messageRequest = new Schema<>();
        messageRequest.addType("object");

        Schema<?> deliveryChannelProp = new StringSchema();
        deliveryChannelProp.setMaxLength(64);
        deliveryChannelProp.setDescription("Identifier for channel to deliver message");
        messageRequest.addProperty("deliveryChannel", deliveryChannelProp);

        Schema<?> priorityProp = new IntegerSchema();
        priorityProp.setMinimum(BigDecimal.ZERO);
        priorityProp.setMaximum(BigDecimal.valueOf(7));
        priorityProp.setDescription("Priority level");
        messageRequest.addProperty("priority", priorityProp);

        OpenAPI openAPI = new OpenAPI();
        openAPI.setOpenapi("3.1.0");
        openAPI.setComponents(new Components().addSchemas("MessageRequest", messageRequest));

        String yaml = SerializerUtils.toYamlString(openAPI);

        // Verify that type fields are preserved in the output
        assertTrue(yaml.contains("type: object"), "Schema type should be present");
        assertTrue(yaml.contains("type: string"), "String property type should be preserved");
        assertTrue(yaml.contains("type: integer"), "Integer property type should be preserved");
        assertTrue(yaml.contains("maxLength: 64"), "MaxLength constraint should be preserved");
        assertTrue(yaml.contains("maximum: 7"), "Maximum constraint should be preserved");
        assertTrue(yaml.contains("minimum: 0"), "Minimum constraint should be preserved");
    }

    @Test
    void handlesNullOpenAPIGracefully() {
        String result = SerializerUtils.toYamlString(null);
        assertTrue(result == null || result.isEmpty(), "Null OpenAPI should return null or empty");
    }
}
