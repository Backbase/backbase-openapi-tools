package com.backbase.oss.boat.transformers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ContainerNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Spliterator;

import io.swagger.v3.core.util.Yaml;
import io.swagger.v3.oas.models.OpenAPI;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.SneakyThrows;

import static java.util.Collections.emptyList;
import static java.util.Optional.ofNullable;
import static java.util.Spliterators.spliteratorUnknownSize;
import static java.util.stream.Collectors.toSet;
import static java.util.stream.StreamSupport.stream;

@SuppressWarnings("java:S3740")
@Getter
@Setter
public class ExtensionFilter implements Transformer {

    private List<String> remove = emptyList();

    @Override
    public @NonNull OpenAPI transform(@NonNull OpenAPI openAPI, @NonNull Map<String, Object> options) {
        List<String> extensions = new ArrayList<>(remove);

        ofNullable(options.get("remove"))
            .map(Collection.class::cast)
            .ifPresent(extensions::addAll);

        extensions.addAll(
            extensions.stream()
                .filter(s -> !s.startsWith("x-"))
                .map(s -> "x-" + s)
                .collect(toSet()));

        return extensions.isEmpty() ? openAPI : transform(openAPI, extensions);
    }

    @SneakyThrows
    private OpenAPI transform(OpenAPI source, Collection<String> remove) {
        final ObjectMapper mapper = Yaml.mapper();
        final JsonNode tree = mapper.valueToTree(source);

        if (tree instanceof ContainerNode) {
            removeExtensions((ContainerNode) tree, remove);
        }

        return mapper.treeToValue(tree, OpenAPI.class);
    }

    private void removeExtensions(ContainerNode node, Collection<String> remove) {
        if (node.isObject()) {
            ((ObjectNode) node).remove(remove);
        }

        stream(spliteratorUnknownSize(node.elements(), Spliterator.ORDERED), false)
            .filter(ContainerNode.class::isInstance)
            .map(ContainerNode.class::cast)
            .forEach(child -> removeExtensions(child, remove));
    }
}


