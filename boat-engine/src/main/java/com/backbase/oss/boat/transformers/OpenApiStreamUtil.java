package com.backbase.oss.boat.transformers;

import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Stream.of;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.media.ArraySchema;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;
import lombok.experimental.UtilityClass;

@UtilityClass
public class OpenApiStreamUtil {

    /**
     * Returns a stream of all(*) the Schema in the openAPI.
     *
     * (*) Currently includes:
     * - operation (get, post, put, delete, patch) requests & response bodies.
     * - components/schemas
     *
     * @param openAPI to get the schema of
     * @return a stream of the schemas in the open api.
     */
    static Stream<Schema> streamSchemas(OpenAPI openAPI) {

        // Schema's used in operations request & response bodies as well as proper components.
        List<Schema> firstLevelSchemas = Stream.of(
            openAPI.getPaths().values().stream()
                .flatMap(OpenApiStreamUtil::streamOperations)
                .flatMap(OpenApiStreamUtil::streamSchemas),
            openAPI.getComponents().getSchemas().values().stream())
            .reduce(Stream::concat)
            .orElseGet(Stream::empty)
            .collect(toList());

        // traverse down the tree of the list of first level schemas, also keep the first level schemas
        return (Stream<Schema>) Stream.of(
            firstLevelSchemas.stream(),
            firstLevelSchemas.stream()
                .map(OpenApiStreamUtil::stream)
                .reduce(Stream::concat)
                .orElse(Stream.empty()))
            .reduce(Stream::concat)
            .orElse(Stream.empty());
    }

    private static Stream<Schema> streamSchemas(Operation operation) {
        // Schema's used in operations request & response bodies as well as proper components.
        return Stream.of(
            nullSafeContent(operation).values().stream()
                .map(MediaType::getSchema)
                .filter(Objects::nonNull),
            nullSafeApiResponses(operation).stream()
                .map(ApiResponse::getContent)
                .filter(Objects::nonNull)
                .flatMap(OpenApiStreamUtil::streamContentSchemas))
            .reduce(Stream::concat)
            .orElseGet(Stream::empty);
    }

    private static Stream<Schema> stream(Schema schema) {
        if (schema instanceof ArraySchema) {
            ArraySchema arraySchema = (ArraySchema) schema;
            return stream(arraySchema.getItems());
        } else if (schema.getProperties() != null) {
            return ((Collection<Schema>)schema.getProperties().values()).stream()
                .map(OpenApiStreamUtil::stream)
                .reduce(Stream::concat)
                .orElse(Stream.empty());
        } else {
            return Stream.of(schema);
        }
    }

    private static Stream<Schema> streamContentSchemas(Content content) {
        return content.values().stream().map(MediaType::getSchema).filter(Objects::nonNull);
    }

    static Stream<Operation> streamOperations(PathItem pathItem) {
        return of(pathItem.getGet(), pathItem.getPut(), pathItem.getPost(), pathItem.getDelete(), pathItem.getPatch());
    }

    static Content nullSafeContent(Operation operation) {
        return operation != null
            && operation.getRequestBody() != null
            && operation.getRequestBody().getContent() != null
            ? operation.getRequestBody().getContent() : new Content();
    }

    static Collection<ApiResponse> nullSafeApiResponses(Operation operation) {
        return operation != null
            && operation.getResponses() != null
            ? operation.getResponses().values() : emptyList();
    }

}
