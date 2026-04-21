package com.backbase.oss.boat;

import com.google.common.base.CaseFormat;
import com.google.common.collect.BiMap;
import lombok.SneakyThrows;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.util.stream.Stream;


@SuppressWarnings({"Duplicates", "java:S3776", "java:S3740"})
public class Utils {

    private static final Logger log = LoggerFactory.getLogger(Utils.class);

    private Utils() {
        throw new UnsupportedOperationException("private constructor");
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


    protected static boolean isUrl(String url) {
        try {
            new URL(url).toURI();
            return true;
        } catch (Exception e) {
            return false;
        }
    }





    public static String[] selectInputs(Path inputPath, String glob) throws IOException {
        final PathMatcher matcher = inputPath
            .getFileSystem()
            .getPathMatcher("glob:" + glob);

        try (Stream<Path> paths = Files.list(inputPath)) {
            return paths
                .map(inputPath::relativize)
                .filter(matcher::matches)
                .map(Path::toString)
                .toArray(String[]::new);
        }
    }
}
