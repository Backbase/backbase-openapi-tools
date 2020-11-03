package com.backbase.oss.boat.transformers.bundler;

import static com.google.common.collect.Maps.newHashMap;
import static java.util.Collections.emptyList;
import static java.util.stream.Stream.of;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.swagger.v3.core.util.Json;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.examples.Example;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.parser.models.RefFormat;
import io.swagger.v3.parser.util.PathUtils;
import io.swagger.v3.parser.util.RefUtils;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

@Slf4j
public class ExamplesProcessor {

    private final OpenAPI openAPI;
    private final Path rootDir;
    private final Map<String, ExampleHolder> cache = newHashMap();

    public ExamplesProcessor(OpenAPI openAPI, String inputFile) {
        super();
        this.openAPI = openAPI;
        this.rootDir = PathUtils.getParentDirectoryOfFile(inputFile);
    }

    public void processExamples(OpenAPI openAPI) {

        // dereference the /component/examples first...
        getComponentExamplesFromOpenAPI().entrySet().stream()
            .map(e -> ExampleHolder.of(e.getKey(), e.getValue(), true))
            .forEach(this::fixInlineExamples);

        // dereference the inline examples creating dereferenced /component/examples
        openAPI.getPaths().entrySet().stream()
            .flatMap(this::streamOperations)
            .flatMap(this::streamOperationExamples)
            .forEach(this::fixInlineExamples);

    }

    public void processContent(Content content, String relativePath) {
        streamContentExamples(content).forEach(example ->  fixInlineExamples(example, relativePath));
    }

    private Map<String, Example> getComponentExamplesFromOpenAPI() {
        // Apparently this is needed.
        if (openAPI.getComponents() == null) {
            openAPI.setComponents(new Components());
        }
        if (openAPI.getComponents().getExamples() == null) {
            openAPI.getComponents().setExamples(newHashMap());
        }
        return openAPI.getComponents().getExamples();
    }

    private Stream<ExampleHolder> streamOperationExamples(Operation operation) {
        Content requestContent = nullSafeContent(operation);
        Collection<ApiResponse> responseResponses = nullSafeApiResponses(operation);
        return Stream.concat(
            streamContentExamples(requestContent),
            responseResponses.stream()
                .map(ApiResponse::getContent)
                .filter(Objects::nonNull)
                .flatMap(this::streamContentExamples)
        );
    }

    private Content nullSafeContent(Operation operation) {
        return operation != null
            && operation.getRequestBody() != null
            && operation.getRequestBody().getContent() != null
            ? operation.getRequestBody().getContent() : new Content();
    }

    private Collection<ApiResponse> nullSafeApiResponses(Operation operation) {
        return operation != null
            && operation.getResponses() != null
            ? operation.getResponses().values() : emptyList();
    }

    private Stream<ExampleHolder> streamContentExamples(Content content) {
        return Stream.of(
            content.values().stream().map(MediaType::getExample).filter(Objects::nonNull)
                .map(ExampleHolder::of),
            content.values().stream().map(MediaType::getExamples).filter(Objects::nonNull)
                .flatMap(map -> map.entrySet().stream())
                .map(e -> ExampleHolder.of(e.getKey(), e.getValue(), false)))
            .flatMap(s -> s);
    }

    private Stream<Operation> streamOperations(Entry<String, PathItem> item) {
        log.info("Processing path item {}", item.getKey());
        PathItem pathItem = item.getValue();
        return of(pathItem.getGet(), pathItem.getPut(), pathItem.getPost(), pathItem.getDelete());
    }

    private void fixInlineExamples(ExampleHolder exampleHolder) {
        fixInlineExamples(exampleHolder, null);
    }

    private void fixInlineExamples(ExampleHolder exampleHolder, String relativePath) {
        log.debug("fixInlineExamples: '{}', relative path '{}'", exampleHolder, relativePath);

        if (exampleHolder.getRef() == null) {
            log.debug("not fixing (ref not found): {}", exampleHolder);
            return;
        }
        String refPath = exampleHolder.getRef();
        if (RefUtils.computeRefFormat(refPath) != RefFormat.RELATIVE) {
            log.debug("not fixing (not relative ref): '{}'", exampleHolder);
            return;
        }
        Path path;
        if (relativePath == null) {
            path = rootDir.resolve(StringUtils.strip(refPath, "./"));
        } else {
            path = Paths.get(rootDir.toString(), relativePath, refPath);
        }
        try {
            String content = StringUtils.strip(StringUtils.replaceEach(
                new String(Files.readAllBytes(path)),
                new String[] {"\t"},
                new String[] {"  "}));
            exampleHolder.setContent(content);
            dereferenceExample(exampleHolder);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void dereferenceExample(ExampleHolder exampleHolder) {
        String rootName = exampleHolder.getExampleName();
        int count = 0;
        while (existsButNotMatching(cache.get(makeCountedName(rootName, count)), exampleHolder)) {
            count++;
        }
        String exampleName = makeCountedName(rootName, count);
        Object content = convertExampleContent(exampleHolder);
        cache.put(exampleName, exampleHolder);
        exampleHolder.replaceRef("#/components/examples/" + exampleName);
        getComponentExamplesFromOpenAPI().put(exampleName, new Example()
            .value(content)
            .summary(exampleName));
    }

    private Object convertExampleContent(ExampleHolder exampleHolder) {
        try {
            if (exampleHolder.getRef().endsWith("json")) {
                return Json.mapper().readValue(exampleHolder.getContent(), Object.class);
            }
            return exampleHolder.getContent();
        } catch (JsonProcessingException | RuntimeException e) {
            throw new RuntimeException("Failed to process example content for " + exampleHolder, e);
        }
    }

    private boolean existsButNotMatching(ExampleHolder cached, ExampleHolder exampleHolder) {
        return cached != null && !Objects.equals(cached.getContent(), exampleHolder.getContent());
    }

    private String makeCountedName(String s, int count) {
        return count == 0 ? s : s + "-" + count;
    }

}
