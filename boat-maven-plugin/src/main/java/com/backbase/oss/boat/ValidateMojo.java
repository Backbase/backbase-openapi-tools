package com.backbase.oss.boat;

import io.swagger.parser.OpenAPIParser;
import io.swagger.v3.parser.core.models.ParseOptions;
import io.swagger.v3.parser.core.models.SwaggerParseResult;
import java.io.File;
import java.util.ArrayList;
import lombok.extern.slf4j.Slf4j;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;

@Mojo(name = "validate", requiresDependencyResolution = ResolutionScope.RUNTIME, threadSafe = true)
@Slf4j
public class ValidateMojo extends AbstractMojo {

    @Parameter(name = "input", required = true)
    private File input;

    @Parameter(name = "failOnWarning", required = true)
    private boolean failOnWarning;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        log.info("Validating OpenAPI: {}", input);

        OpenAPIParser openAPIParser = new OpenAPIParser();
        ParseOptions parseOptions = new ParseOptions();
        parseOptions.setFlatten(true);
        parseOptions.setResolveFully(true);

        SwaggerParseResult swaggerParseResult = openAPIParser.readLocation(input.toURI().toString(), new ArrayList<>(), parseOptions);

        if (swaggerParseResult.getMessages().isEmpty()) {
            log.info("OpenAPI: {} is valid", swaggerParseResult.getOpenAPI().getInfo().getTitle());
        } else {
            log.error("Validation errors while parsing OpenAPI: {}", openAPIParser);
            for (String message : swaggerParseResult.getMessages()) {
                if (failOnWarning) {
                    log.error(message);
                } else {
                    log.warn(message);
                }
            }
            if (failOnWarning) {
                throw new MojoFailureException("Validation errors validating OpenAPI");
            }
        }

    }
}
