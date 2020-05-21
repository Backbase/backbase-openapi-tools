package com.backbase.oss.boat;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import io.swagger.v3.oas.models.*;
import io.swagger.v3.oas.models.examples.Example;
import io.swagger.v3.oas.models.headers.Header;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.responses.ApiResponse;

import javax.validation.constraints.NotNull;
import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

public class DirectoryExploder {

    @NotNull
    private final OpenAPI openApi;

    public DirectoryExploder(@NotNull OpenAPI openApi) {
        this.openApi = openApi;
    }

    public void serializeIntoDirectory(@NotNull File outputDir) {
        Collection<PathItem> pathItems = Optional.ofNullable(openApi.getPaths())
                .orElse(new Paths()) // empty
                .values();
        // pathItemParamsExamples: PathItem > Parameters > examples
        List<Example> pathItemParamsExamples = pathItems.stream()
                .filter(pathItem -> pathItem.getParameters() != null)
                .flatMap(pathItem -> pathItem.getParameters().stream())
                .flatMap(parameter -> parameterExamples(parameter).stream())
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
                .flatMap(parameter -> parameterExamples(parameter).stream())
                .collect(Collectors.toList());
        List<Example> opRequestBodiesExamples = ops.stream()
                .filter(operation -> operation.getRequestBody() != null)
                .filter(operation -> operation.getRequestBody().getContent() != null)
                .flatMap(operation -> contentExamples(operation.getRequestBody().getContent()).stream())
                .collect(Collectors.toList());
        List<ApiResponse> opResponses = ops.stream()
                .filter(operation -> operation.getResponses() != null)
                .flatMap(operation -> operation.getResponses().values().stream())
                .collect(Collectors.toList());
        List<Example> opResponsesContentExamples = opResponses.stream()
                .filter(apiResponse -> apiResponse.getContent() != null)
                .flatMap(apiResponse -> contentExamples(apiResponse.getContent()).stream())
                .collect(Collectors.toList());
        List<Example> opResponsesHeadersExamples = opResponses.stream()
                .filter(apiResponse -> apiResponse.getHeaders() != null)
                .flatMap(apiResponse -> apiResponse.getHeaders().values().stream())
                .flatMap(header -> headerExamples(header).stream())
                .collect(Collectors.toList());
        List<Example> opResponseLinksExamples = opResponses.stream()
                .filter(apiResponse -> apiResponse.getLinks() != null)
                .flatMap(apiResponse -> apiResponse.getLinks().values().stream())
                .filter(link -> link.getHeaders() != null)
                .flatMap(link -> link.getHeaders().values().stream())
                .flatMap(header -> headerExamples(header).stream())
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
                .flatMap(header -> headerExamples(header).stream())
                .collect(Collectors.toList());
        Collection<Example> componentsLinksExamples = Optional.ofNullable(openApi.getComponents())
                .filter(components -> components.getLinks() != null)
                .map(Components::getLinks)
                .map(Map::values)
                .orElse(Collections.emptyList())
                .stream()
                .filter(link -> link.getHeaders() != null)
                .flatMap(link -> link.getHeaders().values().stream())
                .flatMap(header -> headerExamples(header).stream())
                .collect(Collectors.toList());
        Collection<Example> componentsParamsExamples = Optional.ofNullable(openApi.getComponents())
                .filter(components -> components.getHeaders() != null)
                .map(Components::getParameters)
                .map(Map::values)
                .orElse(Collections.emptyList())
                .stream()
                .flatMap(parameter -> parameterExamples(parameter).stream())
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

        System.out.println("example values");
        ObjectMapper mapper = new ObjectMapper();
        ObjectWriter writer = mapper.writer(new DefaultPrettyPrinter());
        System.out.println(
                allExamples.stream()
                        .map(Example::getValue)
                        .filter(Objects::nonNull)
                        .map(valueObject -> {
                            try {
                                return writer.writeValueAsString(valueObject);
                            } catch (JsonProcessingException e) {
                                return "Error while processing json";
                            }
                        })
                        .collect(Collectors.joining("\n"))
        );
        System.out.println("example $ref-s");
        System.out.println(
                allExamples.stream()
                        .map(Example::get$ref)
                        .filter(Objects::nonNull)
                        .collect(Collectors.joining("\n"))
        );
    }

    @NotNull
    private List<Example> headerExamples(@NotNull Header header) {
        Collection<Example> examples = Optional.ofNullable(header.getExamples())
                .orElse(Collections.emptyMap())
                .values();
        List<Example> allExamples = new ArrayList<>(examples);
        // the following if statement can be refactored to use FP, but it'd become less readable.
        if (header.getExample() != null) {
            Example example = new Example();
            example.setValue(header.getExample());
            allExamples.add(example);
        }
        return allExamples;
    }

    @NotNull
    private List<Example> parameterExamples(@NotNull Parameter parameter) {
        Collection<Example> examples = Optional.ofNullable(parameter.getExamples())
                .orElse(Collections.emptyMap())
                .values();
        List<Example> allExamples = new ArrayList<>(examples);
        if (parameter.getExample() != null) {
            Example example = new Example();
            example.setValue(parameter.getExample());
            allExamples.add(example);
        }
        if (parameter.getContent() != null) {
            allExamples.addAll(contentExamples(parameter.getContent()));
        }
        return allExamples;
    }

    @NotNull
    private List<Example> contentExamples(@NotNull Content content) {
        return content.values().stream()
                .flatMap(mediaType -> mediaTypeExamples(mediaType).stream())
                .collect(Collectors.toList());
    }

    private List<Example> mediaTypeExamples(@NotNull MediaType mediaType) {
        Collection<Example> examples = Optional.ofNullable(mediaType.getExamples())
                .orElse(Collections.emptyMap())
                .values();
        Collection<Example> encodingHeadersExamples = Optional.ofNullable(mediaType.getEncoding())
                .orElse(Collections.emptyMap())
                .values()
                .stream()
                .filter(encoding -> encoding.getHeaders() != null)
                .flatMap(encoding -> encoding.getHeaders().values().stream())
                .flatMap(header -> headerExamples(header).stream())
                .collect(Collectors.toList());
        List<Example> allExamples = new ArrayList<>();
        allExamples.addAll(examples);
        allExamples.addAll(encodingHeadersExamples);
        return allExamples;
    }
}