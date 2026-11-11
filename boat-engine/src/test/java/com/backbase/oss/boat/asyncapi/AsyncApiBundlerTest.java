package com.backbase.oss.boat.asyncapi;

import static com.backbase.oss.boat.asyncapi.AsyncApiTestFiles.SHARED_EVENTS;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.backbase.oss.boat.asyncapi.model.AsyncApiDocument;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class AsyncApiBundlerTest {

    @TempDir
    Path dir;

    @Test
    void bundlesIntoSelfContainedValidDocument() throws Exception {
        JsonNode bundled = new AsyncApiBundler().bundle(SHARED_EVENTS);
        String yaml = AsyncApiBundler.toYaml(bundled);

        assertThat(yaml, not(containsString("$ref: lib/")));
        assertThat(yaml, not(containsString("messages/payment-settled.yaml")));
        assertEquals("#/components/schemas/PaymentSettledPayload",
            bundled.at("/channels/paymentSettled/messages/paymentSettled/payload/$ref").asText());
        assertEquals("#/components/schemas/Order",
            bundled.at("/components/messages/ValidateOrder/payload/schema/$ref").asText());
        assertEquals("#/components/schemas/OrderPlacedHeaders",
            bundled.at("/components/messages/OrderPlaced/headers/$ref").asText());
        assertEquals("#/components/messages/OrderPlaced",
            bundled.at("/channels/orderPlaced/messages/orderPlaced/$ref").asText());

        File output = AsyncApiTestFiles.write(dir, "bundled/asyncapi.yaml", yaml);
        AsyncApiValidationResult validation = new AsyncApiValidator().validate(output);
        assertTrue(validation.isValid(), () -> "errors: " + validation.getErrors());
    }

    @Test
    void roundTripsToTheSameProjection() throws Exception {
        File output = AsyncApiTestFiles.write(dir, "bundled/asyncapi.yaml",
            AsyncApiBundler.toYaml(new AsyncApiBundler().bundle(SHARED_EVENTS)));

        AsyncApiDocument original = AsyncApiLoader.load(SHARED_EVENTS);
        AsyncApiDocument bundled = AsyncApiLoader.load(output);

        assertEquals(schemaNames(original), schemaNames(bundled));
        assertEquals(new ArrayList<>(original.getOperations().keySet()),
            new ArrayList<>(bundled.getOperations().keySet()));
    }

    @Test
    void keepsTheSchemaFormatOfComponents() throws Exception {
        JsonNode bundled = new AsyncApiBundler().setOpenApiVersion("3.1.0")
            .bundle(AsyncApiTestFiles.CLOUD_EVENT);

        JsonNode envelope = bundled.at("/components/schemas/AdoptionDecisionCloudEvent");
        assertEquals("application/schema+yaml;version=draft-2020-12", envelope.path("schemaFormat").asText());
        assertEquals("#/components/schemas/AdoptionDecisionData", envelope.at("/schema/properties/data/$ref").asText());
        assertEquals("1.0", envelope.at("/schema/properties/specversion/const").asText());

        File output = AsyncApiTestFiles.write(dir, "bundled/asyncapi.yaml", AsyncApiBundler.toYaml(bundled));
        AsyncApiValidationResult validation = new AsyncApiValidator().validate(output);
        assertTrue(validation.isValid(), () -> "errors: " + validation.getErrors());
        AsyncApiDocument reloaded = AsyncApiLoader.load(output);
        assertEquals("application/schema+yaml;version=draft-2020-12",
            reloaded.getComponentSchemaFormats().get("Pet"));
    }

    @Test
    void bundlesSeveralEventsWithSharedSchemasAndChannelExtensions() throws Exception {
        JsonNode bundled = new AsyncApiBundler().setOpenApiVersion("3.1.0")
            .bundle(AsyncApiTestFiles.CLOUD_EVENTS);

        assertEquals("store", bundled.at("/channels/adoptionRequests/x-producers/0").asText());
        assertEquals("shelter", bundled.at("/channels/adoptionRequests/x-consumers/0").asText());
        assertEquals("shelter", bundled.at("/channels/adoptionDecision/x-producers/0").asText());
        assertEquals("store", bundled.at("/channels/adoptionDecision/x-consumers/0").asText());
        assertEquals(Set.of("AdoptionRequestCloudEvent", "AdoptionRequestData", "AdoptionDecisionCloudEvent",
            "AdoptionDecisionData", "PetType", "Pet"), fieldNames(bundled.at("/components/schemas")));
        for (String data : List.of("AdoptionRequestData", "AdoptionDecisionData")) {
            JsonNode properties = bundled.at("/components/schemas/" + data + "/schema/properties");
            assertEquals("#/components/schemas/Pet", properties.at("/pet/$ref").asText(), data);
            assertEquals("#/components/schemas/PetType", properties.at("/petType/$ref").asText(), data);
        }

        File output = AsyncApiTestFiles.write(dir, "bundled/adoption-events-v1.yaml", AsyncApiBundler.toYaml(bundled));
        AsyncApiValidationResult validation = new AsyncApiValidator().validate(output);
        assertTrue(validation.isValid(), () -> "errors: " + validation.getErrors());
    }

    @Test
    void keepsStringsThatLookLikeNumbersQuoted() throws Exception {
        File file = AsyncApiTestFiles.asyncApi(dir, "asyncapi.yaml",
            "components:",
            "  schemas:",
            "    Event:",
            "      schemaFormat: application/schema+yaml;version=draft-2020-12",
            "      schema:",
            "        type: object",
            "        properties:",
            "          specversion:",
            "            type: string",
            "            const: '1.0'",
            "          code:",
            "            type: string",
            "            enum: ['1', '2']",
            "            examples: ['123']",
            "            maxLength: 3");

        String yaml = AsyncApiBundler.toYaml(new AsyncApiBundler().setOpenApiVersion("3.1.0").bundle(file));
        JsonNode properties = new ObjectMapper(new YAMLFactory()).readTree(yaml)
            .at("/components/schemas/Event/schema/properties");

        assertTrue(properties.at("/specversion/const").isTextual(), yaml);
        assertEquals("1.0", properties.at("/specversion/const").asText());
        assertTrue(properties.at("/code/enum/0").isTextual(), yaml);
        assertTrue(properties.at("/code/examples/0").isTextual(), yaml);
        assertTrue(properties.at("/code/maxLength").isNumber(), yaml);
    }

    @Test
    void copiesComponentsInUnsupportedFormats() throws Exception {
        File file = AsyncApiTestFiles.asyncApi(dir, "asyncapi.yaml",
            "components:",
            "  schemas:",
            "    Event:",
            "      schemaFormat: application/vnd.apache.avro;version=1.9.0",
            "      schema:",
            "        $ref: event.avsc");
        AsyncApiTestFiles.write(dir, "event.avsc", "{\"type\": \"record\", \"name\": \"Event\", \"fields\": []}");

        JsonNode bundled = new AsyncApiBundler().bundle(file);

        assertEquals("application/vnd.apache.avro;version=1.9.0",
            bundled.at("/components/schemas/Event/schemaFormat").asText());
        assertEquals("record", bundled.at("/components/schemas/Event/schema/type").asText());
    }

    @Test
    void setsVersionAndRemovesExtensions() throws Exception {
        File file = AsyncApiTestFiles.asyncApi(dir, "asyncapi.yaml",
            "  x-internal: true",
            "channels:",
            "  events:",
            "    address: events",
            "    x-audience: internal",
            "    messages:",
            "      event:",
            "        payload:",
            "          type: string");

        JsonNode bundled = new AsyncApiBundler()
            .setVersion("2.1.0")
            .setRemoveExtensions(List.of("internal", "x-audience"))
            .bundle(file);

        assertEquals("2.1.0", bundled.at("/info/version").asText());
        assertFalse(bundled.at("/info").has("x-internal"));
        assertFalse(bundled.at("/channels/events").has("x-audience"));
    }

    private static Set<String> fieldNames(JsonNode node) {
        Set<String> names = new TreeSet<>();
        node.fieldNames().forEachRemaining(names::add);
        return names;
    }

    private static Set<String> schemaNames(AsyncApiDocument document) throws AsyncApiLoaderException {
        return new TreeSet<>(new AsyncApiOpenApiProjector().project(document).getOpenAPI()
            .getComponents().getSchemas().keySet());
    }
}
