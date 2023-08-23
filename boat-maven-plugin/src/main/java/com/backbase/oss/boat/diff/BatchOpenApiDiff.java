package com.backbase.oss.boat.diff;

import com.backbase.oss.boat.serializer.SerializerUtils;
import io.swagger.parser.OpenAPIParser;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.parser.core.models.ParseOptions;
import io.swagger.v3.parser.core.models.SwaggerParseResult;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.experimental.UtilityClass;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.maven.artifact.versioning.DefaultArtifactVersion;
import org.openapitools.openapidiff.core.compare.OpenApiDiff;
import org.openapitools.openapidiff.core.model.ChangedOpenApi;
import org.openapitools.openapidiff.core.output.MarkdownRender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@UtilityClass
public class BatchOpenApiDiff {

    private static final Logger log = LoggerFactory.getLogger(BatchOpenApiDiff.class);
    private static final String X_CHANGELOG = "x-changelog";
    private static final MarkdownRender markdownRender = new MarkdownRender();

    @SuppressWarnings("squid:S2095")
    public static void diff(Path outputDirectory, Map<File, OpenAPI> success, Map<String, String> failed,
        boolean insertIntoSpec, boolean writeChangeLogToSeparateFile) throws IOException {

        List<Pair<Path, OpenAPI>> sortedSpecs = Files.walk(outputDirectory)
            .filter(Files::isRegularFile)
            .filter(BatchOpenApiDiff::isOpenApiSpec)
            .map(BatchOpenApiDiff::parseFileToParseResult)
            .filter(BatchOpenApiDiff::isValidOpenApiSpec)
            .map(BatchOpenApiDiff::mapToOpenAPIPair)
            .sorted(BatchOpenApiDiff::sortVersions)
            .collect(Collectors.toList());

        for (int i = 1; i < sortedSpecs.size(); i++) {
            Pair<Path,OpenAPI> oldVersionPair = sortedSpecs.get(i-1);
            Pair<Path,OpenAPI> newVersionPair = sortedSpecs.get(i);

            OpenAPI oldOpenAPI = oldVersionPair.getValue();
            OpenAPI newOpenAPI = newVersionPair.getValue();
            Path newOpenAPIPath = newVersionPair.getKey();

            try {
                ChangedOpenApi compare = OpenApiDiff.compare(oldOpenAPI, newOpenAPI);
                List<ChangedOpenApi> changeLog = getChangeLog(oldOpenAPI);
                changeLog.add(compare);
                String changelogMarkdown = renderChangeLog(changeLog);
                if (insertIntoSpec) {
                    writeChangelogInOpenAPI(newOpenAPIPath, changelogMarkdown);
                }
                if (writeChangeLogToSeparateFile) {
                    Path changeLogFile = newOpenAPIPath.getParent().resolve("changelog.md");
                    Files.write(changeLogFile, changelogMarkdown.getBytes(StandardCharsets.UTF_8), StandardOpenOption.CREATE);
                }
                newOpenAPI.addExtension(X_CHANGELOG, changeLog);
                success.put(newOpenAPIPath.toFile(), newOpenAPI);

            log.debug("Including Changelog for: {} with version: {}", newOpenAPI.getInfo().getTitle(), newOpenAPI.getInfo().getVersion());

            } catch (Exception e) {
                failed.put(newOpenAPI.toString(), e.getMessage());
            }
        }
    }

    private static int sortVersions(Pair<Path, OpenAPI> pair1, Pair<Path, OpenAPI> pair2) {
        return compareVersions(pair1.getRight().getInfo().getVersion(),
            pair2.getRight().getInfo().getVersion());
    }

    private static Pair<Path, OpenAPI> mapToOpenAPIPair(Pair<Path, SwaggerParseResult> pair) {
        return Pair.of(pair.getKey(), pair.getRight().getOpenAPI());
    }

    private static Pair<Path, SwaggerParseResult> parseFileToParseResult(Path path) {
        SwaggerParseResult right = parseOpenAPI(path);
        return Pair.of(path, right);
    }

    private static boolean isOpenApiSpec(Path path) {
        return path.toString().endsWith(".yaml");
    }

    private static boolean isValidOpenApiSpec(Pair<Path, SwaggerParseResult> pair) {
        return pair.getRight().getMessages().isEmpty();
    }

    private static void writeChangelogInOpenAPI(Path openApiFilePath, String changelogMarkdown) throws IOException {
        OpenAPI diffedApi = parseOpenAPI(openApiFilePath).getOpenAPI();
        if (diffedApi.getInfo().getDescription() == null) {
            diffedApi.getInfo().setDescription(changelogMarkdown);
        } else {
            diffedApi.getInfo().setDescription(diffedApi.getInfo().getDescription() + "\n" + changelogMarkdown);
        }

        diffedApi.addExtension("x-compared", true);
        log.debug("Writing change log back into OpenAPI");
        Files.write(openApiFilePath, SerializerUtils.toYamlString(diffedApi).getBytes(StandardCharsets.UTF_8), StandardOpenOption.CREATE);
    }

    private static String renderChangeLog(List<ChangedOpenApi> changeLog) {
        StringBuilder markDown = new StringBuilder();
        markDown.append("# Changelog\n");
        changeLog.forEach(diff -> {
            markDown.append("## ")
                .append(diff.getOldSpecOpenApi().getInfo().getVersion())
                .append(" - ")
                .append(diff.getNewSpecOpenApi().getInfo().getVersion())
                .append("\n");

            if (!diff.isDifferent()) {
                markDown.append("No Changes\n");
            } else {
                if (diff.isIncompatible()) {
                    markDown.append("**Note:** API has incompatible changes!!\n");
                }
                String changes = BatchOpenApiDiff.markdownRender.render(diff);
                markDown.append(changes);
            }
        });
        return markDown.toString();
    }

    @SuppressWarnings("unchecked")
    private static List<ChangedOpenApi> getChangeLog(OpenAPI oldSpec) {
        List<ChangedOpenApi> changeLog;
        if (oldSpec.getExtensions() != null && oldSpec.getExtensions().containsKey(X_CHANGELOG)) {
            changeLog = (List<ChangedOpenApi>) oldSpec.getExtensions().get(X_CHANGELOG);
        } else {
            changeLog = new ArrayList<>();

        }
        return changeLog;
    }

    private static SwaggerParseResult parseOpenAPI(Path file) {

        log.info("Parsing OpenAPI: {}", file);

        OpenAPIParser openAPIParser = new OpenAPIParser();
        ParseOptions parseOptions = new ParseOptions();
        parseOptions.setFlatten(true);
        parseOptions.setResolveFully(true);

        SwaggerParseResult swaggerParseResult = openAPIParser
            .readLocation(file.toFile().toURI().toString(), new ArrayList<>(), parseOptions);

        for (String message : swaggerParseResult.getMessages()) {
            log.warn(message);
        }

        return swaggerParseResult;
    }

    private static int compareVersions(String v1, String v2) {
        DefaultArtifactVersion version1 = new DefaultArtifactVersion(v1);
        DefaultArtifactVersion version2 = new DefaultArtifactVersion(v2);
        return version1.compareTo(version2);
    }
}
