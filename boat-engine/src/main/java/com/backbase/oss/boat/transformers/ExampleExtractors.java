package com.backbase.oss.boat.transformers;

import com.backbase.oss.boat.example.NamedExample;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.headers.Header;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.parameters.Parameter;

import javax.annotation.Nullable;
import javax.validation.constraints.NotNull;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Convenience example extractors from common OpenAPI spec elements.
 * Names of examples in returned lists are extracted from keys in hash maps or
 * other names collected during the traversal of the {@link OpenAPI} spec instance.
 */
public class ExampleExtractors {

    private ExampleExtractors() {
        throw new AssertionError("Private constructor");

    }

    /**
     * Extracts examples from the header.
     * The header has pairs of named examples of its own, and may also have a content field, which may have its own.
     * The content has no direct examples, but it has media types, which do.
     * Also, media types have encodings, which in turns, may have their own headers, with their respective examples.
     * All are considered and extracted recursively, until the hierarchy is exhaustively traversed.
     *
     * @param header the header to get the examples from.
     * @return the list of named examples extracted from the given header.
     */
    @NotNull
    static List<NamedExample> headerExamples(@NotNull Header header) {
        String prefix = prefixFromSchema(header.getSchema());
        List<NamedExample> allExamples = Optional.ofNullable(header.getExamples())
                .orElse(Collections.emptyMap())
                .entrySet()
                .stream()
                .map(entry -> new NamedExample(exampleName(prefix, entry.getKey()), entry.getValue()))
                .collect(Collectors.toList());
        // the following if statement can be refactored to use FP, but it'd become less readable.
        if (header.getContent() != null) {
            allExamples.addAll(contentExamples(header.getContent()));
        }
        return allExamples;
    }

    /**
     * Extracts examples from the parameter and its content.
     *
     * @param parameter the parameter to extract examples from.
     * @return the list of named examples extracted from the given parameter.
     */
    @NotNull
    static List<NamedExample> parameterExamples(@NotNull Parameter parameter) {
        List<NamedExample> allExamples = Optional.ofNullable(parameter.getExamples())
                .orElse(Collections.emptyMap())
                .entrySet()
                .stream()
                .map(entry -> new NamedExample(exampleName(parameter.getName(), entry.getKey()), entry.getValue()))
                .collect(Collectors.toList());
        if (parameter.getContent() != null) {
            allExamples.addAll(contentExamples(parameter.getContent()));
        }
        return allExamples;
    }

    /**
     * Extracts examples from the inset media types of the given content.
     *
     * @param content the content to extract media types and their respective examples from.
     * @return the list of named examples extracted from the media types of the given content.
     */
    @NotNull
    static List<NamedExample> contentExamples(@NotNull Content content) {
        return content.values().stream()
                .flatMap(mediaType -> mediaTypeExamples(mediaType).stream())
                .collect(Collectors.toList());
    }

    /**
     * Extracts examples from the given media type.
     * Will call {@link #headerExamples(Header)} and be called by it recursively
     * in case of deeply nested encoding headers examples.
     *
     * @param mediaType the media type to extract examples from.
     * @return the list of named examples extracted from the given media type.
     */
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
                .flatMap(headerEntry -> headerExamples(headerEntry.getValue()).stream())
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
                                .map(ref -> ref.replaceAll("([a-zA-Z0-9._#]+/)*", ""))
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
