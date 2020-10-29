package com.backbase.oss.boat.transformers;

import com.google.common.collect.ImmutableSet;
import io.swagger.v3.core.util.RefUtils;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.media.Schema;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class UnAliasTransformer implements Transformer {

    private static final Set<String> NON_ALIAS_TYPES = ImmutableSet.<String>builder().add(
        "object", "array", "map").build();

    @Override
    public void transform(OpenAPI openAPI, Map<String, Object> options) {

        OpenApiStreamUtil.streamSchemas(openAPI)
            .forEach(schema -> unAliasType(schema, openAPI));
    }

    private void unAliasType(Schema schema, OpenAPI openAPI) {
        log.info("Processing {}, ref {}", schema.getName(), schema.get$ref());
        if (schema.get$ref() == null) {
            return;
        }
        if (!schema.get$ref().startsWith("#/components/schemas")) {
            log.warn("Only unalias dereferenced schema. Cannot process reference {} in {}",
                schema.get$ref(), schema.getName());
            return;
        }
        Schema referredSchema = openAPI.getComponents().getSchemas().get(
            RefUtils.extractSimpleName(schema.get$ref()).getLeft());
        if (referredSchema == null) {
            log.warn("Referred schema {} not found in {}", schema.get$ref(), schema.getName());
            return;
        }
        if (!isAliasOfSimpleTypes(referredSchema)) {
            return;
        }
        unAlias(schema, referredSchema);
    }

    private static void unAlias(Schema schema, Schema alias) {
        log.info("{} refers to alias type {}", schema.getName(), schema.get$ref());
        schema.set$ref(null);
        schema.setType(alias.getType());
        dontOverride(schema::getName, schema::setName, alias::getName);
        dontOverride(schema::getTitle, schema::setTitle, alias::getTitle);
        dontOverride(schema::getMultipleOf, schema::setMultipleOf, alias::getMultipleOf);
        dontOverride(schema::getMaximum, schema::setMaximum, alias::getMaximum);
        dontOverride(schema::getExclusiveMaximum, schema::setExclusiveMaximum, alias::getExclusiveMaximum);
        dontOverride(schema::getMinimum, schema::setMinimum, alias::getMinimum);
        dontOverride(schema::getExclusiveMinimum, schema::setExclusiveMinimum, alias::getExclusiveMinimum);
        dontOverride(schema::getMaxLength, schema::setMaxLength, alias::getMaxLength);
        dontOverride(schema::getMinLength, schema::setMinLength, alias::getMinLength);
        dontOverride(schema::getPattern, schema::setPattern, alias::getPattern);
        dontOverride(schema::getMaxItems, schema::setMaxItems, alias::getMaxItems);
        dontOverride(schema::getMinItems, schema::setMinItems, alias::getMinItems);
        dontOverride(schema::getUniqueItems, schema::setUniqueItems, alias::getUniqueItems);
        dontOverride(schema::getMaxProperties, schema::setMaxProperties, alias::getMaxProperties);
        dontOverride(schema::getMinProperties, schema::setMinProperties, alias::getMinProperties);
        dontOverride(schema::getRequired, schema::setRequired, alias::getRequired);
        dontOverride(schema::getNot, schema::setNot, alias::getNot);

        dontOverride(schema::getNot, schema::setNot, alias::getNot); 
        dontOverride(schema::getProperties, schema::setProperties, alias::getProperties); 
        dontOverride(schema::getAdditionalProperties, schema::setAdditionalProperties, alias::getAdditionalProperties); 
        dontOverride(schema::getDescription, schema::setDescription, alias::getDescription); 
        dontOverride(schema::getFormat, schema::setFormat, alias::getFormat); 
        dontOverride(schema::getNullable, schema::setNullable, alias::getNullable); 
        dontOverride(schema::getReadOnly, schema::setReadOnly, alias::getReadOnly); 
        dontOverride(schema::getWriteOnly, schema::setWriteOnly, alias::getWriteOnly); 
        dontOverride(schema::getExample, schema::setExample, alias::getExample); 
        dontOverride(schema::getExternalDocs, schema::setExternalDocs, alias::getExternalDocs); 
        dontOverride(schema::getDeprecated, schema::setDeprecated, alias::getDeprecated); 
        dontOverride(schema::getXml, schema::setXml, alias::getXml); 
        dontOverride(schema::getExtensions, schema::setExtensions, alias::getExtensions); 
        dontOverride(schema::getEnum, schema::setEnum, alias::getEnum);
        dontOverride(schema::getDiscriminator, schema::setDiscriminator, alias::getDiscriminator); 
        dontOverride(schema::getExampleSetFlag, schema::setExampleSetFlag, alias::getExampleSetFlag);
    }

    private static <T> void dontOverride(Supplier<T> oGet, Consumer<T> oSet, Supplier<T> aGet) {
        if (oGet.get() != null) {
            return;
        }
        oSet.accept(aGet.get());
    }

    private static Boolean isAliasOfSimpleTypes(Schema schema) {
        return !NON_ALIAS_TYPES.contains(schema.getType());
    }
}
