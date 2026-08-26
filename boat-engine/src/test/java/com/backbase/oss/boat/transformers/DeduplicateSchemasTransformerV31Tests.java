package com.backbase.oss.boat.transformers;

import static java.util.Collections.emptyMap;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.Paths;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.IntegerSchema;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class DeduplicateSchemasTransformerV31Tests {

    @Test
    void preservesPropertyTypesWhenRewritingReferences() {
        Schema<?> messageRequest = new Schema<>();
        messageRequest.setType("object");
        Schema<?> deliveryChannelProp = new StringSchema();
        deliveryChannelProp.setMaxLength(64);
        deliveryChannelProp.setDescription("Identifier for channel to deliver message");
        messageRequest.addProperty("deliveryChannel", deliveryChannelProp);

        Schema<?> priorityProp = new IntegerSchema();
        priorityProp.setMinimum(BigDecimal.ZERO);
        priorityProp.setMaximum(BigDecimal.valueOf(7));
        messageRequest.addProperty("priority", priorityProp);

        Schema<?> duplicate = new Schema<>();
        duplicate.setType("object");
        Schema<?> dupDeliveryChannel = new StringSchema();
        dupDeliveryChannel.setMaxLength(64);
        dupDeliveryChannel.setDescription("Identifier for channel to deliver message");
        duplicate.addProperty("deliveryChannel", dupDeliveryChannel);

        Schema<?> dupPriority = new IntegerSchema();
        dupPriority.setMinimum(BigDecimal.ZERO);
        dupPriority.setMaximum(BigDecimal.valueOf(7));
        duplicate.addProperty("priority", dupPriority);

        OpenAPI openAPI = new OpenAPI();
        openAPI.setOpenapi("3.1.0");
        openAPI.setComponents(new Components()
            .addSchemas("MessageRequest", messageRequest)
            .addSchemas("messageRequest", duplicate));

        new DeduplicateSchemasTransformerV31().transform(openAPI, emptyMap());

        Schema<?> result = openAPI.getComponents().getSchemas().get("MessageRequest");
        assertEquals("string", result.getProperties().get("deliveryChannel").getType(),
            "String property type should be preserved");
        assertEquals("integer", result.getProperties().get("priority").getType(),
            "Integer property type should be preserved");
    }

    @Test
    void mergesStructurallyIdenticalSchemasAndRewritesReferences() {
        Schema<?> canonical = new Schema<>();
        canonical.setType("object");
        canonical.addProperty("id", new StringSchema());

        Schema<?> duplicate = new Schema<>();
        duplicate.setType("object");
        duplicate.addProperty("id", new StringSchema());

        OpenAPI openAPI = new OpenAPI();
        openAPI.setOpenapi("3.1.0");
        openAPI.setComponents(new Components()
            .addSchemas("CurrencyExchangeArrangement", canonical)
            .addSchemas("CurrencyExchangeArrangementYaml", duplicate));

        Operation getOperation = new Operation();
        ApiResponse response = new ApiResponse();
        response.setContent(new Content().addMediaType("application/json",
            new MediaType().schema(new Schema<>().$ref("#/components/schemas/CurrencyExchangeArrangementYaml"))));
        getOperation.setResponses(new ApiResponses().addApiResponse("200", response));

        openAPI.setPaths(new Paths().addPathItem("/currency-exchange-arrangements",
            new PathItem().get(getOperation)));

        new DeduplicateSchemasTransformerV31().transform(openAPI, emptyMap());

        assertEquals(1, openAPI.getComponents().getSchemas().size(), "The duplicate schema should be removed.");
        assertTrue(openAPI.getComponents().getSchemas().containsKey("CurrencyExchangeArrangement"),
            "The shorter (canonical) name should be kept.");
        assertFalse(openAPI.getComponents().getSchemas().containsKey("CurrencyExchangeArrangementYaml"));

        String rewrittenRef = openAPI.getPaths().get("/currency-exchange-arrangements").getGet()
            .getResponses().get("200").getContent().get("application/json").getSchema().get$ref();
        assertEquals("#/components/schemas/CurrencyExchangeArrangement", rewrittenRef,
            "The response schema $ref should be rewritten to point at the canonical schema.");
    }

    @Test
    void leavesDistinctSchemasAlone() {
        Schema<?> first = new Schema<>();
        first.setType("object");
        first.addProperty("id", new StringSchema());

        Schema<?> second = new Schema<>();
        second.setType("object");
        second.addProperty("name", new StringSchema());

        OpenAPI openAPI = new OpenAPI();
        openAPI.setOpenapi("3.1.0");
        openAPI.setComponents(new Components()
            .addSchemas("First", first)
            .addSchemas("Second", second));
        openAPI.setPaths(new Paths());

        OpenAPI result = new DeduplicateSchemasTransformerV31().transform(openAPI, emptyMap());

        assertEquals(2, result.getComponents().getSchemas().size());
        assertNull(result.getComponents().getSchemas().get("First").get$ref());
        assertNull(result.getComponents().getSchemas().get("Second").get$ref());
    }
}
