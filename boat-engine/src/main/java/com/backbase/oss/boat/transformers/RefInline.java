package com.backbase.oss.boat.transformers;

import static io.swagger.v3.oas.models.PathItem.HttpMethod.GET;
import static io.swagger.v3.oas.models.PathItem.HttpMethod.PATCH;
import static io.swagger.v3.oas.models.PathItem.HttpMethod.POST;
import static io.swagger.v3.oas.models.PathItem.HttpMethod.PUT;
import static java.beans.Introspector.decapitalize;
import static java.lang.String.format;
import static java.util.Arrays.stream;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.joining;
import static org.apache.commons.collections4.MapUtils.isNotEmpty;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

import com.backbase.oss.boat.serializer.SerializerUtils;
import com.google.common.base.CaseFormat;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;

import org.springframework.http.HttpStatus;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem.HttpMethod;
import io.swagger.v3.oas.models.media.ArraySchema;
import io.swagger.v3.oas.models.media.ComposedSchema;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.parameters.RequestBody;
import lombok.Getter;
import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;

@SuppressWarnings("rawtypes")
@Slf4j
public class RefInline implements Transformer {

    private static final Path EMPTY_PATH = Paths.get("");

    private static boolean isLocalRef(String ref) {
        return isNotBlank(ref) && !ref.startsWith("http");
    }

    private static Path safeParent(Path path) {
        return path.getParent() != null ? path.getParent() : EMPTY_PATH;
    }

    private static boolean isEmptyPath(Path path) {
        return isBlank(path.normalize().toString());
    }

    private static String toType(Path path) {
        return stream(path.normalize().toString().split("/"))
            .map(RefInline::toCamel)
            .collect(joining());
    }

    private static String toHyphen(String name) {
        return CaseFormat.LOWER_CAMEL
            .to(CaseFormat.LOWER_HYPHEN, decapitalize(name));
    }

    private static String toCamel(String name) {
        return CaseFormat.LOWER_HYPHEN
            .to(CaseFormat.UPPER_CAMEL, name);

    }

    private static <T, X> X nonNull(T t, Function<T, X> get, BiConsumer<T, X> set, Supplier<X> bld) {
        final X x = ofNullable(get.apply(t)).orElseGet(bld);

        set.accept(t, x);

        return x;
    }

    private static boolean isInlined(Schema schema) {
        return schema != null && isBlank(schema.get$ref()) && isNotEmpty(schema.getProperties());
    }

    @Getter
    private final Map<Path, Schema> files = new HashMap<>();
    private final Map<String, Schema> extracted = new HashMap<>();
    private final Set<String> opTypes = new HashSet<>();
    private final Path targetFile;
    private final Path targetPath;

    private final Path schemasPath;

    @Getter
    private OpenAPI openAPI;

    public RefInline(@NonNull Path targetFile, @NonNull Path schemasPath) {
        if (schemasPath.isAbsolute()) {
            throw new IllegalArgumentException("Schemas path cannot be absolute");
        }

        this.targetFile = targetFile.normalize();
        this.targetPath = safeParent(this.targetFile);
        this.schemasPath = schemasPath.normalize();
    }

    @SneakyThrows
    public void export(boolean clean) {
        if (clean && !isEmptyPath(this.targetPath) && Files.isDirectory(this.targetPath)) {
            FileUtils.cleanDirectory(this.targetPath.toFile());
        }

        SerializerUtils.write(this.targetFile, this.openAPI);

        this.files.forEach((file, schema) -> {
            SerializerUtils.write(this.targetPath.resolve(file), schema);
        });
    }

    @SuppressWarnings("unused")
    @Override
    public void transform(@NonNull OpenAPI openAPI, Map<String, Object> unused) {
        this.openAPI = openAPI;

        nonNull(openAPI, OpenAPI::getComponents, OpenAPI::setComponents, Components::new);

        final Map<String, Schema> schemas =
            nonNull(openAPI.getComponents(), Components::getSchemas, Components::setSchemas, TreeMap::new);

        transformSchemas(EMPTY_PATH, schemas);

        nonNull(openAPI, OpenAPI::getPaths, OpenAPI::setPaths, io.swagger.v3.oas.models.Paths::new)
            .forEach((path, item) -> {
                ofNullable(item.getGet()).ifPresent(op -> transformOperation(GET, path, op));
                ofNullable(item.getPatch()).ifPresent(op -> transformOperation(PATCH, path, op));
                ofNullable(item.getPost()).ifPresent(op -> transformOperation(POST, path, op));
                ofNullable(item.getPut()).ifPresent(op -> transformOperation(PUT, path, op));
            });

        schemas.putAll(this.extracted);
    }

    private String buildTypeName(HttpMethod method, String uri, Operation op, String... suffixes) {
        if (isBlank(op.getOperationId())) {
            throw new RuntimeException(format("Cannot find operation id for %s %s", method, uri));
        }

        final List<String> elements = new ArrayList<>();

        elements.add(uri.replace('/', '-').replaceAll("^-|-$|\\{|\\}", ""));
        if (method != HttpMethod.GET) {
            elements.add(method.name().toLowerCase());
        }
        elements.add(toHyphen(op.getOperationId()));

        stream(suffixes).filter(StringUtils::isNotBlank).forEach(elements::add);

        final String type = toCamel(elements.stream()
            .filter(StringUtils::isNotBlank)
            .collect(joining("-")));

        if (this.opTypes.add(type)) {
            return type;
        }

        return type;
    }

