package com.backbase.oss.boat;

import static com.backbase.oss.boat.JsonSchemaToOpenApi.X_JAVA_ENUM_NAMES;
import static com.backbase.oss.boat.JsonSchemaToOpenApi.X_JAVA_TYPE;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.google.common.base.CaseFormat;
import com.google.common.collect.BiMap;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.media.ArraySchema;
import io.swagger.v3.oas.models.media.ComposedSchema;
import io.swagger.v3.oas.models.media.Schema;
import java.io.File;
import java.net.URI;
import java.net.URL;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.SneakyThrows;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


@SuppressWarnings({"Duplicates", "java:S3776", "java:S3740"})
public class Utils {

    private static final Logger log = LoggerFactory.getLogger(Utils.class);

    private Utils() {
        throw new UnsupportedOperationException("private constructor");
    }


    @SneakyThrows
    static URL getAbsoluteReference(URL base, String ref) {
        URI uri = base.toURI().resolve(ref);
        log.trace("Resolved: {} from {} and {}", uri, base, ref);
        File file = new File(uri.toURL().getFile());
        if (!file.exists()) {
            throw new DerefenceException("File does not exist: " + file.getAbsolutePath());
        }
        if (file.isDirectory()) {
            throw new DerefenceException("File is a directory" + file.getAbsolutePath());
        }
        return uri.normalize().toURL();
    }

    @SneakyThrows
    static boolean isDirectory(URL base, String ref) {
        URI uri = base.toURI().resolve(ref);
        File file = new File(uri.toURL().getFile());

        boolean directory = file.isDirectory();
        log.trace("isDirectory: {} $ref: {} = {}", base, ref, directory);

        return directory;
    }

    @SneakyThrows
    static boolean isFragment(String ref) {
        return ref.startsWith("#");
    }

    @SneakyThrows
    static boolean isAbsolute(String ref) {
        return new URI(ref).isAbsolute();
    }


    @SneakyThrows
    static boolean hasFragment(String ref) {
        return ref.contains("#");
    }

    @SneakyThrows
    static URL getAbsoluteReferenceParent(String absoluteReference) {
        return getAbsoluteReferenceParent(new URL(absoluteReference));
    }


    @SneakyThrows
    static URL getAbsoluteReferenceParent(URL absoluteReference) {
        URI uri = absoluteReference.toURI();
        if (uri.getFragment() != null) {
            return uri.toURL();
        }
        URI parent = uri.getPath().endsWith("/") ? uri.resolve("..") : uri.resolve(".");
        return parent.toURL();
    }

    static Schema resolveSchemaByJavaType(JsonNode type, Components components) {
        String typeName = "javaType";
        if (type.hasNonNull(typeName) && !type.get(typeName).textValue().startsWith("java")) {
            log.debug("Resolving Schema Type from javaType: {}", type.get(typeName).textValue());
            final String javaType = type.get(typeName).textValue();
            Optional<io.swagger.v3.oas.models.media.Schema> first = components.getSchemas().values().stream()
                .filter(schema -> schema.getExtensions() != null && javaType
                    .equals(schema.getExtensions().get(X_JAVA_TYPE)))
                .findFirst();
            if (first.isPresent()) {
                log.debug("Found Schema: {} for javaType: {} ", first.get().getName(), javaType);
                return first.get();
            }
        }
        return null;
    }

    static Optional<String> getSchemaNameFromJavaClass(JsonNode type) {
        if (type.hasNonNull("javaType")) {
            log.debug("javaType: {}", type.get("javaType").textValue());
            final String javaType = type.get("javaType").textValue();
            if (!javaType.startsWith("java.")) {
                return Optional.of(StringUtils.substringAfterLast(javaType, "."));
            }
        }
        return Optional.empty();

    }

    static String getSchemaNameFromReference(URL absoluteReference, String parentSchemaName,
        BiMap<String, String> referenceNames) {
        return getSchemaNameFromReference(absoluteReference.toString(), parentSchemaName, referenceNames);
    }

