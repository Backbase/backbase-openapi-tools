package com.backbase.oss.codegen.doc;

import com.backbase.oss.boat.transformers.OpenApiStreamUtil;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.examples.Example;
import io.swagger.v3.oas.models.media.ArraySchema;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import java.util.Arrays;
import java.util.List;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

@Slf4j
@UtilityClass
@SuppressWarnings("java:S3740")
public class BoatExampleUtils {

    private static final String PATHS_REF_PREFIX = "#/paths";
    private static final String COMPONENTS_EXAMPLES_REF_PREFIX = "#/components/examples/";


    public static void convertExamples(OpenAPI openAPI, MediaType mediaType, String responseCode, String contentType, List<BoatExample> examples) {
        if (mediaType.getExample() != null) {
            Object example = mediaType.getExample();
            BoatExample boatExample = new BoatExample("example", contentType, new Example().value(example), isJson(contentType));

            if (example instanceof ObjectNode && ((ObjectNode) example).has("$ref")) {
                boatExample.getExample().set$ref(((ObjectNode) example).get("$ref").asText());
            }
            examples.add(boatExample);
        }

        if (mediaType.getExamples() != null) {
            mediaType.getExamples().forEach((key, example) -> {
                log.debug("Adding example: {} to examples with content type: {} and responseCode: {} ", key, contentType, responseCode);
                BoatExample boatExample = new BoatExample(key, contentType, example, isJson(contentType));
                examples.add(boatExample);
            });
        }

        Schema<?> schema = mediaType.getSchema();
        if (schema != null && schema.get$ref() != null) {
            String ref = schema.get$ref();
            processRef(openAPI, contentType, examples, ref);
        } else if (schema instanceof ArraySchema && ((ArraySchema) schema).getItems().get$ref() != null) {
            String ref = ((ArraySchema) schema).getItems().get$ref();
            processRef(openAPI, contentType, examples, ref);
        }
    }

    private static void processRef(OpenAPI openAPI, String contentType, List<BoatExample> examples, String ref) {
        if (ref.startsWith("#/components/schemas")) {
            ref = StringUtils.substringAfterLast(ref, "/");

            if (openAPI.getComponents().getSchemas() != null && openAPI.getComponents().getSchemas().get(ref) != null && openAPI.getComponents().getSchemas().get(ref).getExample() != null) {
                Object example = openAPI.getComponents().getSchemas().get(ref).getExample();
                BoatExample boatExample = new BoatExample("example", contentType, new Example().value(example), isJson(contentType));
                examples.add(boatExample);
            }
        }
    }

    private static boolean isJson(String contentType) {
        return contentType.toLowerCase().contains("json");
    }

    public static void inlineExamples(String name, List<BoatExample> examples, OpenAPI openAPI) {
        examples.stream()
            .filter(boatExample -> boatExample.getExample().get$ref() != null)
            .forEach(boatExample -> {
                String ref = boatExample.getExample().get$ref();
                if (ref.startsWith(COMPONENTS_EXAMPLES_REF_PREFIX)) {
                    resolveComponentsExamples(name, openAPI, boatExample, ref);
                } else if (ref.startsWith(PATHS_REF_PREFIX)) {
                    resolvePathsExamples(name, openAPI, boatExample, ref);
                } else {
                    log.warn("Example ref: {} used in: {} refers to an example that does not exist", ref, name);
                }
            });
    }

    private static void resolveComponentsExamples(
        String name, OpenAPI openAPI, BoatExample boatExample, String ref) {
        String exampleName = ref.replace(COMPONENTS_EXAMPLES_REF_PREFIX, "");
        if (openAPI.getComponents() == null || openAPI.getComponents().getExamples() == null) {
            log.warn("Example ref: {}  used in: {}  refers to an example that does not exist", ref, name);
            return;
        }
        Example example = openAPI.getComponents().getExamples().get(exampleName);
        if (example == null) {
            log.warn("Example ref: {}  used in: {}  refers to an example that does not exist", ref, name);
            return;
        }
        log.debug("Replacing Example ref: {}  used in: {}  with example from components: {}", ref, name, example);
        boatExample.setExample(example);
    }

    private static void resolvePathsExamples(
        String name, OpenAPI openAPI, BoatExample boatExample, String ref) {

        // #/paths/
        // ~1client-api~1v2~1accounts~1balance-history~1%7BarrangementIds%7D/
        // get/
        // responses/
        // 200/
        // content/
        // text~1csv/
        // example
        if (openAPI.getPaths() == null) {
            log.warn("Example ref: {}  refers to '/paths' but it is not there.", ref);
            return;
        }
        String[] refParts = Arrays.stream(ref.replace(PATHS_REF_PREFIX, "").split("/"))
            .map(s -> s.replace("~1", "/"))
            .toArray(String[]::new);

        String pathName = refParts[1];
        PathItem pathItem = openAPI.getPaths().get(pathName);
        if (pathItem == null) {
            log.warn("Example ref: {} refers to path {} but it is not defined.", ref, pathName);
            return;
        }

        String operationName = refParts[2];
        Operation operation = findOperation(pathItem, operationName);
        if (operation == null) {
            log.warn("Example ref: {} refers to operation {} but it is not defined.", ref, operationName);
            return;
        }

        Content content = null;
        String mediaTypeName = null;
        if ("requestBody".equals(refParts[3])) {
            content = operation.getRequestBody().getContent();
            mediaTypeName = refParts[5];
        } else {
            content = operation.getResponses().get(refParts[4]).getContent();
            mediaTypeName = refParts[6];
        }
        if (content == null) {
            log.warn("Example ref: {} refers to content that is not defined.", ref);
            return;
        }

        MediaType mediaType = content.get(mediaTypeName);
        if (mediaType == null) {
            log.warn("Example ref: {} refers to mediaType {} that is not defined.", ref, mediaTypeName);
            return;
        }

        Example example = new Example().value(mediaType.getExample());
        log.debug("Replacing Example ref: {}  used in: {}  with example from components: {}", ref, name, example);
        boatExample.setExample(example);
    }

    private static Operation findOperation(PathItem pathItem, String operationName) {
        String o = operationName.toLowerCase();
        switch (o) {
            case "get":
                return pathItem.getGet();
            case "post":
                return pathItem.getPost();
            case "put":
                return pathItem.getPut();
            case "patch":
                return pathItem.getPatch();
            case "delete":
                return pathItem.getDelete();
            default:
                throw new IllegalArgumentException("Unsupported operationName " + o);
        }
    }

}
