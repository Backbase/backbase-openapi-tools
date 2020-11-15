package com.backbase.oss.boat;

import com.backbase.oss.boat.transformers.RefInline;
import java.nio.file.Path;
import java.util.concurrent.Callable;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.parser.OpenAPIV3Parser;
import io.swagger.v3.parser.core.models.ParseOptions;
import io.swagger.v3.parser.core.models.SwaggerParseResult;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

@Command(name = "ref-inline",
    description = "Transforms all inlined schemas to references.")
public class RefInlineCommand implements Callable<Integer> {

    @Parameters(description = "Input OpenAPI yaml file.", index = "0")
    private Path input;

    @Parameters(description = "Transformed OpenAPI yaml file.", index = "1")
    private Path output;

    @Option(names = {"-s", "--schemas"}, order = 20, defaultValue = "",
        description = "Path to schema files, relative to output's directory.")
    private Path schemas;

    @Option(names = {"--flatten"},
        defaultValue = "true", fallbackValue = "true",
        paramLabel = "true|false", arity = "0..1")
    private boolean flatten;

    @Option(names = {"--resolve"},
        defaultValue = "false", fallbackValue = "false",
        paramLabel = "true|false", arity = "0..1")
    private boolean resolve;

    @Option(names = {"--clean"},
        defaultValue = "false", fallbackValue = "false",
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
        final RefInline trn = new RefInline(this.output, this.schemas);

        trn.transform(openAPI);
        trn.export(this.clean);

        return 0;
    }
}
