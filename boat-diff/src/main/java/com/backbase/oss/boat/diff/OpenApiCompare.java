package com.backbase.oss.boat.diff;

import com.backbase.oss.boat.diff.compare.OpenApiDiff;
import com.backbase.oss.boat.diff.model.ChangedOpenApi;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.parser.OpenAPIV3Parser;
import io.swagger.v3.parser.core.models.AuthorizationValue;
import io.swagger.v3.parser.core.models.ParseOptions;
import java.io.File;
import java.util.List;

public class OpenApiCompare {

    private static OpenAPIV3Parser openApiParser = new OpenAPIV3Parser();
    private static ParseOptions options = new ParseOptions();

    static {
        options.setResolve(true);
    }

    public static ChangedOpenApi fromContents(String oldContent, String newContent) {
        return fromContents(oldContent, newContent, null);
    }

    public static ChangedOpenApi fromContents(
            String oldContent, String newContent, List<AuthorizationValue> auths) {
        return fromSpecifications(readContent(oldContent, auths), readContent(newContent, auths));
    }

    public static ChangedOpenApi fromFiles(File oldFile, File newFile) {
        return fromFiles(oldFile, newFile, null);
    }

    public static ChangedOpenApi fromFiles(
            File oldFile, File newFile, List<AuthorizationValue> auths) {
        return fromLocations(oldFile.getAbsolutePath(), newFile.getAbsolutePath(), auths);
    }

    public static ChangedOpenApi fromLocations(String oldLocation, String newLocation) {
        return fromLocations(oldLocation, newLocation, null);
    }

    public static ChangedOpenApi fromLocations(
            String oldLocation, String newLocation, List<AuthorizationValue> auths) {
        return fromSpecifications(readLocation(oldLocation, auths), readLocation(newLocation, auths));
    }

    public static ChangedOpenApi fromSpecifications(OpenAPI oldSpec, OpenAPI newSpec) {
        return OpenApiDiff.compare(notNull(oldSpec, "old"), notNull(newSpec, "new"));
    }

    private static OpenAPI notNull(OpenAPI spec, String type) {
        if (spec == null) {
            throw new RuntimeException(String.format("Malformed file: cant read %s file", type));
        }
        return spec;
    }

    private static OpenAPI readContent(String content, List<AuthorizationValue> auths) {
        return openApiParser.readContents(content, auths, options).getOpenAPI();
    }

    private static OpenAPI readLocation(String location, List<AuthorizationValue> auths) {
        return openApiParser.read(location, auths, options);
    }
}