    // Ensure that each name resolved from a json reference is unique
    @SneakyThrows
    static String getSchemaNameFromReference(String reference, String parentSchemaName,
        BiMap<String, String> referenceNames) {
        String proposedName = getProposedSchemaName(reference);

        String existingName = referenceNames.get(reference);
        String existingRef = referenceNames.inverse().get(proposedName);

        String name;
        if (existingName == null && existingRef == null) {
            try {
                referenceNames.put(reference, proposedName);
            } catch (IllegalArgumentException ex) {
                log.error("thingy already exists");
            }
            name = proposedName;
        } else if (existingName != null && existingRef != null
            && existingName.equals(proposedName)
            && existingRef.equals(reference)) {
            name = proposedName;
        } else {
            if (isUrl(reference)) {
                URL parent = getAbsoluteReferenceParent(reference);
                String parentReference = StringUtils.stripEnd(parent.toString(), "/");
                String parentName = getProposedSchemaName(parentReference);
                String newName = parentName + proposedName;
                log.warn("Schema Name already exists for: {} Using: {}", proposedName, newName);
                proposedName = newName;
                referenceNames.put(reference, proposedName);
                name = proposedName;
            } else {
                String newName = proposedName + "Duplicate";
                log.warn("Schema Name already exists for{} Using: {}", proposedName, newName);
                referenceNames.put(reference, newName);
                name = newName;
            }
        }
        return name;
    }

    protected static String getProposedSchemaName(String absoluteReference) {
        String proposedName = absoluteReference;
        if (proposedName.contains("/")) {
            proposedName = StringUtils.substringAfterLast(proposedName, "/");
        }
        proposedName = StringUtils.substringBeforeLast(proposedName, ".");

        return normalizeSchemaName(proposedName);
    }

    public static String normalizeSchemaName(String name) {
        if (name.contains("-")) {
            name = CaseFormat.LOWER_HYPHEN.to(CaseFormat.UPPER_CAMEL, name);
        } else {
            name = CaseFormat.LOWER_CAMEL.to(CaseFormat.UPPER_CAMEL, name);
        }

        name = name.replaceAll("[^A-Za-z0-9]", "");
        name = org.apache.commons.lang3.StringUtils.deleteWhitespace(name);

        return name;
    }

    public static String normalizeDisplayName(String name) {
        name = name.replaceAll("[^A-Za-z0-9]", "");
        return name;
    }

    protected static Set<String> getPlaceholders(String url) {
        Matcher m = Pattern.compile("\\{(.*?)\\}").matcher(url);
        Set<String> placeholders = new LinkedHashSet<>();
        while (m.find()) {
            String match = m.group(0);
            placeholders.add(match.substring(1, match.length() - 1));
        }
        return placeholders;
    }

    protected static boolean isUrl(String url) {
        try {
            new URL(url).toURI();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public static void cleanUp(Schema schema, boolean removeJavaExtensions) {
        if (schema == null) {
            return;
        }
        if (schema.getExtensions() != null) {
            schema.getExtensions().remove(JsonSchemaToOpenApi.X_RAML_TYPE);
            schema.getExtensions().remove(JsonSchemaToOpenApi.X_RAML_BASE);
            schema.getExtensions().remove(JsonSchemaToOpenApi.X_RAML_PARENT);
            schema.getExtensions().remove(JsonSchemaToOpenApi.X_RAML_EXTENDS);

            if (removeJavaExtensions) {
                schema.getExtensions().remove(X_JAVA_TYPE);
                schema.getExtensions().remove(X_JAVA_ENUM_NAMES);
            }
        }

        if (schema.getProperties() != null) {
            schema.getProperties().forEach((s, prop) -> cleanUp((Schema) prop, removeJavaExtensions));
        }

        if (schema instanceof ArraySchema) {
            cleanUp(((ArraySchema) schema).getItems(), removeJavaExtensions);
        }

        if (schema instanceof ComposedSchema) {
            ComposedSchema composedSchema = (ComposedSchema) schema;
            if (composedSchema.getAllOf() != null) {
                composedSchema.getAllOf().forEach(schema1 -> cleanUp(schema1, removeJavaExtensions));
            }
            if (composedSchema.getAnyOf() != null) {
                composedSchema.getAnyOf().forEach(schema1 -> cleanUp(schema1, removeJavaExtensions));
            }
            if (composedSchema.getOneOf() != null) {
                composedSchema.getOneOf().forEach(schema1 -> cleanUp(schema1, removeJavaExtensions));
            }
        }
    }

    public static ObjectMapper createObjectMapper() {
        YAMLFactory yamlFactory = new YAMLFactory();
        yamlFactory.enable(JsonParser.Feature.ALLOW_UNQUOTED_CONTROL_CHARS);
        return new ObjectMapper(yamlFactory);
    }
}
