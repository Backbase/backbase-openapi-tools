package com.backbase.oss.boat;

import com.backbase.oss.boat.loader.OpenAPILoader;
import com.backbase.oss.boat.loader.OpenAPILoaderException;
import com.backbase.oss.boat.serializer.SerializerUtils;
import com.backbase.oss.boat.transformers.Deprecator;
import io.swagger.v3.oas.models.OpenAPI;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import lombok.extern.slf4j.Slf4j;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;

/**
 * Removes deprecated elements in an OpenAPI spec.
 */
@Mojo(name = "remove-deprecated", requiresDependencyResolution = ResolutionScope.RUNTIME, threadSafe = true)
@Slf4j
public class RemoveDeprecatedMojo extends AbstractMojo {

    @Parameter(name = "input", required = true)
    private File input;

    @Parameter(name = "output", required = true)
    private File output;

    public void setInput(File input) {
        this.input = input;
    }

    public void setOutput(File output) {
        this.output = output;
    }

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        log.debug("Removing deprecated objects in OpenAPI: {} to: {}", input, output);

        OpenAPI openAPI = null;
        try {
            openAPI = OpenAPILoader.load(input);
            new Deprecator().transform(openAPI, null);
            Files.write(output.toPath(), SerializerUtils.toYamlString(openAPI).getBytes(StandardCharsets.UTF_8), StandardOpenOption.CREATE);
        } catch (OpenAPILoaderException | IOException e) {
            throw new MojoExecutionException("Error transforming OpenAPI: {}" + input, e);
        }

    }
}
