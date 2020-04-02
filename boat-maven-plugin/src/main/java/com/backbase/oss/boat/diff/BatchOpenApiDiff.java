package com.backbase.oss.boat.diff;

import com.backbase.oss.boat.serializer.SerializerUtils;
import com.qdesrame.openapi.diff.compare.OpenApiDiff;
import com.qdesrame.openapi.diff.model.ChangedOpenApi;
import io.swagger.parser.OpenAPIParser;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.parser.core.models.ParseOptions;
import io.swagger.v3.parser.core.models.SwaggerParseResult;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.experimental.UtilityClass;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.maven.artifact.versioning.DefaultArtifactVersion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@UtilityClass
public class BatchOpenApiDiff {

    private static final Logger log = LoggerFactory.getLogger(BatchOpenApiDiff.class);
    private static final String X_CHANGELOG = "x-changelog";
    private static MarkdownRender markdownRender = new MarkdownRender();

    @SuppressWarnings("squid:S2095")
    public static void diff(Path outputDirectory, Map<File, OpenAPI> success, Map<String, String> failed,
        boolean insertIntoSpec, boolean writeChangeLogToSeparateFile) throws IOException {
        Map<Path, OpenAPI> specs = new LinkedHashMap<>();

        Files.walk(outputDirectory)
            .filter(Files::isRegularFile)
            .filter(path -> path.toString().endsWith("openapi.yaml"))
            .map(path -> {
                SwaggerParseResult right = parseOpenAPI(path);
                return Pair.of(path, right);
            })
            .filter(pair -> pair.getRight().getMessages().isEmpty())
            .map(pair -> Pair.of(pair.getKey(), pair.getRight().getOpenAPI()))
            .sorted((pair1, pair2) -> compareVersions(pair1.getRight().getInfo().getVersion(),
                pair2.getRight().getInfo().getVersion()))
            .forEach(tuple -> specs.put(tuple.getKey(), tuple.getValue()));

        for (Map.Entry<Path, OpenAPI> entry : specs.entrySet()) {
            Path openApiFilePath = entry.getKey();
            OpenAPI openAPI = entry.getValue();
            String version = openAPI.getInfo().getVersion();
            log.debug("Including Changelog for: {} with version: {}", openAPI.getInfo().getTitle(), version);

            previousVersion(openAPI, openApiFilePath, specs).ifPresent(oldSpec -> {
                log.debug("Comparing versions: {} - {} ", version, oldSpec.getInfo().getVersion());
                ChangedOpenApi compare;
                try {
                    compare = OpenApiDiff.compare(oldSpec, openAPI);
                    List<ChangedOpenApi> changeLog = getChangeLog(oldSpec);
                    changeLog.add(compare);
                    String changelogMarkdown = renderChangeLog(markdownRender, changeLog);
                    if (insertIntoSpec) {
                        writeChangelogInOpenAPI(openApiFilePath, changelogMarkdown);
                    }
                    if (writeChangeLogToSeparateFile) {
                        Path changeLogFile = openApiFilePath.getParent().resolve("changelog.md");
                        Files.write(changeLogFile, changelogMarkdown.getBytes(), StandardOpenOption.CREATE);
                    }
                    openAPI.addExtension(X_CHANGELOG, changeLog);
                    success.put(openApiFilePath.toFile(), openAPI);
                } catch (Exception e) {
                    failed.put(openApiFilePath.toString(), e.getMessage());
                }
            });
        }
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
        Files.write(openApiFilePath, SerializerUtils.toYamlString(diffedApi).getBytes(), StandardOpenOption.CREATE);
    }

    private static String renderChangeLog(MarkdownRender renderer, List<ChangedOpenApi> changeLog) {
        StringBuilder markDown = new StringBuilder();
        markDown.append("# Changelog\n");
        changeLog.forEach(diff -> {
            markDown.append("## ")
                .append(diff.getOldSpecOpenApi().getInfo().getVersion())
                .append(" - ")
                .append(diff.getNewSpecOpenApi().getInfo().getVersion())
                .append("\n");

            if (diff.isUnchanged()) {
                markDown.append("No Changes\n");
            } else {
                if (diff.isIncompatible()) {
                    markDown.append("**Note:** API has incompatible changes!!\n");
                }
                String changes = renderer.render(diff);
                markDown.append(changes);
            }
        });
        return markDown.toString();
    }

    private static List<ChangedOpenApi> getChangeLog(OpenAPI oldSpec) {
        List<ChangedOpenApi> changeLog;
        if (oldSpec.getExtensions() != null && oldSpec.getExtensions().containsKey(X_CHANGELOG)) {
            changeLog = (List<ChangedOpenApi>) oldSpec.getExtensions().get(X_CHANGELOG);
        } else {
            changeLog = new ArrayList<>();

        }
        return changeLog;
    }

    @SuppressWarnings({"java:S2629","java:S1075", "java:S2095"})
    private static Optional<OpenAPI> previousVersion(OpenAPI openApi, Path openApiPath, Map<Path, OpenAPI> specs) throws IOException {
        List<Path> lowerVersions = Files.list(openApiPath.getParent().getParent())
            .filter(otherVersionPath -> {
                String otherVersion = otherVersionPath.getFileName().toString();
                int compared = compareVersions(otherVersion, openApi.getInfo().getVersion());
                return compared < 0;
            })
            .sorted().collect(Collectors.toList());

        if (lowerVersions.isEmpty()) {
            log.debug("No previous versions found for: {}", openApi.getInfo().getTitle());
            return Optional.empty();
        } else {
            log.debug("{} previous versions: {}", openApi.getInfo().getTitle(), lowerVersions.stream().map(path -> path.getFileName().toString()).collect(Collectors.joining(", ")));
            File previousVersion = new File(lowerVersions.get(lowerVersions.size() - 1).toFile(), "/openapi.yaml");
            OpenAPI oldSpec = specs.get(previousVersion.toPath());
            return Optional.of(oldSpec);
        }
    }

    private static SwaggerParseResult parseOpenAPI(Path file) {

        log.debug("Parsing OpenAPI: {}", file);

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
