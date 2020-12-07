package com.backbase.oss.boat;

import com.backbase.oss.boat.loader.OpenAPILoader;
import com.backbase.oss.boat.loader.OpenAPILoaderException;
import com.backbase.oss.boat.serializer.SerializerUtils;
import com.backbase.oss.boat.transformers.Decomposer;
import io.swagger.v3.oas.models.OpenAPI;
import java.io.File;
import java.io.IOException;
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
 * Merges any components using allOf references.
 */
@Mojo(name = "decompose", requiresDependencyResolution = ResolutionScope.RUNTIME, threadSafe = true)
@Slf4j
public class DecomposeMojo extends AbstractMojo {

    @Parameter(name = "input", required = true)
    private File input;

    @Parameter(name = "output", required = true)
    private File output;

    public void setOutput(File output) {
        this.output = output;
    }

    public void setInput(File input) {
        this.input = input;
    }

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        log.info("Decomposing OpenAPI: {} to: {}", input, output);

        OpenAPI openAPI = null;
        try {
            openAPI = OpenAPILoader.load(input);
            new Decomposer().transform(openAPI, null);


            File directory = output.getParentFile();
            if (!directory.exists()) {
                directory.mkdirs();
            }
            Files.write(output.toPath(), SerializerUtils.toYamlString(openAPI).getBytes(), StandardOpenOption.CREATE);
        } catch (OpenAPILoaderException | IOException e) {
            throw new MojoExecutionException("Error transforming OpenAPI: {}" + input, e);
        }

    }
}
