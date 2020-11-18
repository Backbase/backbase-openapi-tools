package com.backbase.oss.boat.transformers;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.nullValue;

import com.backbase.oss.boat.loader.OpenAPILoader;
import com.backbase.oss.boat.loader.OpenAPILoaderException;
import com.backbase.oss.boat.serializer.SerializerUtils;
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
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.Collections;
import java.util.Map;
import java.util.function.Function;
import lombok.extern.slf4j.Slf4j;
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.junit.Test;

@Slf4j
public class BundlerTest {

    private static final String APPLICATION_JSON = "application/json";
    private static org.hamcrest.Matcher<java.lang.String> isComponentExample = new BaseMatcher<String>() {

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
    public void testBundleExamples() throws OpenAPILoaderException, IOException {
        String file = getClass().getResource("/openapi/bundler-examples-test-api/openapi.yaml").getFile();
        String spec = System.getProperty("spec", file);
        File input = new File(spec);
        OpenAPI openAPI = OpenAPILoader.load(input);

        new Bundler(input).transform(openAPI, Collections.emptyMap());
        log.info(Yaml.pretty(openAPI.getComponents().getExamples()));
    }

    @Test
    public void testBundleApi() throws OpenAPILoaderException, IOException {
        String file = getClass().getResource("/openapi/bundler-examples-test-api/openapi.yaml").getFile();
        String spec = System.getProperty("spec", file);
        File input = new File(spec);

        OpenAPI openAPI = OpenAPILoader.load(input);

        new Bundler(input).transform(openAPI, Collections.emptyMap());

        log.info(Yaml.pretty(openAPI));

        assertThat("Single inline example is replaced with relative ref",
            singleExampleNode(openAPI, "/users", PathItem::getGet, "200", APPLICATION_JSON).get("$ref").asText(),
            isComponentExample);
        assertThat("Component example without ref is left alone.",
            openAPI.getComponents().getExamples().get("example-in-components-1").getSummary(),
            is("component-examples with example - should be left alone"));


        // actual input is not valid... when there is a ref no other properties should be set - still allow it.
        assertThat("Component example that duplicates a inline example, is left alone. But the summary is removed",
            openAPI.getComponents().getExamples().get("example-number-one").getSummary(), is("example-number-one"));


        assertThat("Deep linked examples are dereferenced.",
            singleExampleMap(openAPI, "/users", PathItem::getPost, "400", APPLICATION_JSON).get("$ref").toString(),
            isComponentExample);
        assertThat("The deep linked example is in /components/examples",
            openAPI.getComponents().getExamples().get("lib-bad-request-validation-error"), notNullValue());

        assertThat("Single bad example is fixed",
            singleExampleNode(openAPI, "/users", PathItem::getPut, "400", APPLICATION_JSON).get("$ref").asText(),
            isComponentExample);

        assertThat("Multiple bad examples are fixed",
            openAPI.getPaths().get("/users").getPut().getResponses().get("401").getContent().get(APPLICATION_JSON)
                .getExamples().get("named-bad-example").get$ref(), isComponentExample);

        assertThat("Relative path works",
            openAPI.getPaths().get("/users").getPut().getResponses().get("403").getContent().get(APPLICATION_JSON)
                .getExample(), notNullValue());

        Example exampleNoThree = openAPI.getPaths().get("/multi-users").getPost().getRequestBody().getContent()
            .get(APPLICATION_JSON).getExamples().get("example-number-three");
        assertThat("value.$ref is cleaned up", exampleNoThree.get$ref(),
            is("#/components/examples/example-number-three"));
        assertThat("value.$ref is cleaned up", exampleNoThree.getValue(), nullValue());

        log.debug(Yaml.pretty(openAPI));
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
        MediaType mediaType = content.get(contentType);
        return mediaType
            .getExample();
    }

    // @Test - not really a test.
    public void testDraftsApi() throws OpenAPILoaderException, IOException {

        File input = new File("/Users/jasper/git/jasper/collect-specs/projects/payment-order-a2a-id-provider-spec/src/main/resources/payment-order-a2a-id-provider-service-api-v1.yaml");
        OpenAPI openAPI = OpenAPILoader.load(input);

        new Bundler(input).transform(openAPI, Collections.emptyMap());

        File output = new File("/Users/jasper/git/tp/backbase-openapi-tools/boat-engine/target/out.yaml");
        output.delete();
        Files.write(output.toPath(), SerializerUtils.toYamlString(openAPI).getBytes(), StandardOpenOption.CREATE);
    }

}
