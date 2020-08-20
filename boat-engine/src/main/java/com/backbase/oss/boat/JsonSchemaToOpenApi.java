package com.backbase.oss.boat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.media.ArraySchema;
import io.swagger.v3.oas.models.media.BooleanSchema;
import io.swagger.v3.oas.models.media.ComposedSchema;
import io.swagger.v3.oas.models.media.DateSchema;
import io.swagger.v3.oas.models.media.DateTimeSchema;
import io.swagger.v3.oas.models.media.EmailSchema;
import io.swagger.v3.oas.models.media.IntegerSchema;
import io.swagger.v3.oas.models.media.NumberSchema;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.raml.v2.api.model.v10.datamodel.JSONTypeDeclaration;

@SuppressWarnings({"unchecked", "WeakerAccess", "rawtypes","java:S3776", "java:S3740"})
@Slf4j
class JsonSchemaToOpenApi {

    private static final String EXTENDS = "extends";
    public static final String X_RAML_TYPE = "x-raml-type";
    public static final String X_RAML_BASE = "x-raml-base";
    public static final String X_RAML_PARENT = "x-raml-parent";
    public static final String X_RAML_EXTENDS = "x-raml-extends";

    public static final String X_JAVA_TYPE = "x-java-type";
    public static final String JAVA_TYPE = "javaType";
    public static final String PROPERTIES = "properties";
    public static final String JAVA_ENUM_NAMES = "javaEnumNames";
    public static final String X_JAVA_ENUM_NAMES = "x-java-enum-names";
    public static final String STRING = "string";
    public static final String FORMAT = "format";
    public static final String TYPE = "type";
    public static final String NUMBER = "number";
    public static final String DATE_TIME = "date-time";
    public static final String REQUIRED = "required";
    public static final String DATETIME_ONLY = "datetime-only";
    public static final String DATETIME = "datetime";
    public static final String DATE = "date";
    public static final String DATE_ONLY = "date-only";
    public static final String DOLLAR_REF = "$ref";

    private final URL baseUrl;
    private final Components components;
    private final Map<String, String> ramlTypeReferences;
    private final Map<String, JsonNode> jsonSchemas;
    private final BiMap<String, String> referenceNames;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public JsonSchemaToOpenApi(URL baseUrl, Components components, Map<String, String> ramlTypeReferences) {
        this.baseUrl = baseUrl;
        this.components = components;
        this.jsonSchemas = new TreeMap<>();
        this.referenceNames = HashBiMap.create();
        this.ramlTypeReferences = ramlTypeReferences;
    }


    public Schema convert(String name, JSONTypeDeclaration typeDeclaration) throws ExportException {
        log.debug("Converting JSONTypeDeclaration: {}", name);
        String ramlRef = ramlTypeReferences.get(name);
        String type = typeDeclaration.type();
        Schema schema;
        String schemaName = Utils.normalizeSchemaName(name);
        try {
            if (ramlRef != null) {
                URL referenceParent = Utils.getAbsoluteReferenceParent(ramlRef);

                schema = components.getSchemas().get(schemaName);
                if (schema == null) {
                    JsonNode jsonSchema = objectMapper.readTree(type);
                    schema = Utils.resolveSchemaByJavaType(jsonSchema, components);
                    if (schema == null) {
                        schema = createNewSchema(null, jsonSchema, schemaName, components, referenceParent);
                        components.addSchemas(schemaName, schema);
                        log.debug("Created new schema from: {} as: {} with ramlRef: {}", name, schemaName, ramlRef);
                    } else {
                        log.debug("Used existing schema: {} resolved by javaType: {}", schemaName,
                            schema.getExtensions().get(X_JAVA_TYPE));
                    }
                } else {
                    log.debug("Using existing schema: {}", schemaName);
                }
            } else {
                JsonNode jsonSchema = objectMapper.readTree(type);
                schema = Utils.resolveSchemaByJavaType(jsonSchema, components);
                if (schema == null) {
                    String referenceName = baseUrl.toString() + "/" + name + ".raml";
                    referenceNames.put(referenceName, name);
                    schema = createNewSchema(null, jsonSchema, name, components, baseUrl);
                    schema.setExample(ExampleUtils.getExampleObject(typeDeclaration.example(), true));
                    components.addSchemas(name, schema);
                }
                log.debug("Created new schema from: {} as: {}", name, schemaName);
                components.addSchemas(schemaName, schema);
            }
        } catch (IOException e) {
            throw new ExportException("Cannot read json schema from type: " + typeDeclaration.name(), e);
        } catch (DerefenceException e) {
            throw new ExportException("Cannot dereference json schema from type: " + typeDeclaration.name(), e);
        } catch (RuntimeException e) {
            throw new ExportException("Runtime exception:", e);
        }

        schema.setExample(ExampleUtils.getExampleObject(typeDeclaration.example(), true));

        return schema;

    }


