package com.backbase.oss.boat.transformers;

import com.backbase.oss.boat.example.NamedExample;
import io.swagger.v3.oas.models.*;
import io.swagger.v3.oas.models.examples.Example;
import io.swagger.v3.oas.models.headers.Header;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.responses.ApiResponse;
import jakarta.validation.constraints.NotNull;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Extracts inline elements from an instance of {@link OpenAPI} into lists that can be processed further.
 */
public class OpenAPIExtractor {

    @NotNull
    private final OpenAPI openApi;

    /**
     * Constructs a new instance of this extractor.
     *
     * @param openApi the open api instance where inline elements will be extracted from.
     */
    public OpenAPIExtractor(@NotNull OpenAPI openApi) {
        this.openApi = openApi;
    }

    /**
     * Extracts inline examples from the given {@link OpenAPI} instance.
     * A non-exhaustive list of examples that are extracted:
     * <ul>
     *     <li>PathItem&gt;Parameters&gt;examples</li>
     *     <li>PathItem&gt;Operation&gt;Parameters&gt;examples</li>
     *     <li>PathItem&gt;Operation&gt;Parameters&gt;examples</li>
     *     <li>PathItem&gt;Operation&gt;RequestBody&gt;Content&gt;MediaType&gt;examples</li>
     *     <li>PathItem&gt;Operation&gt;RequestBody&gt;Content&gt;MediaType&gt;Encoding&gt;Headers&gt;Header&gt;examples</li>
     *     <li>PathItem&gt;Operation&gt;ApiResponses&gt;ApiResponse&gt;Content&gt;MediaType&gt;examples</li>
     *     <li>PathItem&gt;Operation&gt;ApiResponses&gt;ApiResponse&gt;Content&gt;Encoding&gt;Headers&gt;Header&gt;examples</li>
     *     <li>PathItem&gt;Operation&gt;ApiResponses&gt;ApiResponse&gt;Headers&gt;Header&gt;examples</li>
     *     <li>PathItem&gt;Operation&gt;ApiResponses&gt;ApiResponse&gt;Links&gt;Link&gt;Headers&gt;Header&gt;examples</li>
     *     <li>Components&gt;examples</li>
     *     <li>componentsHeadersExamples: Components&gt;Component&gt;Headers&gt;Header&gt;examples</li>
     *     <li>Components&gt;Component&gt;Links&gt;Link&gt;Headers&gt;Header&gt;examples</li>
     *     <li>Components&gt;Component&gt;Links&gt;Link&gt;Headers&gt;Header&gt;examples</li>
     * </ul>
     * This list is not exhaustive because there are cases of recursion such as with headers.
     * Single examples of elements, which are represented as unnamed fields, such as {@link Parameter#getExample()} and
     * {@link Header#getExample()} are not considered for extraction, because they're not meant to have $ref.
     *
     * @return a comprehensive list of all encountered inline examples.
     * @see ExampleExtractors#headerExamples(Header)
     */
    public List<NamedExample> extractInlineExamples() {
        Collection<PathItem> pathItems = Optional.ofNullable(openApi.getPaths())
                .orElse(new Paths()) // empty
                .values();
        // pathItemParamsExamples: PathItem&gt;Parameters&gt;examples
        List<NamedExample> pathItemParamsExamples = pathItems.stream()
                .filter(pathItem -> pathItem.getParameters() != null)
                .flatMap(pathItem -> pathItem.getParameters().stream())
                .flatMap(parameter -> ExampleExtractors.parameterExamples(parameter).stream())
                .collect(Collectors.toList());
        // opParamsExamples: PathItem&gt;Operation&gt;Parameters&gt;examples
        // opRequestBodiesExamples: PathItem&gt;Operation&gt;RequestBody&gt;Content&gt;MediaType&gt;examples
        //                        + PathItem&gt;Operation&gt;RequestBody&gt;Content&gt;MediaType&gt;Encoding&gt;Headers&gt;Header&gt;examples
        // opResponsesContentExamples: PathItem&gt;Operation&gt;ApiResponses&gt;ApiResponse&gt;Content&gt;MediaType&gt;examples
        //                        + PathItem&gt;Operation&gt;ApiResponses&gt;ApiResponse&gt;Content&gt;Encoding&gt;Headers&gt;Header&gt;examples
        // opResponsesHeadersExamples: PathItem&gt;Operation&gt;ApiResponses&gt;ApiResponse&gt;Headers&gt;Header&gt;examples
        // opResponseLinksExamples: PathItem&gt;Operation&gt;ApiResponses&gt;ApiResponse&gt;Links&gt;Link&gt;Headers&gt;Header&gt;examples
        List<Operation> ops = pathItems.stream()
                .flatMap(pathItem -> pathItem.readOperations().stream())
                .collect(Collectors.toList());
        List<NamedExample> opParamsExamples = ops.stream()
                .filter(operation -> operation.getParameters() != null)
                .flatMap(operation -> operation.getParameters().stream())
                .flatMap(parameter -> ExampleExtractors.parameterExamples(parameter).stream())
                .collect(Collectors.toList());
        List<NamedExample> opRequestBodiesExamples = ops.stream()
                .filter(operation -> operation.getRequestBody() != null)
                .filter(operation -> operation.getRequestBody().getContent() != null)
                .flatMap(operation -> ExampleExtractors.contentExamples(operation.getRequestBody().getContent()).stream())
                .collect(Collectors.toList());
        List<ApiResponse> opResponses = ops.stream()
                .filter(operation -> operation.getResponses() != null)
                .flatMap(operation -> operation.getResponses().values().stream())
                .collect(Collectors.toList());
        List<NamedExample> opResponsesContentExamples = opResponses.stream()
                .filter(apiResponse -> apiResponse.getContent() != null)
                .flatMap(apiResponse -> ExampleExtractors.contentExamples(apiResponse.getContent()).stream())
                .collect(Collectors.toList());
        List<NamedExample> opResponsesHeadersExamples = opResponses.stream()
                .filter(apiResponse -> apiResponse.getHeaders() != null)
                .flatMap(apiResponse -> apiResponse.getHeaders().entrySet().stream())
                .flatMap(headerEntry -> ExampleExtractors.headerExamples(headerEntry.getValue()).stream())
                .collect(Collectors.toList());
        List<NamedExample> opResponseLinksExamples = opResponses.stream()
                .filter(apiResponse -> apiResponse.getLinks() != null)
                .flatMap(apiResponse -> apiResponse.getLinks().values().stream())
                .filter(link -> link.getHeaders() != null)
                .flatMap(link -> link.getHeaders().entrySet().stream())
                .flatMap(headerEntry -> ExampleExtractors.headerExamples(headerEntry.getValue()).stream())
                .collect(Collectors.toList());
        // componentsExamples: Components&gt;examples
        // componentsHeadersExamples: Components&gt;Component&gt;Headers&gt;Header&gt;examples
        // componentsLinksExamples: Components&gt;Component&gt;Links&gt;Link&gt;Headers&gt;Header&gt;examples
        // componentsParamsExamples: Components&gt;Component&gt;Links&gt;Link&gt;Headers&gt;Header&gt;examples
        Collection<NamedExample> componentsExamples = Optional.ofNullable(openApi.getComponents())
                .filter(components -> components.getExamples() != null)
                .map(Components::getExamples)
                .map(Map::entrySet)
                .orElse(Collections.<String, Example>emptyMap().entrySet())
                .stream()
                .map(entry -> new NamedExample(entry.getKey(), entry.getValue()))
                .collect(Collectors.toList());
        Collection<NamedExample> componentsHeadersExamples = Optional.ofNullable(openApi.getComponents())
                .filter(components -> components.getHeaders() != null)
                .map(Components::getHeaders)
                .orElse(Collections.emptyMap())
                .entrySet()
                .stream()
                .flatMap(headerEntry -> ExampleExtractors.headerExamples(headerEntry.getValue()).stream())
                .collect(Collectors.toList());
        Collection<NamedExample> componentsLinksExamples = Optional.ofNullable(openApi.getComponents())
                .filter(components -> components.getLinks() != null)
                .map(Components::getLinks)
                .map(Map::values)
                .orElse(Collections.emptyList())
                .stream()
                .filter(link -> link.getHeaders() != null)
                .flatMap(link -> link.getHeaders().entrySet().stream())
                .flatMap(headerEntry -> ExampleExtractors.headerExamples(headerEntry.getValue()).stream())
                .collect(Collectors.toList());
        Collection<NamedExample> componentsParamsExamples = Optional.ofNullable(openApi.getComponents())
                .filter(components -> components.getHeaders() != null)
                .map(Components::getParameters)
                .map(Map::values)
                .orElse(Collections.emptyList())
                .stream()
                .flatMap(parameter -> ExampleExtractors.parameterExamples(parameter).stream())
                .collect(Collectors.toList());

        List<NamedExample> allExamples = new LinkedList<>();
        allExamples.addAll(pathItemParamsExamples);
        allExamples.addAll(opParamsExamples);
        allExamples.addAll(opRequestBodiesExamples);
        allExamples.addAll(opResponsesContentExamples);
        allExamples.addAll(opResponsesHeadersExamples);
        allExamples.addAll(opResponseLinksExamples);
        allExamples.addAll(componentsExamples);
        allExamples.addAll(componentsHeadersExamples);
        allExamples.addAll(componentsLinksExamples);
        allExamples.addAll(componentsParamsExamples);

        return allExamples.stream()
                .filter(namedExample -> namedExample.getExample().getValue() != null)
                .collect(Collectors.toList());
    }

}
