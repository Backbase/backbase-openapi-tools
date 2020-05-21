package com.backbase.oss.boat.transformers;

import io.swagger.v3.oas.models.examples.Example;
import io.swagger.v3.oas.models.headers.Header;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.parameters.Parameter;

import javax.validation.constraints.NotNull;
import java.util.*;
import java.util.stream.Collectors;

public class CommonExtractors {

    @NotNull
    static List<Example> headerExamples(@NotNull Header header) {
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
    static List<Example> parameterExamples(@NotNull Parameter parameter) {
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
    static List<Example> contentExamples(@NotNull Content content) {
        return content.values().stream()
                .flatMap(mediaType -> mediaTypeExamples(mediaType).stream())
                .collect(Collectors.toList());
    }

    @NotNull
    static List<Example> mediaTypeExamples(@NotNull MediaType mediaType) {
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