    public Schema createNewSchema(Schema parent, JsonNode type, String name, Components components, URL baseUrl)
        throws DerefenceException {
        log.debug("Creating Schema for: {} with path: {}", name, baseUrl);
        if (name.contains(" ")) {
            throw new IllegalStateException("name cannot contain spaces");
        }

        Schema schema = Utils.resolveSchemaByJavaType(type, components);
        if (schema != null) {
            return schema;
        }

        String jsonSchemaType = determineJsonSchemaType(type);
        switch (jsonSchemaType) {
            case DATE_TIME:
            case DATETIME_ONLY:
            case DATETIME:
                schema = new DateTimeSchema();
                break;
            case DATE:
            case DATE_ONLY:
                schema = new DateSchema();
                break;
            case "object":
                if (type.get(EXTENDS) != null) {
                    schema = new ComposedSchema();
                    JsonNode extendsNode = type.get(EXTENDS);
                    String extendedSchemaName;
                    if (hasReference(extendsNode)) {
                        String extendsReference = extendsNode.get(DOLLAR_REF).asText();
                        log.debug("Creating Composed Schema for {} with reference: {}", name, extendsReference);

                        URL absoluteReference = resolveAbsoluteReference(parent, extendsReference, baseUrl);
                        URL absoluteReferenceParent = Utils.getAbsoluteReferenceParent(absoluteReference);
                        JsonNode dereferencedExtendedNode = getJsonNode(absoluteReference);

                        JsonNode dereferenced = getJsonNode(absoluteReference);
                        extendedSchemaName = Utils.getSchemaNameFromJavaClass(dereferenced)
                            .orElse(
                                Utils.getSchemaNameFromReference(absoluteReference, schema.getName(), referenceNames));

                        if (extendedSchemaName.equals(name)) {
                            extendedSchemaName += "Parent";
                        }
                        Schema extendedSchema = components.getSchemas().get(extendedSchemaName);
                        if (extendedSchema == null) {
                            log.debug("Creating new schema for extended reference: {} with: {}", absoluteReference,
                                absoluteReferenceParent);
                            extendedSchema = createNewSchema(parent, dereferencedExtendedNode, extendedSchemaName,
                                components, absoluteReferenceParent);
                            components.addSchemas(extendedSchemaName, extendedSchema);
                            schema.addExtension(X_RAML_EXTENDS, extendedSchema);
                        } else {
                            log.debug("Using existing schema: {}", extendedSchema.getName());
                        }
                    } else {
                        extendedSchemaName = name + "Parent";
                        Schema extendedSchema = components.getSchemas().get(extendedSchemaName);
                        if (extendedSchema == null) {
                            extendedSchema = createNewSchema(parent, extendsNode, extendedSchemaName, components,
                                baseUrl);
                            components.addSchemas(extendedSchemaName, extendedSchema);
                        }
                    }
                    ((ComposedSchema) schema)
                        .setAllOf(Collections.singletonList(new ObjectSchema().$ref(extendedSchemaName)));
                } else {
                    schema = new ObjectSchema();
                }
                break;
            case STRING:
            case "time-only":
                schema = new StringSchema();
                break;
            case "email":
                schema = new EmailSchema();
                break;
            case NUMBER:
                schema = new NumberSchema();
                break;
            case "integer":
                schema = new IntegerSchema();
                break;
            case "boolean":
                schema = new BooleanSchema();
                break;
            case "array":
                schema = new ArraySchema();
                JsonNode itemJsonSchema = type.get("items");
                if (hasReference(itemJsonSchema)) {
                    String itemReference = itemJsonSchema.get(DOLLAR_REF).textValue();
                    String absoluteReference;
                    if (StringUtils.isNotEmpty(itemReference) && !Utils.isUrl(itemReference) && !Utils
                        .isFragment(itemReference)) {
                        absoluteReference = Utils.getAbsoluteReference(baseUrl, itemReference).toString();
                    } else if (Utils.isFragment(itemReference) && Utils.isDirectory(baseUrl, itemReference)) {
                        absoluteReference = getReferenceFromParent(itemReference, parent);
                        absoluteReference += itemReference;
                    } else {
                        absoluteReference = itemReference;
                    }
                    if (!absoluteReference.equals(itemReference)) {
                        itemJsonSchema = ((ObjectNode) itemJsonSchema).set(DOLLAR_REF, new TextNode(absoluteReference));
                    }
                    Schema itemSchema = mapProperty(parent, components, name + "Item", (ObjectNode) itemJsonSchema,
                        baseUrl, false);
                    ((ArraySchema) schema).setItems(itemSchema);
                } else {
                    Schema itemSchema = mapProperty(parent, components, name + "Item", (ObjectNode) itemJsonSchema,
                        baseUrl, false);
                    ((ArraySchema) schema).setItems(itemSchema);
                }
                break;

            case "anyOf":
                schema = new ComposedSchema();
                ArrayNode anyOfJsonSchmema = (ArrayNode) type.get(TYPE);
                Iterable<JsonNode> iterable = anyOfJsonSchmema::elements;
                Stream<JsonNode> stream = StreamSupport.stream(iterable.spliterator(), false);
                List<Schema> anyOfSchemas = stream.map(jsonNode -> new Schema().type(jsonNode.textValue()))
                    .collect(Collectors.toList());
                ((ComposedSchema) schema).setAnyOf(anyOfSchemas);
                break;
            default:
                schema = new StringSchema();
        }

        schema.addExtension(X_RAML_BASE, baseUrl);
        schema.addExtension(X_RAML_TYPE, type);
        schema.addExtension(X_RAML_PARENT, parent);
        schema.setName(name);
        schema.setTitle(getString(type, "title"));
        schema.setMinLength(getInteger(type, "minLength"));
        schema.setMaxLength(getInteger(type, "maxLength"));
        schema.setPattern(getString(type, "pattern"));
        schema.setMinimum(getBigDecimal(type, "minimum"));
        schema.setMaximum(getBigDecimal(type, "maximum"));
        String description = getString(type, "description");
        schema.setDescription(description);
        if (description != null && (description.contains("deprecated") || (description.contains("@deprecated")))) {
            schema.setDeprecated(true);
        }

        Map<String, Schema> properties = getProperties(schema, type, components, baseUrl);
        if (schema.getProperties() == null) {
            schema.setProperties(properties);
        } else if (properties != null && properties.size() > 0) {
            schema.getProperties().putAll(properties);
        }

        getRequired(type).ifPresent(schema::setRequired);

        String ref = getString(type, DOLLAR_REF);

        if (StringUtils.isNotEmpty(ref) && !Utils.isUrl(ref) && !Utils.isFragment(ref)) {
            ref = Utils.getAbsoluteReference(baseUrl, ref).toString();
        } else if (StringUtils.isNotEmpty(ref) && Utils.isFragment(ref) && Utils.isDirectory(baseUrl, ref)) {
            ref = getReferenceFromParent(ref, parent);
        }
        schema.set$ref(ref);
        if (type.hasNonNull(JAVA_TYPE)) {
            schema.addExtension(X_JAVA_TYPE, type.get(JAVA_TYPE).textValue());
        }
        if (type.hasNonNull(JAVA_ENUM_NAMES)) {
            schema.addExtension(X_JAVA_ENUM_NAMES, getEnum(type, JAVA_ENUM_NAMES));
        }
        schema.setEnum(getEnum(type));
        return schema;
    }

