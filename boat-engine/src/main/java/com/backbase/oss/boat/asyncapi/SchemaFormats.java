package com.backbase.oss.boat.asyncapi;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

/**
 * Message payload and header schema formats that can be projected onto OpenAPI.
 *
 * <p>JSON Schema is accepted as {@code application/schema+json} or {@code application/schema+yaml}, with or without a
 * {@code version} parameter naming one of the {@link Draft drafts}.
 */
final class SchemaFormats {

    /**
     * JSON Schema drafts accepted in the {@code version} parameter of a JSON Schema format.
     */
    enum Draft {
        DRAFT_04("draft-04"),
        DRAFT_06("draft-06"),
        DRAFT_07("draft-07"),
        DRAFT_2019_09("draft-2019-09"),
        DRAFT_2020_12("draft-2020-12");

        private final String version;

        Draft(String version) {
            this.version = version;
        }

        String getVersion() {
            return version;
        }

        /**
         * Whether the draft is the dialect of OpenAPI 3.1, or close enough that only OpenAPI 3.1 expresses it.
         */
        boolean isOpenApi31() {
            return this == DRAFT_2019_09 || this == DRAFT_2020_12;
        }

        static Optional<Draft> fromVersion(String version) {
            return Arrays.stream(values()).filter(draft -> draft.version.equals(version)).findFirst();
        }
    }

    private static final Set<String> JSON_SCHEMA_MEDIA_TYPES = Collections.unmodifiableSet(new LinkedHashSet<>(
        Arrays.asList("application/schema+json", "application/schema+yaml")));

    private static final Set<String> OTHER_FORMATS = Collections.unmodifiableSet(new LinkedHashSet<>(Arrays.asList(
        "application/vnd.aai.asyncapi;version=3.0.0",
        "application/vnd.aai.asyncapi+json;version=3.0.0",
        "application/vnd.aai.asyncapi+yaml;version=3.0.0",
        "application/vnd.aai.asyncapi;version=3.1.0",
        "application/vnd.aai.asyncapi+json;version=3.1.0",
        "application/vnd.aai.asyncapi+yaml;version=3.1.0",
        "application/vnd.oai.openapi;version=3.0.0",
        "application/vnd.oai.openapi+json;version=3.0.0",
        "application/vnd.oai.openapi+yaml;version=3.0.0"
    )));

    /**
     * Human readable list of the supported formats, for messages.
     */
    static final Set<String> SUPPORTED;

    static {
        Set<String> supported = new LinkedHashSet<>(OTHER_FORMATS);
        for (String mediaType : JSON_SCHEMA_MEDIA_TYPES) {
            supported.add(mediaType);
            for (Draft draft : Draft.values()) {
                supported.add(mediaType + ";version=" + draft.getVersion());
            }
        }
        SUPPORTED = Collections.unmodifiableSet(supported);
    }

    private SchemaFormats() {
    }

    static boolean isSupported(String schemaFormat) {
        if (schemaFormat == null) {
            return true;
        }
        String normalized = normalize(schemaFormat);
        if (OTHER_FORMATS.contains(normalized)) {
            return true;
        }
        String[] parts = normalized.split(";");
        if (!JSON_SCHEMA_MEDIA_TYPES.contains(parts[0])) {
            return false;
        }
        String version = versionParameter(parts);
        return version == null || Draft.fromVersion(version).isPresent();
    }

    /**
     * The JSON Schema draft of the format, empty when the format is not JSON Schema or names no version.
     */
    static Optional<Draft> draft(String schemaFormat) {
        if (schemaFormat == null) {
            return Optional.empty();
        }
        String[] parts = normalize(schemaFormat).split(";");
        if (!JSON_SCHEMA_MEDIA_TYPES.contains(parts[0])) {
            return Optional.empty();
        }
        String version = versionParameter(parts);
        return version == null ? Optional.empty() : Draft.fromVersion(version);
    }

    private static String normalize(String schemaFormat) {
        return schemaFormat.replace(" ", "").toLowerCase(Locale.ROOT);
    }

    private static String versionParameter(String[] parts) {
        for (int i = 1; i < parts.length; i++) {
            if (parts[i].startsWith("version=")) {
                return parts[i].substring("version=".length());
            }
        }
        return null;
    }
}
