package com.backbase.oss.boat.transformers;

import static java.beans.Introspector.decapitalize;
import static java.lang.String.format;
import static java.util.Arrays.stream;
import static java.util.Collections.emptyMap;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.joining;
import static org.apache.commons.collections4.MapUtils.isNotEmpty;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;

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
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.http.HttpStatus;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem.HttpMethod;
import io.swagger.v3.oas.models.media.ArraySchema;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.responses.ApiResponses;
import lombok.Getter;
import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;

@SuppressWarnings("rawtypes")
@Slf4j
public class ExplodeTransformer implements Transformer {

    private static final Path EMPTY_PATH = Paths.get("");

    private static boolean isLocalRef(String ref) {
        return isNotBlank(ref) && !ref.startsWith("http");
    }

    private static Path safeParent(Path path) {
        return ofNullable(path.getParent()).orElse(EMPTY_PATH);
    }

    private static boolean isEmptyPath(Path path) {
        return isBlank(path.normalize().toString());
    }

    private static String toType(Path path) {
        return stream(path.normalize().toString().split("/"))
            .map(ExplodeTransformer::toCamel)
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

    @Getter
    private final Map<Path, Schema> files = new HashMap<>();
    private final Map<String, Schema> extracted = new HashMap<>();
    private final Set<String> opTypes = new HashSet<>();
    private final Path targetFile;
    private final Path targetName;
    private final Path targetPath;

    private final Path schemasPath;

    private final PropUtils pu = new PropUtils().lenient();

    @Getter
    private OpenAPI openAPI;
    private Map<Pattern, String> rename;

    public ExplodeTransformer(@NonNull Path targetFile, @NonNull Path schemasPath) {
        if (schemasPath.isAbsolute()) {
            throw new IllegalArgumentException("Map<String, Schema> path cannot be absolute");
        }

        this.targetFile = targetFile.normalize();
        this.targetName = targetFile.getFileName();
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
    public void transform(@NonNull OpenAPI openAPI, Map<String, Object> options) {
        if (this.openAPI != null) {
            throw new IllegalStateException(format("Instances of %s are not reusable.", getClass().getSimpleName()));
        }

        this.openAPI = openAPI;
        this.rename = ((Map<Pattern, String>) options.getOrDefault("rename", emptyMap()));
        final Map<String, Schema> schemas = new PropUtils().create().get(openAPI, "components.schemas");

        transformSchemas(this.targetName, schemas);

        this.pu.ifPresent(openAPI, "paths", (io.swagger.v3.oas.models.Paths paths) -> {
            paths.forEach((path, item) -> {
                for (final HttpMethod method : HttpMethod.values()) {
                    this.pu.ifPresent(item, method.name().toLowerCase(),
                        (Operation op) -> transformOperation(method, path, op));
                }
            });
        });

        schemas.putAll(this.extracted);
    }

    private void transformOperation(HttpMethod method, String uri, Operation op) {
        this.pu
            .ifPresent(op, "requestBody.content", (Content content) -> {
                transformContent(method, uri, op, content, "request");
            })
            .ifPresent(op, "responses", (ApiResponses responses) -> {
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

    private void transformContent(HttpMethod method, String uri, Operation op, Content content, String... suffixes) {
        content.forEach((name, media) -> {
            this.pu.ifPresent(media, "schema", (Schema schema) -> {
                transformSchema(this.targetName, buildTypeName(method, uri, op, suffixes), schema, media::setSchema);
            });
        });
    }

    private void transformSchemas(Path base, Map<String, Schema> schemas) {
        schemas.forEach((name, schema) -> {
            transformSchema(base, name, schema, s -> schemas.put(name, s));
        });
    }

    private void transformSchema(Path base, String name, Schema schema, Consumer<Schema> set) {
        if (isNotBlank(schema.get$ref())) {
            set.accept(schema);
        } else {
            // if (schema instanceof ComposedSchema) {
            // final ComposedSchema cs = (ComposedSchema) schema;
            //
            // ofNullable(cs.getAllOf()).ifPresent(scms -> {
            // final List<Schema> all = new ArrayList<>();
            //
            // scms.forEach(scm -> {
            // transformSchema(base, "all-of", scm, all::add);
            // });
            //
            // cs.setAllOf(all);
            // });
            // ofNullable(cs.getAnyOf()).ifPresent(scms -> {
            // final List<Schema> all = new ArrayList<>();
            //
            // scms.forEach(scm -> {
            // transformSchema(base, "all-of", scm, all::add);
            // });
            //
            // cs.setAnyOf(all);
            // });
            // ofNullable(cs.getOneOf()).ifPresent(scms -> {
            // final List<Schema> all = new ArrayList<>();
            //
            // scms.forEach(scm -> {
            // transformSchema(base, "all-of", scm, all::add);
            // });
            //
            // cs.setOneOf(all);
            // });
            // } else
            if (schema instanceof ArraySchema) {
                final ArraySchema as = (ArraySchema) schema;

                ofNullable(as.getItems()).ifPresent(scm -> {
                    transformSchema(base, "items", scm, as::setItems);
                });
            } else if (isNotEmpty(schema.getProperties())) {
                final String type = typeOf(base, name, schema);
                final Path file = fileOf(base, name, schema);

                log.debug("base = {}, type = {}, file = {}", base, type, file);

                this.extracted.put(type, new Schema<>().$ref(file.toString()));
                this.files.put(file, schema);

                if (isBlank(schema.getTitle())) {
                    schema.setTitle(type);
                }

                transformSchemas(file, schema.getProperties());
                updateRefs(file, schema);

                set.accept(new Schema<>().$ref("#/components/schemas/" + type));
            } else {
                this.pu.ifPresent(schema, "enum", unused -> {
                    final String type = typeOf(base, name, schema);
                    final Path file = fileOf(base, name, schema);

                    log.debug("base = {}, type = {}, file = {}", base, type, file);

                    if (isBlank(schema.getTitle())) {
                        schema.setTitle(type);
                    }

                    this.extracted.put(type, new Schema<>().$ref(file.toString()));
                    this.files.put(file, schema);

                    set.accept(new Schema<>().$ref("#/components/schemas/" + type));
                });
            }
        }
    }

    // private Schema transformSchema(Path base, String name, Schema schema) {
    // final String item = toHyphen(name);
    // final String type = toType(base.resolve(item));
    // final Path path = resolvePath(base, item);
    // final Path file = this.schemasPath.resolve(path + ".yaml");
    //
    // log.debug("base = {}, item = {}, file = {}, type = {}", base, item, file, type);
    //
    // transformSchemas(path, schema.getProperties());
    //
    // if (schema instanceof ComposedSchema) {
    // final ComposedSchema cs = (ComposedSchema) schema;
    //
    // // transformSchemas(path, "all-of", cs.getAllOf());
    // }
    //
    // if (isBlank(schema.getTitle())) {
    // schema.setTitle(type);
    // }
    //
    // updateRefs(base, file, schema);
    //
    // this.extracted.put(type, new Schema<>().$ref(file.toString()));
    // this.files.put(file, schema);
    //
    // return new Schema<>().$ref("#/components/schemas/" + type);
    // }

    private String typeOf(Path baseFile, String name, Schema schema) {
        return ofNullable(schema.getTitle())
            .orElseGet(() -> typeOf(baseFile) + toCamel(toHyphen(name)));
    }

    private Path fileOf(Path base, String name, Schema schema) {
        final String baseType = typeOf(base);
        final String thisType = ofNullable(schema.getTitle()).orElse(name);
        Path file;

        if (isNotEmpty(baseType) && !thisType.startsWith(baseType)) {
            final String fileName = base.getFileName().toString().replace(".yaml", "/"
                + toHyphen(thisType) + ".yaml");

            file = this.schemasPath.relativize(base.resolveSibling(fileName));
        } else {
            file = Paths.get(toHyphen(thisType) + ".yaml");
        }

        // Path file = this.schemasPath.relativize(base).resolveSibling(name + ".yaml");

        if (this.rename.size() > 0) {
            file = this.rename.entrySet().stream().reduce(file, (p, e) -> {
                final Matcher m = e.getKey().matcher(p.toString());

                return m.matches()
                    ? Paths.get(m.replaceAll(e.getValue()))
                    : p;
            }, (t, u) -> u);
        }

        return this.schemasPath.resolve(file).normalize();
    }

    private String typeOf(Path file) {
        return ofNullable(this.files.get(file)).map(Schema::getTitle).orElse("");
    }

    private void updateRefs(Path base, Schema schema) {
        if (isLocalRef(schema.get$ref())) {
            updateRef(base, schema);
        } else {
            this.pu
                .ifPresent(schema, "properties", (Map<String, Schema> m) -> updateRefs(base, m.values()))
                .ifPresent(schema, "allOf", (Collection all) -> updateRefs(base, all))
                .ifPresent(schema, "anyOf", (Collection all) -> updateRefs(base, all))
                .ifPresent(schema, "oneOf", (Collection all) -> updateRefs(base, all));
        }
    }

    private void updateRefs(Path base, Collection<Schema> schemas) {
        schemas.stream()
            .filter(s -> isLocalRef(s.get$ref()))
            .forEach(s -> updateRef(base, s));
    }

    private void updateRef(Path base, Schema schema) {
        final String $ref = schema.get$ref();
        final String link;

        switch ($ref.indexOf("#/")) {
            case 0:
                // link = this.targetPath.resolve(base.relativize(safeParent(file))).relativize(this.targetFile) +
                // $ref;
                link = this.targetPath
                    .resolve(safeParent(base))
                    .normalize()
                    .relativize(this.targetFile)
                    + $ref;
                break;

            case -1:
                final Path from = this.targetPath.resolve($ref).normalize();

                link = this.targetPath.resolve(this.schemasPath).resolve(base).normalize().relativize(from).toString();
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

        log.debug("{}: relocated {} to {}", base, $ref, link);
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