    @SneakyThrows
    private URL resolveAbsoluteReference(Schema parent, String ref, URL baseUrl) {
        if (StringUtils.isNotEmpty(ref) && !Utils.isUrl(ref) && !Utils.isFragment(ref)) {
            return Utils.getAbsoluteReference(baseUrl, ref);
        } else if (Utils.isFragment(ref) && Utils.isDirectory(baseUrl, ref)) {
            String referenceFromParent = getReferenceFromParent(ref, parent);
            if (referenceFromParent == null) {
                throw new IllegalStateException("Whha??");
            }
            return new URL(referenceFromParent);
        } else if (Utils.isFragment(ref) && Utils.hasFragment(baseUrl.toString())) {
            String base = StringUtils.substringBefore(baseUrl.toString(), "#");
            return new URL(base + ref);
        } else {
            return new URL(ref);
        }
    }

    private boolean hasReference(JsonNode itemJsonSchema) {
        return itemJsonSchema.hasNonNull(DOLLAR_REF);
    }

    private String determineJsonSchemaType(JsonNode type) {
        assert type != null;
        JsonNode jsonType = type.get(TYPE);
        if (jsonType != null && jsonType.isTextual() && !type.has(FORMAT)) {
            return determineTypeFromJavaType(type, jsonType.asText());
        } else if (jsonType != null && type.has(FORMAT)) {
            return type.get(FORMAT).textValue();
        } else if (jsonType != null && jsonType.isArray()) {
            return "anyOf";
        } else if (type.hasNonNull("enum")) {
            return STRING;
        } else if (type.hasNonNull(JAVA_TYPE) || type.hasNonNull(DOLLAR_REF)) {
            return "object";
        } else if (type instanceof ObjectNode && ((ObjectNode) type).size() == 0) {
            return STRING;
        } else {
            log.error("Cannot determine json type from: {}. Guessing string", type);
            return STRING;
        }
    }

