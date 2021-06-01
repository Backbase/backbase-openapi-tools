package com.backbase.oss.boat.loader;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.parser.OpenAPIV3Parser;
import io.swagger.v3.parser.core.models.AuthorizationValue;
import io.swagger.v3.parser.core.models.ParseOptions;
import io.swagger.v3.parser.core.models.SwaggerParseResult;
import java.io.File;
import java.util.List;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class OpenAPILoader {

    private OpenAPILoader() {
        throw new AssertionError("Private constructor");
    }

    public static OpenAPI parse(String openApi) throws OpenAPILoaderException {
        OpenAPIV3Parser openAPIParser = new OpenAPIV3Parser();
        ParseOptions parseOptions = new ParseOptions();
        parseOptions.setFlattenComposedSchemas(true);
        parseOptions.setResolveCombinators(true);

        SwaggerParseResult swaggerParseResult = null;
        try {
            swaggerParseResult = openAPIParser.readContents(openApi);
        } catch (Exception e) {
            throw new OpenAPILoaderException("Cannot parse openAPI", e);
        }

        if (swaggerParseResult.getOpenAPI() == null) {
            throw new OpenAPILoaderException("Cannot parse OpenAPI", swaggerParseResult.getMessages());
        }

        return swaggerParseResult.getOpenAPI();

    }

    public static OpenAPI load(String url) throws OpenAPILoaderException {
        return load(url, false, false);
    }

    public static OpenAPI load(File file) throws OpenAPILoaderException {
        return load(file, false, false);
    }

    public static OpenAPI load(File file, boolean resolveFully) throws OpenAPILoaderException {
        return load(file, resolveFully, false);
    }

    public static OpenAPI load(File file, boolean resolveFully, boolean flatten) throws OpenAPILoaderException {
        if (!file.exists()) {
            throw new OpenAPILoaderException("Could not load open api from file :" + file.getAbsolutePath() + ". File does not exist!");
        }
        return load(file.toURI().toString(), resolveFully, flatten);
    }

    public static OpenAPI load(String url, boolean resolveFully, boolean flatten) throws OpenAPILoaderException {
        return load(url, resolveFully, flatten, null);
    }

    public static OpenAPI load(String url, boolean resolveFully, boolean flatten, List<AuthorizationValue> auth) throws OpenAPILoaderException {
        log.debug("Reading OpenAPI from: {} resolveFully: {}", url, resolveFully);
        OpenAPIV3Parser openAPIParser = new OpenAPIV3Parser();
        ParseOptions parseOptions = new ParseOptions();
        parseOptions.setFlatten(flatten);
        parseOptions.setResolve(resolveFully);
        parseOptions.setResolveFully(resolveFully);
        parseOptions.setFlattenComposedSchemas(true);
        parseOptions.setResolveCombinators(true);

        SwaggerParseResult swaggerParseResult = openAPIParser.readLocation(url, auth, parseOptions);
        if (swaggerParseResult.getOpenAPI() == null) {
            log.error("Could not load OpenAPI from : {} \n{}", url, String.join("\t\n", swaggerParseResult.getMessages()));
            throw new OpenAPILoaderException("Could not load open api from :" + url, swaggerParseResult.getMessages());
        }
        return swaggerParseResult.getOpenAPI();
    }
}
