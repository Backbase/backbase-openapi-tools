package com.backbase.oss.boat.transformers;

import static com.google.common.collect.Maps.newHashMap;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.media.ArraySchema;
import io.swagger.v3.oas.models.media.ComposedSchema;
import io.swagger.v3.oas.models.media.Schema;
import java.util.Map;
import java.util.Map.Entry;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

/**
 * Dereferences openApi.
 *
 * <p>Goes through all the components/schema and recursively dereferences schemas, including properties for objects,
 * items for arrays. Also merges allOf composites.
 *
 */
@Slf4j
public class DereferenceComponentsPropertiesTransformer implements Transformer {

    private static final String COMPONENTS_SCHEMAS_PATH = "#/components/schemas/";

    @Override
    public void transform(OpenAPI openAPI, Map<String, Object> options) {
        if (openAPI.getComponents() == null || openAPI.getComponents().getSchemas() == null) {
            log.debug("nothing to dereference.");
            return;
        }
        // Find all the components referenced from paths.
        openAPI.getComponents().getSchemas().entrySet().stream()
            .forEach(e -> deferenceSchema(e.getValue(), openAPI, e.getKey()));
    }

    private void deferenceSchema(Schema schema, OpenAPI openAPI, String crumb) {
        log.info(crumb);
        if (schema instanceof ComposedSchema) {
            deferenceAllOf((ComposedSchema) schema, openAPI, crumb);
        }
        if (schema instanceof ArraySchema) {
            dereferenceItems((ArraySchema) schema, openAPI, crumb);
        }
        if (schema.getProperties() != null) {
            dereferenceProperties(schema, openAPI, crumb);
        }
    }

    private void dereferenceProperties(Schema schema, OpenAPI openAPI, String crumb) {
        Map<String, Schema> replacements = newHashMap();
        for (Entry<String, Schema> entry : ((Map<String, Schema>) schema.getProperties()).entrySet()) {
            Schema propertySchema = entry.getValue();
            if (propertySchema.get$ref() != null) {
                log.info(crumb + " : Replacing property {} with schema {}", entry.getKey(),
                    propertySchema.get$ref());
                Schema referencedSchema = getSchemaByInternalReference(propertySchema.get$ref(), openAPI);
                replacements.put(entry.getKey(), referencedSchema);
                propertySchema = referencedSchema;
            }
            // recursively dereference
            deferenceSchema(propertySchema, openAPI, crumb + "/" + entry.getKey());
        }
        log.info(crumb +  " : Replacing {} properties", replacements.size());
        schema.getProperties().putAll(replacements);
    }

    private void dereferenceItems(ArraySchema schema, OpenAPI openAPI, String crumb) {
        ArraySchema arraySchema = schema;
        if (arraySchema.getItems().get$ref() != null) {
            log.info(crumb + " : Replacing items with schema {}", arraySchema.getItems().get$ref());
            Schema referencedSchema = getSchemaByInternalReference(arraySchema.getItems().get$ref(), openAPI);
            arraySchema.setItems(referencedSchema);
        }
        deferenceSchema(arraySchema.getItems(), openAPI, crumb + "/items");
    }

    private void deferenceAllOf(ComposedSchema schema, OpenAPI openAPI, String crumb) {
        if (schema.getAllOf() == null) {
            log.warn(crumb + " composite schema without all-of not dereferenced.");
            return;
        }
        schema.getAllOf().stream().forEach(ref -> apply(ref, schema, openAPI, crumb));
        // can we get away with this? or should it be replaced with a proper Schema<Object> ?
        schema.setAllOf(null);
    }

    private void apply(Schema ref, ComposedSchema schema, OpenAPI openAPI, String crumb) {
        Schema refSchema = ref;
        if (ref.get$ref() != null) {
            refSchema = getSchemaByInternalReference(ref.get$ref(), openAPI);
        }
        if (refSchema.getProperties() != null) {
            if (schema.getProperties() == null) {
                schema.setProperties(newHashMap());
            }
            schema.getProperties().putAll(refSchema.getProperties());
        }
        if (refSchema.getRequired() != null) {
            // setRequired is not a simple setter... it will filter the list for non-existing properties
            // .... and set to null for an empty list
            if (schema.getRequired() == null) {
                schema.setRequired(refSchema.getRequired());
            } else {
                // once the list is there you can do whatever you want ;-)
                schema.getRequired().addAll(refSchema.getRequired());
            }
        }
    }

    private Schema getSchemaByInternalReference(String internalReference, OpenAPI openAPI) {
        if (!internalReference.startsWith(COMPONENTS_SCHEMAS_PATH)) {
            throw new IllegalArgumentException(String.format("Not an internal ref %s", internalReference));
        }
        String[] parts = StringUtils.removeStart(internalReference, COMPONENTS_SCHEMAS_PATH).split("/");

        Schema schema = openAPI.getComponents().getSchemas().get(parts[0]);
        if (schema == null) {
            throw new RuntimeException(String.format("No component schema found by name %s", internalReference));
        }

        for (int i = 1; i < parts.length; i++) {
            if (parts[i].equals("properties")) {
                schema = (Schema) schema.getProperties().get(parts[++i]);
            } else if (parts[i].equals("items")) {
                schema = ((ArraySchema) schema).getItems();
            } else {
                throw new RuntimeException("Unable to process $ref " + internalReference);
            }
        }

        if (schema.get$ref() != null) {
            // sometimes refs go wild
            return getSchemaByInternalReference(schema.get$ref(), openAPI);
        }
        return schema;
    }


}