    private String determineTypeFromJavaType(JsonNode type, String fallback) {
        if (type.hasNonNull(JAVA_TYPE)) {
            String javaType = type.get(JAVA_TYPE).textValue();
            switch (javaType) {
                case "java.util.Date":
                    Optional<String> format = Optional
                        .ofNullable(type.hasNonNull(FORMAT) ? type.get(FORMAT).textValue() : null);
                    return format.orElseGet(() -> DATE_TIME);
                case "java.lang.Long":
                    return NUMBER;
                default:
                    return fallback;
            }
        } else {
            return fallback;
        }
    }

    private List getEnum(JsonNode type) {
        String propertyName = "enum";
        return getEnum(type, propertyName);
    }

    private List getEnum(JsonNode type, String propertyName) {
        if (type.hasNonNull(propertyName)) {
            JsonNode enumList = type.get(propertyName);
            ArrayList result = new ArrayList();
            ArrayNode arrayNode = (ArrayNode) enumList;

            arrayNode.forEach(jsonNode -> {
                if (jsonNode.isTextual()) {
                    result.add(jsonNode.textValue());
                } else if (jsonNode.isInt()) {
                    result.add(jsonNode.intValue());
                } else if (jsonNode.isBigDecimal()) {
                    result.add(jsonNode.decimalValue());
                } else {
                    throw new UnsupportedOperationException("Haven't come across type enum type: " + type);
                }
            });
            return result;
        } else {
            return Collections.emptyList();
        }
    }

    @SuppressWarnings({"Duplicates","java:S112"})
    private Map<String, Schema> getProperties(Schema parent, JsonNode type, Components components, URL
        baseUrl) {
        Map<String, Schema> properties = new LinkedHashMap<>();

        if (type.hasNonNull(PROPERTIES)) {
            Iterator<Map.Entry<String, JsonNode>> fields = type.get(PROPERTIES).fields();
            fields.forEachRemaining(field -> {
                JsonNode property = field.getValue();
                Schema schema;
                try {
                    schema = mapProperty(parent, components, field.getKey(), (ObjectNode) property, baseUrl, false);
                    properties.put(field.getKey(), schema);
                } catch (DerefenceException e) {
                    log.error("Failed to dereference property: {}", field.getKey(), e);
                    throw new RuntimeException(e);
                }
            });
        }
        if (properties.isEmpty()) {
            return null;
        } else {
            return properties;
        }
    }

