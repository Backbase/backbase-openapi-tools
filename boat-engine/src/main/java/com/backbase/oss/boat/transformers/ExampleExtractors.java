package com.backbase.oss.boat.transformers;

import com.backbase.oss.boat.example.NamedExample;
import io.swagger.v3.oas.models.examples.Example;
import io.swagger.v3.oas.models.headers.Header;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.parameters.Parameter;

import javax.annotation.Nullable;
import javax.validation.constraints.NotNull;
import java.util.*;
import java.util.stream.Collectors;

public class ExampleExtractors {

    @NotNull
    static List<NamedExample> headerExamples(@NotNull String name, @NotNull Header header) {
        String prefix = prefixFromSchema(header.getSchema());
        List<NamedExample> allExamples = Optional.ofNullable(header.getExamples())
                .orElse(Collections.emptyMap())
                .entrySet()
                .stream()
                .map(entry -> new NamedExample(exampleName(prefix, entry.getKey()), entry.getValue()))
                .collect(Collectors.toList());
        // the following if statement can be refactored to use FP, but it'd become less readable.
        if (header.getExample() != null) {
            allExamples.add(new NamedExample(name, new Example().value(header.getExample())));
        }
        if (header.getContent() != null) {
            allExamples.addAll(contentExamples(header.getContent()));
        }
        return allExamples;
    }

    @NotNull
    static List<NamedExample> parameterExamples(@NotNull Parameter parameter) {
        List<NamedExample> allExamples = Optional.ofNullable(parameter.getExamples())
                .orElse(Collections.emptyMap())
                .entrySet()
                .stream()
                .map(entry -> new NamedExample(exampleName(parameter.getName(), entry.getKey()), entry.getValue()))
                .collect(Collectors.toList());
        if (parameter.getName() != null && parameter.getExample() != null) {
            String name = exampleName("param", parameter.getName());
            allExamples.add(new NamedExample(name, new Example().value(parameter.getExample())));
        }
        if (parameter.getContent() != null) {
            allExamples.addAll(contentExamples(parameter.getContent()));
        }
        return allExamples;
    }

    @NotNull
    static List<NamedExample> contentExamples(@NotNull Content content) {
        return content.values().stream()
                .flatMap(mediaType -> mediaTypeExamples(mediaType).stream())
                .collect(Collectors.toList());
    }

    @NotNull
    static List<NamedExample> mediaTypeExamples(@NotNull MediaType mediaType) {
        String prefix = prefixFromSchema(mediaType.getSchema());
        Collection<NamedExample> examples = Optional.ofNullable(mediaType.getExamples())
                .orElse(Collections.emptyMap())
                .entrySet()
                .stream()
                .map(entry -> new NamedExample(exampleName(prefix, entry.getKey()), entry.getValue()))
                .collect(Collectors.toList());
        Collection<NamedExample> encodingHeadersExamples = Optional.ofNullable(mediaType.getEncoding())
                .orElse(Collections.emptyMap())
                .values()
                .stream()
                .filter(encoding -> encoding.getHeaders() != null)
                .flatMap(encoding -> encoding.getHeaders().entrySet().stream())
                .flatMap(headerEntry -> headerExamples(headerEntry.getKey(), headerEntry.getValue()).stream())
                .collect(Collectors.toList());
        List<NamedExample> allExamples = new ArrayList<>();
        allExamples.addAll(examples);
        allExamples.addAll(encodingHeadersExamples);
        return allExamples;
    }

    @NotNull
    private static String prefixFromSchema(@Nullable Schema<?> optSchema) {
        return Optional.ofNullable(optSchema).map(schema ->
                Optional.ofNullable(schema.getName()).orElse(
                        Optional.ofNullable(schema.get$ref())
                                .map($ref -> $ref.replaceAll("([a-zA-Z0-9._#]+/)*", ""))
                                .orElse("")
                )).orElse("");
    }

    @NotNull
    private static String exampleName(@NotNull String prefix, @NotNull String key) {
        if ("example".equalsIgnoreCase(key)) {
            return prefix;
        } else {
            return prefix + "-" + key;
        }
    }
}
