package com.backbase.boat.loader;

import io.swagger.parser.OpenAPIParser;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.parser.core.models.ParseOptions;
import io.swagger.v3.parser.core.models.SwaggerParseResult;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;

public class OpenAPILoader {

    public static OpenAPI load(File file) throws OpenAPILoaderException {
        byte[] bytes = new byte[0];
        try {
            bytes = Files.readAllBytes(file.toPath());
        } catch (IOException e) {
            throw new OpenAPILoaderException("Cannot read yaml from: " + file.getAbsolutePath(), e);
        }
        return load(new String(bytes));
    }

    public static OpenAPI load(String api) {
        OpenAPIParser openAPIParser = new OpenAPIParser();
        ParseOptions parseOptions = new ParseOptions();
        parseOptions.setFlatten(true);
        SwaggerParseResult swaggerParseResult = openAPIParser.readContents(api, new ArrayList<>(), parseOptions);

        OpenAPI openAPI = swaggerParseResult.getOpenAPI();

        return openAPI;
    }
}
