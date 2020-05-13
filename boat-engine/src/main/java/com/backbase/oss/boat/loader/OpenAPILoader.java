package com.backbase.oss.boat.loader;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.parser.OpenAPIV3Parser;
import io.swagger.v3.parser.core.models.ParseOptions;
import io.swagger.v3.parser.core.models.SwaggerParseResult;
import java.io.File;

public class OpenAPILoader {

    public static OpenAPI load(File file) throws OpenAPILoaderException {
        return load(file, false);
    }

    public static OpenAPI load(File file, boolean resolveFully) throws OpenAPILoaderException {
        OpenAPIV3Parser openAPIParser = new OpenAPIV3Parser();
        ParseOptions parseOptions = new ParseOptions();
//        parseOptions.setFlatten(false);
//        parseOptions.setResolve(resolveFully);
//        parseOptions.setResolveCombinators(false);
////        parseOptions.setResolveFully(resolveFully);
//        parseOptions.setSkipMatches(true);
        SwaggerParseResult swaggerParseResult = openAPIParser.readLocation(file.toURI().toString(), null, parseOptions);


        return swaggerParseResult.getOpenAPI();

    }

}
