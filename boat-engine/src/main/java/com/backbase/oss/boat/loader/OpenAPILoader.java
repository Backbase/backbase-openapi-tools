package com.backbase.oss.boat.loader;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.parser.OpenAPIV3Parser;
import io.swagger.v3.parser.core.models.ParseOptions;
import io.swagger.v3.parser.core.models.SwaggerParseResult;
import java.io.File;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class OpenAPILoader {

    private OpenAPILoader() {
        throw new AssertionError("Private constructor");
    }

    public static OpenAPI parse(String openApi) {
        OpenAPIV3Parser openAPIParser = new OpenAPIV3Parser();
        ParseOptions parseOptions = new ParseOptions();
        parseOptions.setFlattenComposedSchemas(true);
        parseOptions.setResolveCombinators(true);
        SwaggerParseResult swaggerParseResult = openAPIParser.readContents(openApi);
        return swaggerParseResult.getOpenAPI();

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

        log.debug("Reading OpenAPI from: {} resolveFully: {}", file, resolveFully);
        OpenAPIV3Parser openAPIParser = new OpenAPIV3Parser();
        ParseOptions parseOptions = new ParseOptions();
        parseOptions.setFlatten(flatten);
        parseOptions.setResolve(resolveFully);
        parseOptions.setResolveFully(resolveFully);
        parseOptions.setFlattenComposedSchemas(true);
        parseOptions.setResolveCombinators(true);

        SwaggerParseResult swaggerParseResult = openAPIParser.readLocation(file.toURI().toString(), null, parseOptions);
        if (swaggerParseResult.getOpenAPI() == null) {
            log.error("Could not load OpenAPI from file: {} \n{}", file, String.join("\t\n", swaggerParseResult.getMessages()));
            throw new OpenAPILoaderException("Could not load open api from file :" + file);
        }
        return swaggerParseResult.getOpenAPI();

    }

}
