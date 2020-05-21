package com.backbase.oss.boat.transformers;

import io.swagger.v3.oas.models.*;
import io.swagger.v3.oas.models.examples.Example;
import io.swagger.v3.oas.models.responses.ApiResponse;

import javax.validation.constraints.NotNull;
import java.util.*;
import java.util.stream.Collectors;

public class OpenAPIExtractor {

    @NotNull
    private final OpenAPI openApi;

    public OpenAPIExtractor(@NotNull OpenAPI openApi) {
        this.openApi = openApi;
    }

    public List<Example> extractExamples() {
        Collection<PathItem> pathItems = Optional.ofNullable(openApi.getPaths())
                .orElse(new Paths()) // empty
                .values();
        // pathItemParamsExamples: PathItem > Parameters > examples
        List<Example> pathItemParamsExamples = pathItems.stream()
                .filter(pathItem -> pathItem.getParameters() != null)
                .flatMap(pathItem -> pathItem.getParameters().stream())
                .flatMap(parameter -> CommonExtractors.parameterExamples(parameter).stream())
                .collect(Collectors.toList());
        // opParamsExamples: PathItem > Operation > Parameters > examples
        // opRequestBodiesExamples: PathItem > Operation > RequestBody > Content > MediaType > examples
        //                        + PathItem > Operation > RequestBody > Content > MediaType > Encoding > Headers > Header > examples
        // opResponsesContentExamples: PathItem > Operation > ApiResponses > ApiResponse > Content > MediaType > examples
        //                        + PathItem > Operation > ApiResponses > ApiResponse > Content > Encoding > Headers > Header > examples
        // opResponsesHeadersExamples: PathItem > Operation > ApiResponses > ApiResponse > Headers > Header > examples
        // opResponseLinksExamples: PathItem > Operation > ApiResponses > ApiResponse > Links > Link > Headers > Header > examples
        List<Operation> ops = pathItems.stream()
                .flatMap(pathItem -> pathItem.readOperations().stream())
                .collect(Collectors.toList());
        List<Example> opParamsExamples = ops.stream()
                .filter(operation -> operation.getParameters() != null)
                .flatMap(operation -> operation.getParameters().stream())
                .flatMap(parameter -> CommonExtractors.parameterExamples(parameter).stream())
                .collect(Collectors.toList());
        List<Example> opRequestBodiesExamples = ops.stream()
                .filter(operation -> operation.getRequestBody() != null)
                .filter(operation -> operation.getRequestBody().getContent() != null)
                .flatMap(operation -> CommonExtractors.contentExamples(operation.getRequestBody().getContent()).stream())
                .collect(Collectors.toList());
        List<ApiResponse> opResponses = ops.stream()
                .filter(operation -> operation.getResponses() != null)
                .flatMap(operation -> operation.getResponses().values().stream())
                .collect(Collectors.toList());
        List<Example> opResponsesContentExamples = opResponses.stream()
                .filter(apiResponse -> apiResponse.getContent() != null)
                .flatMap(apiResponse -> CommonExtractors.contentExamples(apiResponse.getContent()).stream())
                .collect(Collectors.toList());
        List<Example> opResponsesHeadersExamples = opResponses.stream()
                .filter(apiResponse -> apiResponse.getHeaders() != null)
                .flatMap(apiResponse -> apiResponse.getHeaders().values().stream())
                .flatMap(header -> CommonExtractors.headerExamples(header).stream())
                .collect(Collectors.toList());
        List<Example> opResponseLinksExamples = opResponses.stream()
                .filter(apiResponse -> apiResponse.getLinks() != null)
                .flatMap(apiResponse -> apiResponse.getLinks().values().stream())
                .filter(link -> link.getHeaders() != null)
                .flatMap(link -> link.getHeaders().values().stream())
                .flatMap(header -> CommonExtractors.headerExamples(header).stream())
                .collect(Collectors.toList());
        // componentsExamples: Components > examples
        // componentsHeadersExamples: Components > Component > Headers > Header > examples
        // componentsLinksExamples: Components > Component > Links > Link > Headers > Header > examples
        // componentsParamsExamples: Components > Component > Links > Link > Headers > Header > examples
        Collection<Example> componentsExamples = Optional.ofNullable(openApi.getComponents())
                .filter(components -> components.getExamples() != null)
                .map(Components::getExamples)
                .map(Map::values)
                .orElse(Collections.emptyList());
        Collection<Example> componentsHeadersExamples = Optional.ofNullable(openApi.getComponents())
                .filter(components -> components.getHeaders() != null)
                .map(Components::getHeaders)
                .map(Map::values)
                .orElse(Collections.emptyList())
                .stream()
                .flatMap(header -> CommonExtractors.headerExamples(header).stream())
                .collect(Collectors.toList());
        Collection<Example> componentsLinksExamples = Optional.ofNullable(openApi.getComponents())
                .filter(components -> components.getLinks() != null)
                .map(Components::getLinks)
                .map(Map::values)
                .orElse(Collections.emptyList())
                .stream()
                .filter(link -> link.getHeaders() != null)
                .flatMap(link -> link.getHeaders().values().stream())
                .flatMap(header -> CommonExtractors.headerExamples(header).stream())
                .collect(Collectors.toList());
        Collection<Example> componentsParamsExamples = Optional.ofNullable(openApi.getComponents())
                .filter(components -> components.getHeaders() != null)
                .map(Components::getParameters)
                .map(Map::values)
                .orElse(Collections.emptyList())
                .stream()
                .flatMap(parameter -> CommonExtractors.parameterExamples(parameter).stream())
                .collect(Collectors.toList());

        List<Example> allExamples = new LinkedList<>();
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

        return allExamples;
    }

}
