package com.backbase.oss.boat.asyncapi;

import static com.backbase.oss.boat.asyncapi.AsyncApiTestFiles.SHARED_EVENTS;
import static com.backbase.oss.boat.asyncapi.AsyncApiTestFiles.SHARED_OPENAPI;
import static com.backbase.oss.boat.asyncapi.AsyncApiTestFiles.asyncApi;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.backbase.oss.boat.asyncapi.model.AsyncApiDocument;
import com.backbase.oss.boat.loader.OpenAPILoader;
import com.backbase.oss.boat.serializer.SerializerUtils;
import com.backbase.oss.boat.transformers.Bundler;
import com.backbase.oss.boat.transformers.DeduplicateSchemasTransformer;
import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.media.Schema;
import java.io.File;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class AsyncApiOpenApiProjectorTest {

    @TempDir
    Path dir;

    @Test
    void sharedSchemasMatchTheOpenApiBundle() throws Exception {
        OpenAPI rest = OpenAPILoader.load(SHARED_OPENAPI);
        new Bundler(SHARED_OPENAPI).transform(rest);
        new DeduplicateSchemasTransformer().transform(rest);

        OpenAPI events = project(SHARED_EVENTS).getOpenAPI();

        for (String name : List.of("Order", "Money", "OrderStatus")) {
            assertEquals(json(rest, rest.getComponents().getSchemas().get(name)),
                json(events, events.getComponents().getSchemas().get(name)), name);
        }
        assertEquals(Set.of("Order", "Money", "OrderStatus"), rest.getComponents().getSchemas().keySet());
    }

    @Test
    void namesInlinePayloadsAndHeadersAfterTheMessage() throws Exception {
        AsyncApiProjection projection = project(SHARED_EVENTS);
        Map<String, Schema> schemas = projection.getOpenAPI().getComponents().getSchemas();

        assertEquals(Set.of("Order", "Money", "OrderStatus", "OrderPlacedHeaders", "OrderStatusChangedPayload",
            "OrderValidatedPayload", "PaymentSettledPayload"), schemas.keySet());
        assertTrue(projection.getWarnings().isEmpty(), () -> "warnings: " + projection.getWarnings());

        AsyncApiProjection.MessageSchemas orderPlaced = projection.getMessageSchemas().get("/components/messages/OrderPlaced");
        assertEquals("#/components/schemas/Order", orderPlaced.getPayloadRef());
        assertEquals("#/components/schemas/OrderPlacedHeaders", orderPlaced.getHeadersRef());
    }

    @Test
    void projectsOperationsOntoSyntheticPaths() throws Exception {
        OpenAPI openAPI = project(SHARED_EVENTS).getOpenAPI();

        Operation publish = openAPI.getPaths().get("/publishOrderPlaced").getPost();
        assertEquals("publishOrderPlaced", publish.getOperationId());
        assertEquals(List.of("orders"), publish.getTags());
        assertEquals("send", publish.getExtensions().get(AsyncApiExtensions.ACTION));
        assertEquals("#/components/schemas/Order",
            publish.getRequestBody().getContent().get("application/json").getSchema().get$ref());
        assertNotNull(publish.getResponses().get("202"));

        Operation validate = openAPI.getPaths().get("/validateOrder").getPost();
        assertEquals("receive", validate.getExtensions().get(AsyncApiExtensions.ACTION));
        assertEquals("#/components/schemas/OrderValidatedPayload",
            validate.getResponses().get("200").getContent().get("application/json").getSchema().get$ref());
        Map<?, ?> reply = (Map<?, ?>) validate.getExtensions().get(AsyncApiExtensions.REPLY);
        assertEquals("$message.header#/replyTo", reply.get("addressLocation"));

        Operation statusChanged = openAPI.getPaths().get("/onOrderStatusChanged").getPost();
        Map<?, ?> channel = (Map<?, ?>) statusChanged.getExtensions().get(AsyncApiExtensions.CHANNEL);
        assertEquals("orders.{region}.status", channel.get("address"));
        assertEquals(Boolean.TRUE, channel.get("parameterized"));

        assertEquals(List.of("payments"), openAPI.getPaths().get("/onPaymentSettled").getPost().getTags());
    }

    @Test
    void splitsOperationsWithSeveralMessages() throws Exception {
        File file = asyncApi(dir, "asyncapi.yaml",
            "channels:",
            "  accounts:",
            "    address: accounts",
            "    messages:",
            "      opened:",
            "        name: account-opened",
            "        payload:",
            "          type: object",
            "          properties:",
            "            id:",
            "              type: string",
            "      closed:",
            "        name: account-closed",
            "        payload:",
            "          type: object",
            "          properties:",
            "            reason:",
            "              type: string",
            "operations:",
            "  onAccount:",
            "    action: receive",
            "    channel:",
            "      $ref: '#/channels/accounts'");

        OpenAPI openAPI = project(file).getOpenAPI();

        assertEquals(Set.of("/onAccountAccountOpened", "/onAccountAccountClosed"), openAPI.getPaths().keySet());
        assertEquals(List.of("accounts"), openAPI.getPaths().get("/onAccountAccountOpened").getPost().getTags());
        assertEquals(Set.of("AccountOpenedPayload", "AccountClosedPayload"),
            openAPI.getComponents().getSchemas().keySet());
    }

    @Test
    void directFileReferencesAreNamedAfterTheMessage() throws Exception {
        AsyncApiTestFiles.write(dir, "lib/order.yaml",
            "type: object",
            "properties:",
            "  id:",
            "    type: string");
        File file = asyncApi(dir, "asyncapi.yaml",
            "channels:",
            "  orders:",
            "    address: orders",
            "    messages:",
            "      created:",
            "        name: OrderCreated",
            "        payload:",
            "          $ref: lib/order.yaml",
            "operations:",
            "  onOrderCreated:",
            "    action: receive",
            "    channel:",
            "      $ref: '#/channels/orders'");

        OpenAPI openAPI = project(file).getOpenAPI();

        assertEquals(Set.of("OrderCreatedPayload"), openAPI.getComponents().getSchemas().keySet(),
            "a payload referencing a file directly does not get the component name an OpenAPI spec gives it; "
                + "declare shared schemas under components.schemas and reference #/components/schemas/Order");
    }

    @Test
    void deduplicatesSchemasReachedThroughComponentKeyAndFile() throws Exception {
        AsyncApiTestFiles.write(dir, "lib/order.yaml",
            "type: object",
            "properties:",
            "  id:",
            "    type: string");
        File file = asyncApi(dir, "asyncapi.yaml",
            "channels:",
            "  orders:",
            "    address: orders",
            "    messages:",
            "      created:",
            "        name: OrderCreated",
            "        payload:",
            "          type: object",
            "          properties:",
            "            order:",
            "              $ref: lib/order.yaml",
            "      updated:",
            "        name: OrderUpdated",
            "        payload:",
            "          $ref: '#/components/schemas/Order'",
            "operations:",
            "  onOrder:",
            "    action: receive",
            "    channel:",
            "      $ref: '#/channels/orders'",
            "components:",
            "  schemas:",
            "    Order:",
            "      $ref: lib/order.yaml");

        OpenAPI openAPI = project(file).getOpenAPI();

        assertEquals(Set.of("Order", "OrderCreatedPayload"), openAPI.getComponents().getSchemas().keySet());
        assertEquals("#/components/schemas/Order", ((Schema<?>) openAPI.getComponents().getSchemas()
            .get("OrderCreatedPayload").getProperties().get("order")).get$ref());
    }

    @Test
    void warnsAboutKeywordsOpenApi30CannotExpress() throws Exception {
        File file = asyncApi(dir, "asyncapi.yaml",
            "channels:",
            "  events:",
            "    address: events",
            "    messages:",
            "      event:",
            "        name: Event",
            "        payload:",
            "          type: object",
            "          properties:",
            "            kind:",
            "              const: created",
            "operations:",
            "  onEvent:",
            "    action: receive",
            "    channel:",
            "      $ref: '#/channels/events'");

        AsyncApiProjection projection = project(file);
        assertThat(String.join("\n", projection.getWarnings()), containsString("const"));

        AsyncApiProjection projection31 = new AsyncApiOpenApiProjector().setOpenApiVersion("3.1.0")
            .project(AsyncApiLoader.load(file));
        assertFalse(String.join("\n", projection31.getWarnings()).contains("const"), projection31.getWarnings()::toString);
    }

    @Test
    void rejectsUnsupportedSchemaFormatsInOperations() {
        File file = asyncApi(dir, "asyncapi.yaml",
            "channels:",
            "  events:",
            "    address: events",
            "    messages:",
            "      event:",
            "        payload:",
            "          schemaFormat: application/vnd.apache.avro;version=1.9.0",
            "          schema:",
            "            type: record",
            "            name: Event",
            "            fields: []",
            "operations:",
            "  onEvent:",
            "    action: receive",
            "    channel:",
            "      $ref: '#/channels/events'");

        AsyncApiLoaderException e = assertThrows(AsyncApiLoaderException.class, () -> project(file));
        assertThat(e.getMessage(), containsString("avro"));
    }

    @Test
    void multiFormatComponentsAreNamedByTheirKey() throws Exception {
        AsyncApiProjection projection = new AsyncApiOpenApiProjector().setOpenApiVersion("3.1.0")
            .project(AsyncApiLoader.load(AsyncApiTestFiles.CLOUD_EVENT));
        OpenAPI openAPI = projection.getOpenAPI();
        Map<String, Schema> schemas = openAPI.getComponents().getSchemas();

        assertEquals(Set.of("ApprovalDecisionCloudEvent", "ApprovalDecisionData", "RequestType", "Scope"),
            schemas.keySet(), "file references reached through the envelope reuse the component keys");
        assertEquals("#/components/schemas/ApprovalDecisionCloudEvent",
            projection.getMessageSchemas().get("/components/messages/ApprovalDecision").getPayloadRef());
        JsonNode envelope = json(openAPI, schemas.get("ApprovalDecisionCloudEvent"));
        assertEquals("#/components/schemas/ApprovalDecisionData", envelope.at("/properties/data/$ref").asText());
        assertEquals("1.0", envelope.at("/properties/specversion/const").asText());
        assertEquals("employee", envelope.at("/if/properties/actortype/const").asText());
        assertEquals("#/components/schemas/RequestType", json(openAPI, schemas.get("ApprovalDecisionData"))
            .at("/properties/requestType/$ref").asText());
        assertTrue(projection.getWarnings().isEmpty(), projection.getWarnings()::toString);
    }

    @Test
    void warnsAboutJsonSchema2020OnOpenApi30() throws Exception {
        AsyncApiProjection projection = project(AsyncApiTestFiles.CLOUD_EVENT);
        String warnings = String.join("\n", projection.getWarnings());

        assertThat(warnings, containsString("Schema 'ApprovalDecisionCloudEvent' is JSON Schema draft-2020-12"));
        assertThat(warnings, containsString("Schema 'ApprovalDecisionCloudEvent' uses [const, if, then]"));
        assertThat(warnings, containsString("Set the projection OpenAPI version to 3.1.0"));
    }

    @Test
    void warnsAboutDraft04KeywordsOnOpenApi31() throws Exception {
        File file = asyncApi(dir, "asyncapi.yaml",
            "components:",
            "  schemas:",
            "    Legacy:",
            "      schemaFormat: application/schema+json;version=draft-04",
            "      schema:",
            "        id: http://example.com/legacy.json",
            "        type: number",
            "        minimum: 0",
            "        exclusiveMinimum: true",
            "    WithIdProperty:",
            "      schemaFormat: application/schema+json;version=draft-04",
            "      schema:",
            "        type: object",
            "        properties:",
            "          id:",
            "            type: string");

        AsyncApiProjection projection = new AsyncApiOpenApiProjector().setOpenApiVersion("3.1.0")
            .project(AsyncApiLoader.load(file));

        assertEquals(List.of("Schema 'Legacy' is JSON Schema draft-04 and uses [id, exclusiveMinimum: <boolean>], "
                + "which OpenAPI 3.1.0 reads differently. Set the projection OpenAPI version to 3.0.3."),
            projection.getWarnings());
        assertFalse(String.join("\n", project(file).getWarnings()).contains("draft-04"),
            "draft-04 bounds mean the same in OpenAPI 3.0");
    }

    @Test
    void warnsAboutNumericExclusiveBoundsOnOpenApi30() throws Exception {
        File file = asyncApi(dir, "asyncapi.yaml",
            "components:",
            "  schemas:",
            "    Amount:",
            "      schemaFormat: application/schema+yaml;version=draft-07",
            "      schema:",
            "        type: number",
            "        exclusiveMinimum: 0");

        assertThat(String.join("\n", project(file).getWarnings()),
            containsString("Schema 'Amount' uses [exclusiveMinimum: <number>]"));
    }

    @Test
    void skipsComponentsInUnsupportedFormats() throws Exception {
        File file = asyncApi(dir, "asyncapi.yaml",
            "components:",
            "  schemas:",
            "    Event:",
            "      schemaFormat: application/vnd.apache.avro;version=1.9.0",
            "      schema:",
            "        type: record",
            "        name: Event",
            "        fields: []");

        AsyncApiProjection projection = project(file);

        Map<String, Schema> schemas = projection.getOpenAPI().getComponents().getSchemas();
        assertTrue(schemas == null || !schemas.containsKey("Event"));
        assertThat(String.join("\n", projection.getWarnings()), containsString(
            "Schema 'Event' uses unsupported schema format 'application/vnd.apache.avro;version=1.9.0'"));
    }

    @Test
    void rejectsOperationsWhosePayloadComponentHasAnUnsupportedFormat() {
        File file = asyncApi(dir, "asyncapi.yaml",
            "channels:",
            "  events:",
            "    address: events",
            "    messages:",
            "      event:",
            "        payload:",
            "          $ref: '#/components/schemas/Event'",
            "operations:",
            "  onEvent:",
            "    action: receive",
            "    channel:",
            "      $ref: '#/channels/events'",
            "components:",
            "  schemas:",
            "    Event:",
            "      schemaFormat: application/vnd.apache.avro;version=1.9.0",
            "      schema:",
            "        type: record",
            "        name: Event",
            "        fields: []");

        AsyncApiLoaderException e = assertThrows(AsyncApiLoaderException.class, () -> project(file));
        assertThat(e.getMessage(), containsString("avro"));
    }

    @Test
    void rejectsUnsupportedProjectionVersions() throws AsyncApiLoaderException {
        AsyncApiDocument document = AsyncApiLoader.load(SHARED_EVENTS);
        AsyncApiOpenApiProjector projector = new AsyncApiOpenApiProjector().setOpenApiVersion("2.0");
        assertThrows(AsyncApiLoaderException.class, () -> projector.project(document));
    }

    private static AsyncApiProjection project(File file) throws AsyncApiLoaderException {
        return new AsyncApiOpenApiProjector().project(AsyncApiLoader.load(file));
    }

    private static JsonNode json(OpenAPI openAPI, Schema<?> schema) {
        return SerializerUtils.jsonMapper(openAPI).valueToTree(schema);
    }
}
