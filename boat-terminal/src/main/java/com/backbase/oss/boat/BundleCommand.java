package com.backbase.oss.boat;

import com.backbase.oss.boat.transformers.Bundler;

import java.nio.file.Path;
import java.util.Collections;
import java.util.concurrent.Callable;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.parser.OpenAPIV3Parser;
import io.swagger.v3.parser.core.models.ParseOptions;
import io.swagger.v3.parser.core.models.SwaggerParseResult;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

@Command(name = BundleCommand.NAME,
    description = "Bundles all references in the OpenAPI specification into one file.",
    mixinStandardHelpOptions = true)
public class BundleCommand implements Callable<Integer> {
    static public final String NAME = "bundle";

    @Parameters(description = "Input OpenAPI yaml file.")
    private Path input;

    @Option(names = {"-o", "--output"}, order = 20,
        description = "Bundled OpenAPI file name.")
    private Path output;

    @Option(names = {"--flatten"},
        defaultValue = "true", fallbackValue = "true",
        paramLabel = "true|false", arity = "0..1")
    private boolean flatten;

    @Option(names = {"--resolve"},
        defaultValue = "true", fallbackValue = "true",
        paramLabel = "true|false", arity = "0..1")
    private boolean resolve;

    @Override
    public Integer call() throws Exception {
        final OpenAPIV3Parser parser = new OpenAPIV3Parser();
        final ParseOptions options = new ParseOptions();

        options.setFlatten(this.flatten);
        options.setResolve(this.resolve);
        options.setResolveFully(this.resolve);
        options.setFlattenComposedSchemas(true);
        options.setResolveCombinators(true);

        final SwaggerParseResult result = parser.readLocation(this.input.toString(), null, options);
        final OpenAPI openAPI = result.getOpenAPI();

        new Bundler(this.input.toFile()).transform(openAPI, Collections.emptyMap());

        return 0;
    }
}
