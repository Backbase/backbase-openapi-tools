package com.backbase.oss.boat;

import static java.util.Collections.singletonMap;

import com.backbase.oss.boat.transformers.ExplodeTransformer;

import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.regex.Pattern;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.parser.OpenAPIV3Parser;
import io.swagger.v3.parser.core.models.ParseOptions;
import io.swagger.v3.parser.core.models.SwaggerParseResult;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

@Command(name = "explode",
    description = "Moves all inlined schemas to concrete types.")
public class ExplodeCommand implements Callable<Integer> {

    @Parameters(description = "Input OpenAPI yaml file.", index = "0", converter = ExistingPath.class)
    private Path input;

    @Parameters(description = "Transformed OpenAPI yaml file.", index = "1")
    private Path output;

    @Option(names = {"-s", "--schemas"}, order = 20, defaultValue = "",
        description = "Path to schema files, relative to output's directory.")
    private Path schemas;

    @Option(names = {"-r", "--rename"}, description = "Pattern used to rename the generated files.")
    private final Map<Pattern, String> rename = new LinkedHashMap<>();

    @Option(names = {"--flatten"},
        defaultValue = "true", fallbackValue = "true",
        paramLabel = "true|false", arity = "0..1")
    private boolean flatten;

    @Option(names = {"--resolve"},
        defaultValue = "false", fallbackValue = "true",
        paramLabel = "true|false", arity = "0..1")
    private boolean resolve;

    @Option(names = {"--clean"},
        defaultValue = "false", fallbackValue = "true",
        paramLabel = "true|false", arity = "0..1")
    private boolean clean;

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
        final ExplodeTransformer trn = new ExplodeTransformer(this.output, this.schemas);

        trn.transform(openAPI, singletonMap("rename", this.rename));
        trn.export(this.clean);

        return 0;
    }
}
