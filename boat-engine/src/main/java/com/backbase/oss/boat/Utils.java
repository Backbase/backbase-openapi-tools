package com.backbase.oss.boat;

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
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.SneakyThrows;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


@SuppressWarnings("Duplicates")
public class Utils {

    private static final Logger log = LoggerFactory.getLogger(Utils.class);

    private Utils() {
        throw new UnsupportedOperationException("private constructor");
    }


    @SneakyThrows
    static URL getAbsoluteReference(URL base, String ref) {
        URI uri = base.toURI().resolve(ref);
        log.debug("Resolved: {} from {} and {}", uri, base, ref);
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
        log.debug("isDirectory: {} $ref: {} = {}", base, ref, directory);

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
        if (type.hasNonNull("javaType") && !type.get("javaType").textValue().startsWith("java")) {
            log.debug("Resolving Schema Type from javaType: {}", type.get("javaType").textValue());
            final String javaType = type.get("javaType").textValue();
            Optional<io.swagger.v3.oas.models.media.Schema> first = components.getSchemas().values().stream()
                .filter(schema -> schema.getExtensions() != null && javaType.equals(schema.getExtensions().get(X_JAVA_TYPE)))
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
            if (!javaType.startsWith("java."))
                return Optional.of(StringUtils.substringAfterLast(javaType, "."));
        }
        return Optional.empty();

    }

    static String getSchemaNameFromReference(URL absoluteReference, String parentSchemaName, BiMap<String, String> referenceNames) {
        return getSchemaNameFromReference(absoluteReference.toString(), parentSchemaName, referenceNames);
    }

    // Ensure that each name resolved from a json reference is unique
    @SneakyThrows
    static String getSchemaNameFromReference(String reference, String parentSchemaName, BiMap<String, String> referenceNames) {
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
                if (contentsAreEqual(reference, existingRef)) {

                    if (existingRef != null) {
                        existingName = referenceNames.get(existingRef);
                    }
                    if (existingName.equals(proposedName)) {
                        name = parentSchemaName + proposedName;
                    } else {
                        name = proposedName;
                    }


                } else {
                    URL parent = getAbsoluteReferenceParent(reference);
                    String parentName = getProposedSchemaName(parent.toString().substring(0, parent.toString().length() - 1));
                    String newName = parentName + proposedName;
                    log.warn("Schema Name already exists for: {}! Using: {}", proposedName, newName);
                    proposedName = newName;
                    referenceNames.put(reference, proposedName);
                    name = proposedName;
                }
            } else {
                String newName = proposedName + "Duplicate";
                log.warn("Schema Name already exists for{}! Using: {}", proposedName, newName);
                referenceNames.put(reference, newName);
                name = newName;
            }
        }
        return name;
    }

    private static boolean contentsAreEqual(String reference, String existingName) throws IOException {
        File file1 = new File("file1.txt");
        File file2 = new File("file2.txt");
        boolean isTwoEqual = FileUtils.contentEquals(file1, file2);
        return isTwoEqual;
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

    protected static String normalizeDisplayName(String name) {
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


    public static void cleanUp(Schema schema) {
        if (schema == null) {
            return;
        }
        if (schema.getExtensions() != null) {
            schema.getExtensions().remove(JsonSchemaToOpenApi.X_RAML_TYPE);
            schema.getExtensions().remove(JsonSchemaToOpenApi.X_RAML_BASE);
            schema.getExtensions().remove(JsonSchemaToOpenApi.X_RAML_PARENT);
            schema.getExtensions().remove(JsonSchemaToOpenApi.X_RAML_EXTENDS);
//            schema.getExtensions().remove(Exporter.X_EXAMPLES);

        }

        if (schema.getProperties() != null) {
            schema.getProperties().forEach((s, prop) -> cleanUp((Schema) prop));
        }

        if (schema instanceof ArraySchema) {
            cleanUp(((ArraySchema) schema).getItems());
        }

        if (schema instanceof ComposedSchema) {
            ComposedSchema composedSchema = (ComposedSchema) schema;
            if (composedSchema.getAllOf() != null) {
                composedSchema.getAllOf().forEach(Utils::cleanUp);
            }
            if (composedSchema.getAnyOf() != null) {
                composedSchema.getAnyOf().forEach(Utils::cleanUp);
            }
            if (composedSchema.getOneOf() != null) {
                composedSchema.getOneOf().forEach(Utils::cleanUp);
            }
        }
    }

    public static ObjectMapper createObjectMapper() {

        YAMLFactory yamlFactory = new YAMLFactory();
        yamlFactory.enable(JsonParser.Feature.ALLOW_UNQUOTED_CONTROL_CHARS);

        return new ObjectMapper(yamlFactory);
    }
}
