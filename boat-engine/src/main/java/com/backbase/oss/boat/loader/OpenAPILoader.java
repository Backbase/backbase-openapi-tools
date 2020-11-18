package com.backbase.oss.boat.loader;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.parser.OpenAPIV3Parser;
import io.swagger.v3.parser.core.models.ParseOptions;
import io.swagger.v3.parser.core.models.SwaggerParseResult;
import java.io.File;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class OpenAPILoader {

    public static OpenAPI load(File file) throws OpenAPILoaderException {
        return load(file, false, false);
    }

    public static OpenAPI load(File file, boolean resolveFully) throws OpenAPILoaderException {
        return load(file, resolveFully, false);
    }

    public static OpenAPI load(File file, boolean resolveFully, boolean flatten) throws OpenAPILoaderException {
        log.debug("Reading OpenAPI from: {} resolveFully: {}", file, resolveFully);
        OpenAPIV3Parser openAPIParser = new OpenAPIV3Parser();
        ParseOptions parseOptions = new ParseOptions();
        parseOptions.setFlatten(flatten);
        parseOptions.setResolve(resolveFully);
        parseOptions.setResolveFully(resolveFully);
        parseOptions.setFlattenComposedSchemas(true);
        parseOptions.setResolveCombinators(true);
        SwaggerParseResult swaggerParseResult = openAPIParser.readLocation(file.toURI().toString(), null, parseOptions);


        return swaggerParseResult.getOpenAPI();

    }

}