    private Schema mapProperty(Schema parent, Components components, String propertyName, ObjectNode jsonSchema,
        URL baseUrl, boolean derefence) throws DerefenceException {
        Schema schema;
        boolean hasJsonRef = jsonSchema.has(DOLLAR_REF) && StringUtils.isNotEmpty(jsonSchema.get(DOLLAR_REF).textValue());

        if (hasJsonRef && !derefence) {
            String reference = jsonSchema.get(DOLLAR_REF).textValue();
            URL absoluteReference = Utils.getAbsoluteReference(baseUrl, reference);
            jsonSchema.set(DOLLAR_REF, new TextNode(absoluteReference.toString()));
            schema = createNewSchema(parent, jsonSchema, propertyName, components, baseUrl);
        } else if (hasJsonRef) {
            String absoluteReference = jsonSchema.get(DOLLAR_REF).textValue();
            log.debug("Dereference jsonSchema: {}", absoluteReference);
            JsonNode dereferencedJsonSchema = getJsonNode(absoluteReference);
            String schemaName = Utils.getSchemaNameFromJavaClass(dereferencedJsonSchema)
                .orElse(Utils.getSchemaNameFromReference(absoluteReference, parent.getName(), referenceNames));
            if (components.getSchemas().containsKey(schemaName)) {
                schema = components.getSchemas().get(schemaName);
            } else {
                schema = Utils.resolveSchemaByJavaType(dereferencedJsonSchema, components);
                if (schema == null) {
                    log.debug("Adding property {} schema to catalog as: {}", propertyName, schemaName);
                    schema = createNewSchema(parent, dereferencedJsonSchema, schemaName, components,
                        Utils.getAbsoluteReferenceParent(absoluteReference));
                    components.addSchemas(schemaName, schema);
                    dereferenceSchema(schema, components);
                } else {
                    log.debug("Reusing schema: {} for property: {}", schema.getName(), propertyName);
                }
            }
            log.debug("Property referenced schema: {}", schema.getName());
        } else {
            schema = createNewSchema(parent, jsonSchema, propertyName, components, baseUrl);
        }
        return schema;
    }

    private Optional<List<String>> getRequired(JsonNode type) {
        if (!type.hasNonNull(PROPERTIES)) {
            return Optional.empty();
        }
        List<String> required = new ArrayList<>();
        type.get(PROPERTIES).fields().forEachRemaining(field -> {
            JsonNode property = field.getValue();
            if (property.hasNonNull(REQUIRED) && property.get(REQUIRED).asBoolean()) {
                required.add(field.getKey());
            }
        });

        if (type.has(REQUIRED)) {
            JsonNode requiredNode = type.get(REQUIRED);
            if (requiredNode.isArray()) {
                requiredNode.elements().forEachRemaining(jsonNode -> required.add(jsonNode.asText()));
            } else {
                log.warn("wut?");
            }
        }
        return required.isEmpty() ? Optional.empty() : Optional.of(required);
    }

    private String getString(JsonNode result, String key) {
        return result.hasNonNull(key) ? result.get(key).textValue() : null;
    }

    private Integer getInteger(JsonNode result, String key) {
        return result.hasNonNull(key) ? result.get(key).asInt() : null;
    }

    private BigDecimal getBigDecimal(JsonNode result, String key) {

        if (result.hasNonNull(key) && result.get(key).isTextual()) {
            String val = result.get(key).textValue();
            try {
                return new BigDecimal(val);
            } catch (Exception e) {
                log.warn("Cannot convert: {} to Big Decimal", val);
            }
        }
        return null;
    }

    void dereferenceSchema(final Schema schema, Components components) throws DerefenceException {
        String ref = schema.get$ref();

        if (ref != null && Utils.isAbsolute(ref)) {
            URL absoluteParentReference = Utils.getAbsoluteReferenceParent(ref);
            log.debug("ref: {}", ref);

            JsonNode dereferenced = getJsonNode(ref);
            String schemaName = Utils.getSchemaNameFromJavaClass(dereferenced)
                .orElse(Utils.getSchemaNameFromReference(ref, schema.getName(), referenceNames));

            log.debug("Dereference schema: {} with ref: {}", schemaName, ref);

            Schema referencedSchema = Utils.resolveSchemaByJavaType(dereferenced, components);
            if (referencedSchema == null) {
                if (components.getSchemas().containsKey(schemaName)) {
                    referencedSchema = components.getSchemas().get(schemaName);
                } else {
                    referencedSchema = createNewSchema(schema, dereferenced, schemaName, components,
                        absoluteParentReference);
                    components.addSchemas(schemaName, referencedSchema);
                    if (hasReference(referencedSchema)) {
                        dereferenceSchema(referencedSchema, components);
                    }
                }
            }
            schema.$ref(referencedSchema.getName());
        }
        if (schema instanceof ArraySchema) {
            dereferenceArraySchema((ArraySchema) schema, components);
        }
        if (schema.getProperties() != null) {
            Map<String, Schema> properties = new LinkedHashMap<>(schema.getProperties());
            for (Map.Entry<String, Schema> entry : properties.entrySet()) {
                Schema value = entry.getValue();
                value.addExtension(X_RAML_PARENT, schema);
                log.debug("Deference item property: {} from: {}", value.getName(), schema.getName());
                dereferenceSchema(value, components);
            }
        }
    }

