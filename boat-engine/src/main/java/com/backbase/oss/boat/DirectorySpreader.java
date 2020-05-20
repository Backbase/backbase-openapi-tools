package com.backbase.oss.boat;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.google.common.collect.Lists;
import com.google.common.collect.Streams;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.examples.Example;

import java.io.File;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class DirectorySpreader {

    private final OpenAPI openApi;

    public DirectorySpreader(OpenAPI openApi) {
        this.openApi = openApi;
    }

    public void serializeIntoDirectory(File outputDir) {
        // e1: PathItem > Operation > Parameters > examples
        // e2: PathItem > Operation > RequestBody > Content > MediaType > examples
        // e3: PathItem > Operation > ApiResponses > ApiResponse > Content > MediaType > examples
        List<Operation> ops = openApi.getPaths().values().stream()
                .flatMap(pathItem -> pathItem.readOperations().stream())
                .collect(Collectors.toList());
        List<Example> e1 = ops.stream()
                .filter(operation -> operation.getParameters() != null)
                .flatMap(operation -> operation.getParameters().stream())
                .filter(parameter -> parameter.getExamples() != null)
                .flatMap(parameter -> parameter.getExamples().values().stream())
                .collect(Collectors.toList());
        List<Example> e2 = ops.stream()
                .filter(operation -> operation.getRequestBody() != null)
                .filter(operation -> operation.getRequestBody().getContent() != null)
                .flatMap(operation -> operation.getRequestBody().getContent().values().stream())
                .filter(mediaType -> mediaType.getExamples() != null)
                .flatMap(mediaType -> mediaType.getExamples().values().stream())
                .collect(Collectors.toList());
        List<Example> e3 = ops.stream()
                .filter(operation -> operation.getResponses() != null)
                .flatMap(operation -> operation.getResponses().values().stream())
                .filter(apiResponse -> apiResponse.getContent() != null)
                .flatMap(apiResponse -> apiResponse.getContent().values().stream())
                .filter(mediaType -> mediaType.getExamples() != null)
                .flatMap(mediaType -> mediaType.getExamples().values().stream())
                .collect(Collectors.toList());
        List<Example> allExamples = new LinkedList<>();
        allExamples.addAll(e1);
        allExamples.addAll(e2);
        allExamples.addAll(e3);
        allExamples = allExamples.stream()
                .filter(example -> example.getValue() != null)
                .collect(Collectors.toList());

        ObjectMapper mapper = new ObjectMapper();
        ObjectWriter writer = mapper.writer(new DefaultPrettyPrinter());
        System.out.println(
                allExamples.stream()
                        .map(Example::getValue)
                        .map(valueObject -> {
                            try {
                                return writer.writeValueAsString(valueObject);
                            } catch (JsonProcessingException e) {
                                return "Error while processing json";
                            }
                        })
                        .collect(Collectors.joining())
        );
    }
}