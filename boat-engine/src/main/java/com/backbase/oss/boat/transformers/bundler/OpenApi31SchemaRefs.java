package com.backbase.oss.boat.transformers.bundler;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.media.Discriminator;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.parser.ResolverCache;
import io.swagger.v3.parser.models.RefFormat;
import io.swagger.v3.parser.processors.ExternalRefProcessor;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;

import static io.swagger.v3.parser.util.RefUtils.computeRefFormat;
import static io.swagger.v3.parser.util.RefUtils.isAnExternalRefFormat;

/**
 * Bundles the references that swagger-parser does not follow in OpenAPI 3.1 schemas.
 *
 * <p>swagger-parser follows the references under {@code allOf}, {@code anyOf}, {@code oneOf} and {@code items} only
 * for schemas parsed as {@code ComposedSchema} and {@code ArraySchema}. OpenAPI 3.1 schemas are parsed as
 * {@code JsonSchema}, so those references, and those under the other JSON Schema keywords, stay relative to the file
 * they were written in, as do the discriminator mappings of nested schemas.</p>
 */
final class OpenApi31SchemaRefs {

    private static final String COMPONENTS_SCHEMAS = "#/components/schemas/";

    /**
     * Where a subschema of a loaded schema sits, as far as swagger-parser's {@link ExternalRefProcessor} is concerned.
     */
    private enum Position {
        /** The loaded schema. */
        ROOT,
        /** A property, or the additional properties of a property, of a property... of the loaded schema. */
        PROPERTY,
        /** The additional properties of the loaded schema. */
        ROOT_MAP,
        /** The additional properties of the additional properties of the loaded schema. */
        ROOT_MAP_VALUE,
        /** Anywhere else; swagger-parser leaves these references alone. */
        HIDDEN;

        Position property() {
            return this == ROOT || this == PROPERTY ? PROPERTY : HIDDEN;
        }

        Position additionalProperties() {
            switch (this) {
                case ROOT:
                    return ROOT_MAP;
                case ROOT_MAP:
                    return ROOT_MAP_VALUE;
                case PROPERTY:
                    return PROPERTY;
                default:
                    return HIDDEN;
            }
        }
    }

    private final Set<Schema<?>> rebased = Collections.newSetFromMap(new IdentityHashMap<>());

    static boolean isOpenApi31(OpenAPI openAPI) {
        return openAPI != null && openAPI.getOpenapi() != null && openAPI.getOpenapi().startsWith("3.1");
    }

    /**
     * Rebases the references that swagger-parser skips in a schema loaded from {@code file} onto the root document, as
     * swagger-parser does for the references it follows. {@link #resolve} then turns them into component references.
     *
     * <p>The references swagger-parser follows must be left alone: it reads an internal reference there as one into
     * {@code file}.</p>
     */
    void rebase(Schema<?> loaded, String file) {
        rebase(loaded, file, Position.ROOT);
    }

    private void rebase(Schema<?> schema, String file, Position position) {
        if (schema == null || !rebased.add(schema)) {
            return;
        }
        if (position == Position.HIDDEN && schema.get$ref() != null) {
            schema.set$ref(rebase(schema.get$ref(), file));
        }
        // swagger-parser rebases the discriminator mapping of the loaded schema only
        if (position != Position.ROOT && schema.getDiscriminator() != null
            && schema.getDiscriminator().getMapping() != null) {
            schema.getDiscriminator().getMapping().replaceAll((key, ref) -> rebase(ref, file));
        }
        if (schema.getProperties() != null) {
            schema.getProperties().values().forEach(property -> rebase(property, file, position.property()));
        }
        if (schema.getAdditionalProperties() instanceof Schema) {
            rebase((Schema<?>) schema.getAdditionalProperties(), file, position.additionalProperties());
        }
        forEachKeywordSchema(schema, subschema -> rebase(subschema, file, Position.HIDDEN));
    }

    private static String rebase(String ref, String file) {
        RefFormat format = computeRefFormat(ref);
        if (format == RefFormat.URL) {
            return ref;
        }
        return format == RefFormat.INTERNAL ? file + ref : ExternalRefProcessor.join(file, ref);
    }

    /**
     * Replaces the external references left in the component schemas, and those in the schemas they add, with
     * component references.
     */
    static void resolve(OpenAPI openAPI, ResolverCache cache) {
        if (openAPI.getComponents() == null || openAPI.getComponents().getSchemas() == null) {
            return;
        }
        ExternalRefProcessor processor = new ExternalRefProcessor(cache, openAPI);
        Set<Schema<?>> visited = Collections.newSetFromMap(new IdentityHashMap<>());
        Set<String> resolved = new HashSet<>();
        List<String> pending = new ArrayList<>(openAPI.getComponents().getSchemas().keySet());
        while (!pending.isEmpty()) {
            for (String name : pending) {
                if (resolved.add(name)) {
                    resolve(openAPI.getComponents().getSchemas().get(name), processor, visited);
                }
            }
            pending = new ArrayList<>(openAPI.getComponents().getSchemas().keySet());
            pending.removeAll(resolved);
        }
    }

    private static void resolve(Schema<?> schema, ExternalRefProcessor processor, Set<Schema<?>> visited) {
        if (schema == null || !visited.add(schema)) {
            return;
        }
        if (schema.get$ref() != null) {
            schema.set$ref(resolve(schema.get$ref(), processor));
        }
        Discriminator discriminator = schema.getDiscriminator();
        if (discriminator != null && discriminator.getMapping() != null) {
            discriminator.getMapping().replaceAll((key, ref) -> resolve(ref, processor));
        }
        if (schema.getProperties() != null) {
            schema.getProperties().values().forEach(property -> resolve(property, processor, visited));
        }
        if (schema.getAdditionalProperties() instanceof Schema) {
            resolve((Schema<?>) schema.getAdditionalProperties(), processor, visited);
        }
        forEachKeywordSchema(schema, subschema -> resolve(subschema, processor, visited));
    }

    private static String resolve(String ref, ExternalRefProcessor processor) {
        RefFormat format = computeRefFormat(ref);
        if (!isAnExternalRefFormat(format)) {
            return ref;
        }
        String name = processor.processRefToExternalSchema(ref, format);
        // swagger-parser returns the reference itself when it cannot load it
        return ref.equals(name) ? ref : COMPONENTS_SCHEMAS + name;
    }

    /**
     * Visits the subschemas of {@code schema} other than its properties and additional properties.
     */
    private static void forEachKeywordSchema(Schema<?> schema, Consumer<Schema<?>> action) {
        forEach(schema.getAllOf(), action);
        forEach(schema.getAnyOf(), action);
        forEach(schema.getOneOf(), action);
        forEach(schema.getPrefixItems(), action);
        forEach(values(schema.getPatternProperties()), action);
        forEach(values(schema.getDependentSchemas()), action);
        forEach(Arrays.<Schema<?>>asList(schema.getItems(), schema.getNot(), schema.getContains(), schema.getIf(),
            schema.getThen(), schema.getElse(), schema.getPropertyNames(), schema.getAdditionalItems(),
            schema.getUnevaluatedItems(), schema.getUnevaluatedProperties(), schema.getContentSchema()), action);
    }

    private static Collection<Schema> values(Map<String, Schema> schemas) {
        return schemas == null ? null : schemas.values();
    }

    @SuppressWarnings("rawtypes")
    private static void forEach(Collection<? extends Schema> schemas, Consumer<Schema<?>> action) {
        if (schemas != null) {
            schemas.stream().filter(Objects::nonNull).forEach(subschema -> action.accept(subschema));
        }
    }
}
