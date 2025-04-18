package com.backbase.oss.boat.transformers;

import com.backbase.oss.boat.loader.OpenAPILoader;
import com.backbase.oss.boat.loader.OpenAPILoaderException;
import com.backbase.oss.boat.serializer.SerializerUtils;
import com.backbase.oss.boat.transformers.bundler.BoatCache;
import com.backbase.oss.boat.transformers.bundler.ExamplesProcessor;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.swagger.v3.core.util.Yaml;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.examples.Example;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import io.swagger.v3.parser.models.RefFormat;
import lombok.extern.slf4j.Slf4j;
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Collections;
import java.util.Map;
import java.util.function.Function;

import static org.apache.commons.lang3.StringUtils.substringAfterLast;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.*;

@Slf4j
class BundlerTests {

    private static final String APPLICATION_JSON = "application/json";
    private static final org.hamcrest.Matcher<java.lang.String> isComponentExample = new BaseMatcher<String>() {

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
    void testBoatCache() throws OpenAPILoaderException {
        String file = Paths.get("src/test/resources/openapi/bundler-examples-test-api/openapi.yaml").toAbsolutePath().toString();
        String spec = System.getProperty("spec", file);
        File input = new File(spec);
        OpenAPI openAPI = OpenAPILoader.load(input);

        BoatCache boatCache = new BoatCache(openAPI, null, spec, new ExamplesProcessor(openAPI, input.toURI().toString()));

        try{
            boatCache.loadRef("doesn't exist", RefFormat.RELATIVE, Example.class);
            fail("expects TransformerException to be thrown");
        }catch (TransformerException e){
            assertEquals("Reference: doesn't exist cannot be loaded", e.getMessage());
        }

    }

    @Test
    void testBundleExamples() throws OpenAPILoaderException, IOException {
        String file = Paths.get("src/test/resources/openapi/bundler-examples-test-api/openapi.yaml").toAbsolutePath().toString();
        String spec = System.getProperty("spec", file);
        File input = new File(spec);
        OpenAPI openAPI = OpenAPILoader.load(input);
        OpenAPI openAPIUnproccessed = openAPI;

        new Bundler(input).transform(openAPI, Collections.emptyMap());
        log.info(Yaml.pretty(openAPI.getComponents().getExamples()));
        assertEquals(openAPIUnproccessed, openAPI);
    }

    @Test
    void testBundleHttp() throws OpenAPILoaderException, IOException {

        String url = "https://raw.githubusercontent.com/OAI/OpenAPI-Specification/refs/heads/main/_archive_/schemas/v3.0/pass//petstore.yaml";
        OpenAPI openAPI = OpenAPILoader.load(url);
        OpenAPI openAPIUnproccessed = openAPI;

        new Bundler(url).transform(openAPI, Collections.emptyMap());
        log.info(Yaml.pretty(openAPI.getComponents().getExamples()));
        assertEquals(openAPIUnproccessed, openAPI);
    }

    @Test
    void testBundleNonExistingFiles() throws OpenAPILoaderException, IOException {
        String inputUri = getClass().getResource("/openapi/bundler-examples-test-api/openapi-example-not-found.yaml").toString();
        String spec = System.getProperty("spec", inputUri);
        OpenAPI openAPI = OpenAPILoader.load(inputUri);
        Bundler bundler = new Bundler(inputUri);
        try {
            bundler.transform(openAPI, Collections.EMPTY_MAP);
            fail("Expected TransformerException");
        } catch (TransformerException e){
            assertTrue(e.getMessage().startsWith("Unable to fix inline examples from file"));
            assertTrue(e.getMessage().endsWith("notexist.json"));
        }

    }

    @Test
    void testExamplesProcessor() throws OpenAPILoaderException, URISyntaxException {
        String inputUri = getClass().getResource("/openapi/bundler-examples-test-api/openapi.yaml").toString();
        String spec = System.getProperty("spec", inputUri);
        OpenAPI openAPI = OpenAPILoader.load(spec);
        OpenAPI openAPIUnproccessed = openAPI;
        new ExamplesProcessor(openAPI,spec).processExamples();
        assertEquals(openAPIUnproccessed, openAPI);
    }

    @Test
    void testExamplesProcessorComponentError() throws OpenAPILoaderException {
        String inputUri = getClass().getResource("/openapi/bundler-examples-test-api/openapi-component-example-error.yaml").toString();
        String spec = System.getProperty("spec", inputUri);;
        OpenAPI openAPI = OpenAPILoader.load(spec);
        ExamplesProcessor examplesProcessor = new ExamplesProcessor(openAPI,spec);
        try {
            examplesProcessor.processExamples();
            fail("Expected TransformerException");
        }catch (TransformerException e){
            assertEquals("Failed to process example content for ExampleHolder{name='example-in-components', ref=null}",e.getMessage());
        }
    }


    @Test
    void testBundleApi() throws OpenAPILoaderException, IOException {
        String inputUri = getClass().getResource("/openapi/bundler-examples-test-api/openapi.yaml").toString();
        String spec = System.getProperty("spec", inputUri);

        OpenAPI openAPI = OpenAPILoader.load(inputUri);

        OpenAPI transform = new Bundler(inputUri).transform(openAPI, Collections.emptyMap());

        log.debug(Yaml.pretty(openAPI));

        assertThat("Single inline example is replaced with relative ref (get)",
            singleExampleNode(openAPI, "/users", PathItem::getGet, "200", APPLICATION_JSON).get("$ref").asText(),
            isComponentExample);
        assertThat("Single inline example is replaced with relative ref (put)",
            singleExampleNode(openAPI, "/users", PathItem::getPut, "200", APPLICATION_JSON).get("$ref").asText(),
            isComponentExample);
        assertThat("Single inline example is replaced with relative ref (post)",
            singleExampleNode(openAPI, "/users", PathItem::getPost, "200", APPLICATION_JSON).get("$ref").asText(),
            isComponentExample);
        assertThat("Single inline example is replaced with relative ref (patch)",
            singleExampleNode(openAPI, "/users", PathItem::getPatch, "200", APPLICATION_JSON).get("$ref").asText(),
            isComponentExample);
        assertThat("Component example without ref is left alone.",
            openAPI.getComponents().getExamples().get("example-in-components-1").getSummary(),
            is("component-examples with example - should be left alone"));

        // actual input is not valid... when there is a ref no other properties should be set - still allow it.
        assertThat("Component example that duplicates a inline example, is left alone. But the summary is removed",
            openAPI.getComponents().getExamples().get("example-number-one").getSummary(), is("example-number-one"));


        assertComponentExample(openAPI, "/users", PathItem::getPost, "400");

        assertThat("The deep linked example is in /components/examples",
            openAPI.getComponents().getExamples().get("lib-bad-request-validation-error"), notNullValue());

        assertThat("Single bad example is fixed",
            singleExampleNode(openAPI, "/users", PathItem::getPut, "400", APPLICATION_JSON).get("$ref").asText(),
            isComponentExample);

        assertThat("Multiple bad examples are fixed",
            openAPI.getPaths().get("/users").getPut().getResponses().get("401").getContent().get(APPLICATION_JSON)
                .getExamples().get("named-bad-example").get$ref(), isComponentExample);

        assertComponentExample(openAPI, "/users", PathItem::getPut, "403");

        Example exampleNoThree = openAPI.getPaths().get("/multi-users").getPost().getRequestBody().getContent()
            .get(APPLICATION_JSON).getExamples().get("example-number-three");
        assertThat("value.$ref is cleaned up", exampleNoThree.get$ref(),
            is("#/components/examples/example-number-three"));
        assertThat("value.$ref is cleaned up", exampleNoThree.getValue(), nullValue());

    }

    /**
     * Asserts that the specified path/operation/responseCode points to a component/response that has a ref to an
     * example and validates that this example exists in components/examples.
     * @param openAPI the OpenAPI object
     * @param path the path of the endpoint
     * @param operationGetter the getter function to retrieve the operation from the path item
     * @param responseCode the response code to check
     */
    private void assertComponentExample(OpenAPI openAPI, String path, Function<PathItem, Operation> operationGetter,
                                        String responseCode) {
        String responseRef = operationGetter.apply(openAPI.getPaths().get(path)).getResponses().get(responseCode).get$ref();
        assertThat("Response is referenced", responseRef, notNullValue());
        ApiResponse response = openAPI.getComponents().getResponses().get(substringAfterLast(responseRef, "/"));
        assertThat("Response " + responseRef + " is inlined in components/response", response, notNullValue());
        Object exampleRef = ((Map)response.getContent().get(APPLICATION_JSON).getExample()).get("$ref");
        assertThat("The component has a ref to the actual example", exampleRef, notNullValue());
        assertThat("400 response is inlined in components/response",
                openAPI.getComponents().getExamples().containsKey(
                        substringAfterLast(String.valueOf(exampleRef), "/")), is(true));
    }

    private ObjectNode singleExampleNode(OpenAPI openAPI, String path, Function<PathItem, Operation> operation,
                                         String response, String contentType) {
        return (ObjectNode) singleExample(openAPI, path, operation, response, contentType);
    }

    private Map singleExampleMap(OpenAPI openAPI, String path, Function<PathItem, Operation> operation,
                                 String response, String contentType) {
        return (Map) singleExample(openAPI, path, operation, response, contentType);
    }

    private Object singleExample(OpenAPI openAPI, String path, Function<PathItem, Operation> operation,
                                 String response, String contentType) {
        Operation apply = operation.apply(openAPI.getPaths().get(path));
        ApiResponses responses = apply.getResponses();
        ApiResponse apiResponse = responses.get(response);
        Content content = apiResponse.getContent();
        if (content == null) {
            return Map.of();
        }
        MediaType mediaType = content.get(contentType);
        return mediaType.getExample();
    }

    // @Test - not really a test.
    void testDraftsApi() throws OpenAPILoaderException, IOException {

        File input = new File("/Users/jasper/git/jasper/collect-specs/projects/payment-order-a2a-id-provider-spec/src/main/resources/payment-order-a2a-id-provider-service-api-v1.yaml");
        OpenAPI openAPI = OpenAPILoader.load(input);

        new Bundler(input).transform(openAPI, Collections.emptyMap());

        File output = new File("/Users/jasper/git/tp/backbase-openapi-tools/boat-engine/target/out.yaml");
        output.delete();
        Files.write(output.toPath(), SerializerUtils.toYamlString(openAPI).getBytes(StandardCharsets.UTF_8), StandardOpenOption.CREATE);
    }


}
