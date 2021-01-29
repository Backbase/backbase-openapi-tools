package com.backbase.oss.boat;

import com.backbase.oss.boat.serializer.SerializerUtils;
import io.swagger.parser.OpenAPIParser;
import io.swagger.v3.parser.core.models.ParseOptions;
import io.swagger.v3.parser.core.models.SwaggerParseResult;
import java.io.File;
import java.util.ArrayList;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;

/**
 * Validates OpenAPI specs.
 */
@Mojo(name = "validate", requiresDependencyResolution = ResolutionScope.RUNTIME, threadSafe = true)
@Slf4j
public class ValidateMojo extends AbstractMojo {

    @Parameter(name = "input", required = true)
    private File input;

    @Parameter(name = "failOnWarning", required = true)
    private boolean failOnWarning;

    @Parameter(readonly = true, required = true, defaultValue = "${project}")
    protected MavenProject project;
/// cant get it to be sent in source pom file ???
    @Parameter(name = "sourceKey", defaultValue = "", property = "boat.maven.plugin.sourceKey")
    private String sourceKey;

    @Parameter(name = "boatBayUrl", defaultValue = "", property = "BOAT_BAY_SERVER_URL")
    private String boatBayUrl;


    public void setInput(File input) {
        this.input = input;
    }
    public void setSourceId(String sourceId){this.sourceKey = sourceId;}
    public void setBoatBayUrl(String boatBayUrl){this.boatBayUrl = boatBayUrl;}
    public void setFailOnWarning(boolean failOnWarning) {
        this.failOnWarning= failOnWarning;
    }




    @SneakyThrows
    @Override
    public void execute() throws  MojoFailureException {

        if (!input.exists()) {
            throw new MojoFailureException("File not found: " + input.getName());
        }

        if (input.isDirectory()) {
            log.info("Validating files '*.yaml' in: {}", input);
            for (File inputFile: input.listFiles(pathname -> StringUtils.endsWith(pathname.getName(), ".yaml"))) {
                validate(inputFile);
            }
        } else {
            validate(input);
            if (!sourceKey.isEmpty()
                    || !System.getenv("BOAT_BAY_SERVER_URL").isEmpty()
                    || System.getenv("BOAT_BAY_SERVER_URL") != null
                    || !boatBayUrl.isEmpty()){
                new BoatBayRadio(input,null,project, boatBayUrl).upload(sourceKey);
            }
        }
    }

    private void validate(File inputFile) throws MojoFailureException {

        log.info("Validating {}", inputFile);

        OpenAPIParser openAPIParser = new OpenAPIParser();
        ParseOptions parseOptions = new ParseOptions();
        parseOptions.setFlatten(true);
        parseOptions.setResolveFully(true);

        SwaggerParseResult swaggerParseResult =
            openAPIParser.readLocation(inputFile.toURI().toString(), new ArrayList<>(), parseOptions);

        if (swaggerParseResult.getMessages().isEmpty()) {
            log.info("OpenAPI: {} is valid", swaggerParseResult.getOpenAPI().getInfo().getTitle());
        } else {
            for (String message : swaggerParseResult.getMessages()) {
                processMessages(message, inputFile, swaggerParseResult);
            }
            if (failOnWarning) {
                throw new MojoFailureException("Validation errors validating OpenAPI");
            }
        }
    }

    private void processMessages(String message,File inputFile, SwaggerParseResult swaggerParseResult){
        if (failOnWarning) {
            log.error("Validation errors while parsing OpenAPI: {}", inputFile.getName());
            log.error(message);
        } else {
            log.warn("Validation errors while parsing OpenAPI: {}", inputFile.getName());
            log.warn(message);
        }
        if (log.isDebugEnabled()) {
            try {
                log.debug("Dumping open api");
                log.debug(SerializerUtils.toYamlString(swaggerParseResult.getOpenAPI()));
            } catch (RuntimeException e) {
                log.debug("That did not end well: ", e);
            }
        }
    }
}
