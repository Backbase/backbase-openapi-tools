package com.backbase.oss.boat.transformers.bundler;


import com.backbase.oss.boat.transformers.TransformerException;
import com.fasterxml.jackson.core.JsonPointer;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import io.swagger.v3.core.util.Json;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.examples.Example;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.parser.models.RefFormat;
import io.swagger.v3.parser.util.PathUtils;
import io.swagger.v3.parser.util.RefUtils;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;


@Slf4j
@SuppressWarnings({"rawtypes","java:S3740"})
public class ExamplesProcessor {

    public static final String COMPONENTS_EXAMPLES = "#/components/examples/";
    ObjectMapper yamlObjectMapper = new ObjectMapper(YAMLFactory.builder().build());
    ObjectMapper jsonObjectMapper = new ObjectMapper();

    private final OpenAPI openAPI;
    private final Path rootDir;
    private final Map<String, ExampleHolder> cache = new LinkedHashMap<>();

    public ExamplesProcessor(OpenAPI openAPI, String inputFile) {
        super();
        this.openAPI = openAPI;
        this.rootDir = PathUtils.getParentDirectoryOfFile(inputFile);
    }

    public void processExamples(OpenAPI openAPI) {

        log.debug("Processing examples in Components");
        // dereference the /component/examples first...
        getComponentExamplesFromOpenAPI().entrySet().stream()
            .map(e -> ExampleHolder.of(e.getKey(), e.getValue(), true))
            .forEach(this::fixInlineExamples);


        openAPI.getPaths()
            .forEach((path, pathItem) -> {
                log.debug("Processing examples in Path: {}", path);
                pathItem.readOperationsMap().forEach((httpMethod, operation) -> {
                    log.debug("Processing examples in Operation: {}", httpMethod);
                    if (operation.getRequestBody() != null) {
                        operation.getRequestBody().getContent().forEach((contentType, mediaType) -> {
                            log.debug("Processing Request Body examples for Content Type: {}", contentType);
                            processMediaType(mediaType, null, false);
                        });
                    }
                    operation.getResponses().forEach((responseCode, apiResponse) -> {
                        log.debug("Processing Response Body Examples for Response Code: {}", responseCode);
                        if (apiResponse.getContent() != null) {
                            apiResponse.getContent().forEach((contentType, mediaType) -> {
                                log.debug("Processing Response Body Examples for Content Type: {}", contentType);
                                processMediaType(mediaType, null, false);
                            });
                        }
                    });
                });
            });
    }

    public void processContent(Content content, String relativePath) {

        content.forEach((s, mediaType) -> {
            log.debug("Processing Consent for: {} with relative path: {}", s, relativePath);
            processMediaType(mediaType, relativePath, true);
        });


    }

    public void processMediaType(MediaType mediaType, String relativePath, boolean derefenceExamples) {
        if (mediaType.getExamples() != null) {
            mediaType.getExamples().forEach(((key, example) -> {
                log.debug("Processing Example: {} with value: {} and ref: {} ", key, example.getValue(), example.get$ref());
                ExampleHolder<?> exampleHolder = ExampleHolder.of(key, example);
                fixInlineExamples(exampleHolder, relativePath, derefenceExamples);
                if (exampleHolder.getRef() != null) {
                    example.set$ref(exampleHolder.getRef());
                    example.setValue(null);
                } else {
                    example.setValue(exampleHolder.example());
                }
                log.debug("Finished Processing Example: {} with value: {}", key, exampleHolder);
            }));
        }
        if (mediaType.getExample() != null) {
            log.debug("Processing Example: {} ", mediaType.getExample());
            ExampleHolder<?> exampleHolder = ExampleHolder.of(null, mediaType.getExample());
            fixInlineExamples(exampleHolder, relativePath, false);
            log.debug("Finished Processing Example: {}", exampleHolder);
        }
    }

    private void putComponentExample(String key, Example example) {
        getComponentExamplesFromOpenAPI().put(key, example);
    }

    private Map<String, Example> getComponentExamplesFromOpenAPI() {
        // Apparently this is needed.
        if (openAPI.getComponents() == null) {
            openAPI.setComponents(new Components());
        }
        if (openAPI.getComponents().getExamples() == null) {
            openAPI.getComponents().setExamples(new LinkedHashMap<>());
        }
        return openAPI.getComponents().getExamples();
    }

    private void fixInlineExamples(ExampleHolder exampleHolder) {
        fixInlineExamples(exampleHolder, null, false);
    }