    private void transformOperation(HttpMethod method, String uri, Operation op) {
        ofNullable(op.getRequestBody())
            .map(RequestBody::getContent)
            .ifPresent(content -> {
                transformContent(method, uri, op, content, "request");
            });

        ofNullable(op.getResponses())
            .ifPresent(responses -> {
                // response.entrySet().stream()
                // .filter(ent -> isInlined(ent.getValue().getContent()));

                responses.forEach((status, resp) -> {
                    final HttpStatus st = toHttpStatus(status);

                    if (st != null) {
                        if (st.is2xxSuccessful()) {
                            transformContent(method, uri, op, resp.getContent(), "response");
                        } else {
                            transformContent(method, uri, op, resp.getContent(), "response",
                                Integer.toString(st.value()));
                        }
                    } else {
                        transformContent(method, uri, op, resp.getContent(), "response", status);
                    }
                });
            });
    }

    private HttpStatus toHttpStatus(String status) {
        try {
            return HttpStatus.valueOf(Integer.parseInt(status));
        } catch (final IllegalArgumentException ex) {
        }
        try {
            return HttpStatus.valueOf(status);
        } catch (final IllegalArgumentException ex) {
        }

        // unparseable
        return null;
    }

    private void transformContent(HttpMethod method, String uri, Operation op, Content content, String... suffixes) {
        content.entrySet().stream()
            .filter(ent -> isInlined(ent.getValue().getSchema()))
            .forEach(ent -> {
                final MediaType media = ent.getValue();

                media.schema(
                    transformSchema(EMPTY_PATH, buildTypeName(method, uri, op, suffixes), media.getSchema()));
            });
    }

    private void transformSchemas(Path parent, Map<String, Schema> schemas) {
        inlinedSchemas(schemas)
            .forEach(e -> {
                schemas.put(e.getKey(), transformSchema(parent, e.getKey(), e.getValue()));
            });
    }

    private Stream<Map.Entry<String, Schema>> inlinedSchemas(Map<String, Schema> schemas) {
        return schemas.entrySet().stream()
            .filter(ent -> isInlined(ent.getValue()));
    }

    private Schema transformSchema(Path base, String name, Schema schema) {
        final String item = toHyphen(name);
        final String type = toType(base.resolve(item));
        final Path file = this.schemasPath.resolve(base.resolve(item + ".yaml"));

        log.debug("base = {}, item = {}, file = {}, type = {}", base, item, file, type);

        transformSchemas(base.resolve(item), schema.getProperties());

        if (isBlank(schema.getTitle())) {
            schema.setTitle(type);
        }

        // schemas.put(name, $ref(this.schemasPath.resolve(base), type));

        // schemas.put(name, new Schema<>().$ref("#/components/schemas/" + type));

        updateRefs(this.schemasPath.resolve(base), file, schema);

        this.extracted.put(type, new Schema<>().$ref(file.toString()));
        this.files.put(file, schema);

        return new Schema<>().$ref("#/components/schemas/" + type);
    }

    private void updateRefs(Path base, Path file, Schema schema) {
        // if (isLocalRef(schema.get$ref())) {
        // updateRef(output, schema);
        // } else {
        updateRefs(base, file, schema.getProperties().values());

        if (schema instanceof ComposedSchema) {
            final ComposedSchema cs = (ComposedSchema) schema;

            ofNullable(cs.getAllOf()).ifPresent(s -> updateRefs(base, file, s));
        }
        if (schema instanceof ArraySchema) {
            final ArraySchema as = (ArraySchema) schema;

            ofNullable(as.getItems()).ifPresent(s -> updateRefs(base, file, s));
        }
        // }
    }

    private void updateRefs(Path base, Path file, Collection<Schema> schemas) {
        schemas.stream()
            .filter(s -> isLocalRef(s.get$ref()))
            .forEach(s -> updateRef(base, file, s));
    }

    private void updateRef(Path base, Path file, Schema schema) {
        final String $ref = schema.get$ref();
        final String link;

        switch ($ref.indexOf("#/")) {
            case 0:
                link = this.targetPath.resolve(base).relativize(this.targetFile) + $ref;
                break;

            default:
                link = $ref;
        }
        // final String link = this.target.resolve(file)
        // .relativize(this.target)
        // .resolve(this.target.getFileName())
        // .normalize().toString();
        //
        // if (isNotBlank($ref) /* && updatedRefs.add(s) */) {
        // switch ($ref.indexOf("#/")) {
        // case 0:
        // schema.set$ref(link + $ref);
        // break;
        //
        // case -1:
        // schema.set$ref(file.relativize(Paths.get("")).resolve($ref).normalize().toString());
        // break;
        //
        // default:
        // final String[] parts = splitByWholeSeparator(schema.get$ref(), "#/");
        //
        // parts[0] = file.relativize(Paths.get("")).resolve(parts[0]).normalize().toString();
        //
        // schema.set$ref(stream(parts).collect(joining("#/")));
        // }
        // }

        schema.set$ref(link);

        log.debug("relocated {} to {}", $ref, link);
    }

    private Schema $ref(Path base, String type) {
        final Path rel = this.targetPath.resolve(safeParent(this.schemasPath.resolve(base)))
            .relativize(this.targetFile);

        final Schema s = new Schema<>()
            .$ref(rel + "#/components/schemas/" + type);

        // updatedRefs.add(scm);

        log.debug("added new type: {} to {}", type, base);

        return s;
    }
}
