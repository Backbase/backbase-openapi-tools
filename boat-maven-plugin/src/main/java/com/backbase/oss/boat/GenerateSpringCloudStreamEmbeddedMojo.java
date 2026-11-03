package com.backbase.oss.boat;

import com.backbase.oss.codegen.asyncapi.BoatSpringCloudStreamCodegen;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.openapitools.codegen.languages.SpringCodegen;

/**
 * Generating Spring Cloud Stream producers and consumers from an AsyncAPI 3.0 document.
 */
@Mojo(name = "generate-spring-cloud-stream-embedded", threadSafe = true)
public class GenerateSpringCloudStreamEmbeddedMojo extends AbstractGenerateMojo {

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        getLog().info("Generating Spring Cloud Stream producers and consumers");
        Map<Object, Object> options = new HashMap<>();
        if (this.configOptions != null) {
            options.putAll(this.configOptions);
        }
        options.putIfAbsent(SpringCodegen.INTERFACE_ONLY, "false");
        this.configOptions = options;
        execute(BoatSpringCloudStreamCodegen.NAME, BoatSpringCloudStreamCodegen.LIBRARY, true, false, false);
    }

    @Override
    protected Collection<String> getGeneratorSpecificSupportingFiles() {
        return Set.of("BigDecimalCustomSerializer.java", "AsyncApiHeaders.java", "asyncapi-bindings.properties");
    }
}