    @SneakyThrows
    private JsonNode getJsonNode(URL reference) {
        log.debug("Getting json for: {}", reference);
        return getJsonNode(reference.toURI());
    }

    @SneakyThrows
    private JsonNode getJsonNode(String reference) {
        URI ref = new URI(reference);
        return getJsonNode(ref);
    }

    @SneakyThrows
    private JsonNode getJsonNode(URI ref) {

        if (jsonSchemas.containsKey(ref.toString())) {
            log.debug("Returning cached ref: {}", ref);
            return jsonSchemas.get(ref.toString());
        }
        JsonNode dereferenced;
        log.debug("getJsonNode for: {}", ref);

        dereferenced = objectMapper.readTree(ref.toURL());
        if (ref.getFragment() != null) {
            List<String> fragments = Arrays.stream(ref.getFragment().split("/")).filter(StringUtils::isNotEmpty)
                .collect(Collectors.toList());
            for (String fragment : fragments) {
                dereferenced = dereferenced.get(fragment);
            }
        }
        jsonSchemas.put(ref.toString(), dereferenced);
        return dereferenced;
    }

    @SneakyThrows
    private String getReferenceFromParent(String reference, Schema schema) {
        Schema parentSchema = (Schema) schema.getExtensions().get(X_RAML_PARENT);
        while (parentSchema != null && parentSchema.getExtensions() != null) {
            String ref;
            if (parentSchema instanceof ArraySchema) {
                ref = ((ArraySchema) parentSchema).getItems().get$ref();
            } else {
                ref = parentSchema.get$ref();
            }
            if (ref != null && !ref.equals(reference)) {
                return ref;
            }
            parentSchema = (Schema) parentSchema.getExtensions().get(X_RAML_PARENT);
        }
        return null;
    }


    private boolean hasReference(Schema schema) {
        if (schema.get$ref() != null) {
            return true;
        }
        if (schema instanceof ArraySchema && ((ArraySchema) schema).getItems().get$ref() != null) {
            return true;
        }
        if (schema.getProperties() != null && schema.getProperties().values().stream()
            .anyMatch(property -> hasReference((Schema) property))) {
            return true;
        }
        return schema.getExtensions().containsKey(X_RAML_EXTENDS);

    }

    private void dereferenceArraySchema(ArraySchema schema, Components components) throws DerefenceException {
        Schema<?> parent = schema.getItems();
        String reference = parent.get$ref();

        if (parent.getExtensions() != null && reference != null && Utils.isAbsolute(reference)) {
            JsonNode dereferenced = getJsonNode(reference);
            String schemaName = Utils.getSchemaNameFromJavaClass(dereferenced)
                .orElse(Utils.getSchemaNameFromReference(reference, schema.getName(), referenceNames));

            if (schemaName.equals(schema.getName())) {
                schemaName = schemaName + "Item";
            }

            Schema referencedSchema;
            if (components.getSchemas().containsKey(schemaName)) {
                referencedSchema = components.getSchemas().get(schemaName);
            } else {

                referencedSchema = Utils.resolveSchemaByJavaType(dereferenced, components);
                if (referencedSchema == null) {
                    referencedSchema = createNewSchema(schema, dereferenced, schemaName, components,
                        Utils.getAbsoluteReferenceParent(reference));
                    components.addSchemas(schemaName, referencedSchema);
                    if (hasReference(referencedSchema)) {
                        dereferenceSchema(referencedSchema, components);
                    }
                }
            }
            schema.getItems().$ref(referencedSchema.getName());

        } else {
            dereferenceSchema(parent, components);
        }
    }
}
