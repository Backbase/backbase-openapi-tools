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
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import org.junit.jupiter.api.Test;

class DeduplicateSchemasTransformerTests {

    @Test
    void mergesStructurallyIdenticalSchemasAndRewritesReferences() {
        Schema<?> canonical = new Schema<>();
        canonical.setType("object");
        canonical.addProperty("id", new StringSchema());

        Schema<?> duplicate = new Schema<>();
        duplicate.setType("object");
        duplicate.addProperty("id", new StringSchema());

        OpenAPI openAPI = new OpenAPI();
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

        new DeduplicateSchemasTransformer().transform(openAPI, emptyMap());

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
        openAPI.setComponents(new Components()
            .addSchemas("First", first)
            .addSchemas("Second", second));
        openAPI.setPaths(new Paths());

        OpenAPI result = new DeduplicateSchemasTransformer().transform(openAPI, emptyMap());

        assertEquals(2, result.getComponents().getSchemas().size());
        assertNull(result.getComponents().getSchemas().get("First").get$ref());
        assertNull(result.getComponents().getSchemas().get("Second").get$ref());
    }
}
