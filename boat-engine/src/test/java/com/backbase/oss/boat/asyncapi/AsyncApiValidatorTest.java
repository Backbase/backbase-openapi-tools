package com.backbase.oss.boat.asyncapi;

import static com.backbase.oss.boat.asyncapi.AsyncApiTestFiles.SHARED_EVENTS;
import static com.backbase.oss.boat.asyncapi.AsyncApiTestFiles.asyncApi;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class AsyncApiValidatorTest {

    @TempDir
    Path dir;

    private final AsyncApiValidator validator = new AsyncApiValidator();

    @Test
    void sharedEventsAreValid() {
        AsyncApiValidationResult result = validator.validate(SHARED_EVENTS);

        assertTrue(result.isValid(), () -> "errors: " + result.getErrors());
        assertFalse(result.hasWarnings(), () -> "warnings: " + result.getWarnings());
        assertEquals("Order Events", result.getTitle());
    }

    @Test
    void reportsMissingRequiredProperties() {
        File file = AsyncApiTestFiles.write(dir, "asyncapi.yaml",
            "asyncapi: 3.0.0",
            "info:",
            "  title: Missing version");

        AsyncApiValidationResult result = validator.validate(file);

        assertFalse(result.isValid());
        assertThat(result.getErrors(), hasItem("$.info: required property 'version' not found"));
    }

    @Test
    void reportsInvalidTypes() {
        File file = asyncApi(dir, "asyncapi.yaml",
            "channels:",
            "  events:",
            "    address: 42");

        AsyncApiValidationResult result = validator.validate(file);

        assertFalse(result.isValid());
        assertThat(String.join("\n", result.getErrors()), containsString("$.channels.events.address: integer found"));
    }

    @Test
    void validatesAgainstTheSchemaOfTheDocumentVersion() {
        // ROS 2 bindings were added in AsyncAPI 3.1.
        String[] lines = {
            "info: {title: x, version: '1'}",
            "servers:",
            "  robot:",
            "    host: localhost",
            "    protocol: ros2",
            "    bindings:",
            "      ros2: {domainId: 0}"};
        String[] v30 = new String[lines.length + 1];
        String[] v31 = new String[lines.length + 1];
        v30[0] = "asyncapi: 3.0.0";
        v31[0] = "asyncapi: 3.1.0";
        System.arraycopy(lines, 0, v30, 1, lines.length);
        System.arraycopy(lines, 0, v31, 1, lines.length);

        AsyncApiValidationResult result30 = validator.validate(AsyncApiTestFiles.write(dir, "v30.yaml", v30));
        AsyncApiValidationResult result31 = validator.validate(AsyncApiTestFiles.write(dir, "v31.yaml", v31));

        assertThat(String.join("\n", result30.getErrors()),
            containsString("$.servers.robot.bindings: property 'ros2' is not defined in the schema"));
        assertTrue(result31.isValid(), () -> "errors: " + result31.getErrors());
    }

    @Test
    void reportsVersionErrors() {
        File file = AsyncApiTestFiles.write(dir, "asyncapi.yaml", "asyncapi: 2.6.0", "info: {title: x, version: '1'}");

        AsyncApiValidationResult result = validator.validate(file);

        assertFalse(result.isValid());
        assertEquals(1, result.getErrors().size());
        assertThat(result.getErrors().get(0), containsString("2.6.0"));
    }

    @Test
    void reportsInvalidReferences() {
        File file = asyncApi(dir, "asyncapi.yaml",
            "channels:",
            "  orders:",
            "    address: 'orders.{region}'",
            "    messages:",
            "      created:",
            "        payload:",
            "          type: string",
            "  payments:",
            "    address: payments",
            "    messages:",
            "      settled:",
            "        payload:",
            "          type: string",
            "operations:",
            "  onOrder:",
            "    action: receive",
            "    channel:",
            "      $ref: '#/channels/orders'",
            "    messages:",
            "      - $ref: '#/channels/payments/messages/settled'",
            "    reply:",
            "      address:",
            "        location: 'replyTo'",
            "      channel:",
            "        $ref: '#/channels/payments'");

        AsyncApiValidationResult result = validator.validate(file);

        assertFalse(result.isValid());
        assertThat(result.getErrors(), hasItem(
            "Channel 'orders' address parameter 'region' is not declared under 'parameters'"));
        assertThat(result.getErrors(), hasItem("Operation 'onOrder' message 'settled' must reference a message of "
            + "channel 'orders' (#/channels/orders/messages/...)"));
        assertThat(String.join("\n", result.getErrors()), containsString(
            "address location 'replyTo' must be a runtime expression"));
    }

    @Test
    void warnsAboutUnsupportedSchemaFormats() {
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

        AsyncApiValidationResult result = validator.validate(file);

        assertTrue(result.isValid(), () -> "errors: " + result.getErrors());
        assertThat(String.join("\n", result.getWarnings()), containsString("not supported for code generation"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"draft-04", "draft-06", "draft-07", "draft-2019-09", "draft-2020-12"})
    void acceptsJsonSchemaDrafts(String draft) {
        File file = asyncApi(dir, "asyncapi.yaml",
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
            "components:",
            "  schemas:",
            "    Event:",
            "      schemaFormat: application/schema+yaml;version=" + draft,
            "      schema:",
            "        type: object",
            "        properties:",
            "          id:",
            "            type: string",
            "  messages:",
            "    Event:",
            "      headers:",
            "        schemaFormat: application/schema+json;version=" + draft,
            "        schema:",
            "          type: object",
            "      payload:",
            "        $ref: '#/components/schemas/Event'");

        AsyncApiValidationResult result = validator.validate(file);

        assertTrue(result.isValid(), () -> "errors: " + result.getErrors());
        assertFalse(result.hasWarnings(), () -> "warnings: " + result.getWarnings());
    }

    @Test
    void acceptsMultiFormatComponentsInSeparateFiles() {
        AsyncApiValidationResult result = validator.validate(AsyncApiTestFiles.CLOUD_EVENT);

        assertTrue(result.isValid(), () -> "errors: " + result.getErrors());
        assertFalse(result.hasWarnings(), () -> "warnings: " + result.getWarnings());
    }

    @Test
    void acceptsSeveralEventsWithChannelExtensions() {
        AsyncApiValidationResult result = validator.validate(AsyncApiTestFiles.CLOUD_EVENTS);

        assertTrue(result.isValid(), () -> "errors: " + result.getErrors());
        assertFalse(result.hasWarnings(), () -> "warnings: " + result.getWarnings());
        assertEquals("Adoption Events", result.getTitle());
    }

    @Test
    void warnsAboutUnsupportedComponentSchemaFormats() {
        File file = asyncApi(dir, "asyncapi.yaml",
            "components:",
            "  schemas:",
            "    Event:",
            "      schemaFormat: application/vnd.apache.avro;version=1.9.0",
            "      schema:",
            "        type: record",
            "        name: Event",
            "        fields: []");

        AsyncApiValidationResult result = validator.validate(file);

        assertTrue(result.isValid(), () -> "errors: " + result.getErrors());
        assertEquals(List.of("Schema 'Event' uses schema format 'application/vnd.apache.avro;version=1.9.0', which "
            + "is not supported for code generation"), result.getWarnings());
    }
}
