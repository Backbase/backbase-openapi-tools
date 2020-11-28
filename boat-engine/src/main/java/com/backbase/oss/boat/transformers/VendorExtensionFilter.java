package com.backbase.oss.boat.transformers;

import static java.util.Optional.ofNullable;
import static java.util.Spliterators.spliteratorUnknownSize;
import static java.util.stream.StreamSupport.stream;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ContainerNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.Collection;
import java.util.Map;
import java.util.Spliterator;

import io.swagger.v3.core.util.Yaml;
import io.swagger.v3.oas.models.OpenAPI;
import lombok.NonNull;
import lombok.SneakyThrows;

public class VendorExtensionFilter implements Transformer {

    @Override
    public @NonNull OpenAPI transform(@NonNull OpenAPI openAPI, @NonNull Map<String, Object> options) {
        return ofNullable(options.get("remove"))
            .map(remove -> transform(openAPI, (Collection<String>) remove))
            .orElse(openAPI);
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


