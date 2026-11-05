package com.backbase.oss.boat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;

/**
 * Detects whether a specification file is an OpenAPI or an AsyncAPI document from its root version key.
 *
 * <p>Files that are neither (for example shared schema fragments under {@code lib/}) are reported as
 * {@link #UNKNOWN}.
 */
@Slf4j
public enum SpecFormat {
    OPENAPI,
    ASYNCAPI,
    UNKNOWN;

    private static final Pattern YAML_ROOT_KEY = Pattern.compile("^[\"']?(openapi|swagger|asyncapi)[\"']?\\s*:");

    public static SpecFormat detect(File file) {
        if (file == null || !file.isFile()) {
            return UNKNOWN;
        }
        try {
            return detect(new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8));
        } catch (IOException e) {
            log.warn("Cannot read {} to detect its specification format", file, e);
            return UNKNOWN;
        }
    }

    public static SpecFormat detect(String content) {
        if (content == null) {
            return UNKNOWN;
        }
        String trimmed = content.trim();
        if (trimmed.startsWith("{")) {
            return detectJson(trimmed);
        }
        try (BufferedReader reader = new BufferedReader(new StringReader(content))) {
            String line;
            while ((line = reader.readLine()) != null) {
                Matcher matcher = YAML_ROOT_KEY.matcher(line);
                if (matcher.find()) {
                    return fromKey(matcher.group(1));
                }
            }
        } catch (IOException e) {
            log.debug("Cannot read content to detect its specification format", e);
        }
        return UNKNOWN;
    }

    public boolean isAsyncApi() {
        return this == ASYNCAPI;
    }

    private static SpecFormat detectJson(String content) {
        try {
            JsonNode root = new ObjectMapper().readTree(content);
            if (root.has("asyncapi")) {
                return ASYNCAPI;
            }
            if (root.has("openapi") || root.has("swagger")) {
                return OPENAPI;
            }
        } catch (IOException e) {
            log.debug("Content is not valid JSON", e);
        }
        return UNKNOWN;
    }

    private static SpecFormat fromKey(String key) {
        return "asyncapi".equals(key) ? ASYNCAPI : OPENAPI;
    }
}