    private void fixInlineExamples(ExampleHolder exampleHolder, String relativePath, boolean derefenceExamples) {
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

        if (refPath.startsWith(COMPONENTS_EXAMPLES)) {
            log.debug("Ref path already points to examples. Leave it as it is");
            return;
        }

        Path path;
        Optional<String> fragment;
        if (refPath.contains("#") && !refPath.startsWith("#/components/examples")) {
            fragment = Optional.of(StringUtils.substringAfter(refPath, "#"));
            refPath = StringUtils.strip(StringUtils.substringBefore(refPath, "#"), "./");
        } else {
            fragment = Optional.empty();
        }

        path = resolvePath(relativePath, refPath);
        try {
            String content = readContent(path);

            if (fragment.isPresent()) {
                String exampleName = StringUtils.substringAfterLast(fragment.get(), "/");
                // resolve fragment from json node
                JsonNode jsonNode = yamlObjectMapper.readTree(content);
                JsonPointer jsonPointer = JsonPointer.compile(fragment.get());
                JsonNode exampleNode = jsonNode.at(jsonPointer);

                if (exampleNode.has("$ref")) {
                    processExample(exampleHolder, relativePath, exampleName, exampleNode);
                } else {
                    exampleHolder.setContent(jsonObjectMapper.writeValueAsString(exampleNode));
                }
            } else {
                exampleHolder.setContent(content);
                dereferenceExample(exampleHolder);
            }
            if (derefenceExamples) {
                dereferenceExample(exampleHolder);
            }

        } catch (IOException e) {
            throw new TransformerException("Unable to fix inline examples", e);
        }
    }

    private void processExample(ExampleHolder exampleHolder, String relativePath, String exampleName, JsonNode exampleNode) throws IOException {
        String refPath;
        Path path;
        String content;
        refPath = exampleNode.get("$ref").asText();
        exampleHolder.replaceRef(refPath);
        path = resolvePath(relativePath, refPath);
        resolvePath(relativePath, refPath);
        content = readContent(path);

        exampleHolder.setContent(content);
        if (exampleName != null) {
            exampleHolder.setExampleName(exampleName);
            exampleHolder.replaceRef(COMPONENTS_EXAMPLES + exampleName);
        }

        if (getComponentExamplesFromOpenAPI().containsKey(exampleName)) {
            log.debug("Updating example: {} in components/examples", exampleName);
            // Check whether example is already dereferenced
        } else {
            log.debug("Adding Example: {} to components/examples", exampleName);
            putComponentExample(exampleName,
                new Example().value(convertExampleContent(exampleHolder, refPath)));
        }
    }

    private String readContent(Path path) throws IOException {
        String content;
        content = StringUtils.strip(StringUtils.replaceEach(
            new String(Files.readAllBytes(path)),
            new String[]{"\t"},
            new String[]{"  "}));
        return content;
    }

    private Path resolvePath(String relativePath, String refPath) {
        Path path;
        if (relativePath == null) {
            path = rootDir.resolve(StringUtils.strip(refPath, "./"));
        } else {
            path = Paths.get(rootDir.toString(), relativePath, refPath);
        }
        return path;
    }

    private void dereferenceExample(ExampleHolder exampleHolder) {
        String rootName = exampleHolder.getExampleName();
        int count = 0;
        while (existsButNotMatching(cache.get(makeCountedName(rootName, count)), exampleHolder)) {
            count++;
        }
        String exampleName = makeCountedName(rootName, count);
        Object content = convertExampleContent(exampleHolder, exampleHolder.getRef());
        cache.put(exampleName, exampleHolder);
        exampleHolder.replaceRef(COMPONENTS_EXAMPLES + exampleName);
        putComponentExample(exampleName, new Example().value(content).summary(exampleName));
    }

    private Object convertExampleContent(ExampleHolder exampleHolder, String refPath) {
        try {
            if (exampleHolder.getRef().endsWith("json") || refPath.endsWith("json")) {
                return Json.mapper().readValue(exampleHolder.getContent(), Object.class);
            }
            return exampleHolder.getContent();
        } catch (JsonProcessingException | RuntimeException e) {
            throw new TransformerException("Failed to process example content for " + exampleHolder, e);
        }
    }

    private boolean existsButNotMatching(ExampleHolder cached, ExampleHolder exampleHolder) {
        return cached != null && !Objects.equals(cached.getContent(), exampleHolder.getContent());
    }

    private String makeCountedName(String s, int count) {
        return count == 0 ? s : s + "-" + count;
    }


}
