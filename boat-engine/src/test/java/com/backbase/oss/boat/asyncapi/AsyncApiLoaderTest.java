package com.backbase.oss.boat.asyncapi;

import static com.backbase.oss.boat.asyncapi.AsyncApiTestFiles.SHARED_EVENTS;
import static com.backbase.oss.boat.asyncapi.AsyncApiTestFiles.asyncApi;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.backbase.oss.boat.asyncapi.model.AsyncApiDocument;
import com.backbase.oss.boat.asyncapi.model.Channel;
import com.backbase.oss.boat.asyncapi.model.Message;
import com.backbase.oss.boat.asyncapi.model.Operation;
import java.io.File;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class AsyncApiLoaderTest {

    @TempDir
    Path dir;

    @Test
    void rejectsAsyncApi2() {
        File file = AsyncApiTestFiles.write(dir, "asyncapi.yaml",
            "asyncapi: 2.6.0",
            "info:",
            "  title: Old",
            "  version: 1.0.0",
            "channels: {}");

        AsyncApiLoaderException e = assertThrows(AsyncApiLoaderException.class, () -> AsyncApiLoader.load(file));
        assertThat(e.getMessage(), containsString("2.6.0"));
        assertThat(e.getMessage(), containsString("Migrate the document to AsyncAPI 3"));
    }

    @Test
    void loadsAsyncApi31() throws Exception {
        File file = AsyncApiTestFiles.write(dir, "asyncapi.yaml",
            "asyncapi: 3.1.0",
            "info: {title: x, version: '1'}",
            "channels:",
            "  events:",
            "    messages:",
            "      event: {payload: {type: object}}",
            "operations:",
            "  onEvent:",
            "    action: receive",
            "    channel: {$ref: '#/channels/events'}");

        AsyncApiDocument document = AsyncApiLoader.load(file);

        assertEquals("3.1.0", document.getAsyncapi());
        assertSame(document.getChannels().get("events"), document.getOperations().get("onEvent").getChannel());
    }

    @Test
    void rejectsOtherVersionsAndDocuments() {
        File future = AsyncApiTestFiles.write(dir, "future.yaml", "asyncapi: 3.2.0", "info: {title: x, version: '1'}");
        assertThat(assertThrows(AsyncApiLoaderException.class, () -> AsyncApiLoader.load(future)).getMessage(),
            containsString("AsyncAPI 3.2.0 is not supported, expected 3.0.x or 3.1.x"));

        File openApi = AsyncApiTestFiles.write(dir, "openapi.yaml", "openapi: 3.0.3", "info: {title: x, version: '1'}");
        assertThat(assertThrows(AsyncApiLoaderException.class, () -> AsyncApiLoader.load(openApi)).getMessage(),
            containsString("missing 'asyncapi'"));
    }

    @Test
    void loadsSharedEvents() throws Exception {
        AsyncApiDocument document = AsyncApiLoader.load(SHARED_EVENTS);

        assertEquals("Order Events", document.getInfo().getTitle());
        assertEquals("application/json", document.getDefaultContentType());
        assertEquals("kafka", document.getServers().get("production").getProtocol());
        assertEquals(List.of("orderPlaced", "orderStatus", "paymentSettled", "orderValidation",
            "orderValidationReplies"), List.copyOf(document.getChannels().keySet()));
        assertEquals(List.of("publishOrderPlaced", "onOrderStatusChanged", "onPaymentSettled", "validateOrder"),
            List.copyOf(document.getOperations().keySet()));

        Channel orderStatus = document.getChannels().get("orderStatus");
        assertTrue(orderStatus.isParameterized());
        assertEquals(List.of("eu", "us"), orderStatus.getParameters().get("region").getEnumValues());
        assertNull(document.getChannels().get("orderValidationReplies").getAddress());

        Operation publish = document.getOperations().get("publishOrderPlaced");
        assertEquals(Operation.Action.SEND, publish.getAction());
        assertSame(document.getChannels().get("orderPlaced"), publish.getChannel());
        assertEquals(List.of("orders"), publish.getTags());
        Message orderPlaced = publish.getMessages().get(0);
        assertEquals("OrderPlaced", orderPlaced.getName());
        assertEquals("$message.header#/correlationId", orderPlaced.getCorrelationId().getLocation());
        assertSame(document.getComponentMessages().get("OrderPlaced").getPayload(), orderPlaced.getPayload(),
            "a channel message referencing a component message shares its schemas");
    }

    @Test
    void defaultsOperationMessagesToChannelMessagesAndResolvesExternalMessages() throws Exception {
        AsyncApiDocument document = AsyncApiLoader.load(SHARED_EVENTS);

        Operation onPaymentSettled = document.getOperations().get("onPaymentSettled");
        assertEquals(Operation.Action.RECEIVE, onPaymentSettled.getAction());
        Message paymentSettled = onPaymentSettled.getMessages().get(0);
        assertEquals("PaymentSettled", paymentSettled.getName());
        assertEquals("#/components/schemas/Money",
            paymentSettled.getPayload().path("properties").path("amount").path("$ref").asText(),
            "refs in the external message file are rebased onto the root document");
    }

    @Test
    void resolvesReplies() throws Exception {
        AsyncApiDocument document = AsyncApiLoader.load(SHARED_EVENTS);

        Operation validateOrder = document.getOperations().get("validateOrder");
        Operation.Reply reply = validateOrder.getReply();
        assertEquals("$message.header#/replyTo", reply.getAddressLocation());
        assertSame(document.getChannels().get("orderValidationReplies"), reply.getChannel());
        assertEquals(List.of("OrderValidated"),
            reply.getMessages().stream().map(Message::getName).collect(Collectors.toList()));
        assertEquals("application/vnd.oai.openapi;version=3.0.0",
            validateOrder.getMessages().get(0).getPayloadSchemaFormat());
    }

    @Test
    void appliesTraits() throws Exception {
        File file = asyncApi(dir, "traits.yaml",
            "channels:",
            "  events:",
            "    address: events",
            "    messages:",
            "      event:",
            "        $ref: '#/components/messages/Event'",
            "operations:",
            "  onEvent:",
            "    action: receive",
            "    channel:",
            "      $ref: '#/channels/events'",
            "    traits:",
            "      - tags:",
            "          - name: traited",
            "components:",
            "  messages:",
            "    Event:",
            "      payload:",
            "        type: string",
            "      traits:",
            "        - $ref: '#/components/messageTraits/Common'",
            "  messageTraits:",
            "    Common:",
            "      contentType: text/plain",
            "      headers:",
            "        type: object",
            "        properties:",
            "          tenant:",
            "            type: string");

        AsyncApiDocument document = AsyncApiLoader.load(file);
        Message event = document.getComponentMessages().get("Event");
        assertEquals("text/plain", event.getContentType());
        assertEquals("string", event.getHeaders().path("properties").path("tenant").path("type").asText());
        assertEquals(List.of("traited"), document.getOperations().get("onEvent").getTags());
    }
}
