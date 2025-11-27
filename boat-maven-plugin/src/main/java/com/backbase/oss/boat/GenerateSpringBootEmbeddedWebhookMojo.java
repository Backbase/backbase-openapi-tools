package com.backbase.oss.boat;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;

import java.util.Collection;
import java.util.Set;

/**
 * Generating Server Stubs using Spring Boot.
 */
@Mojo(name = "generate-spring-boot-embedded-webhooks", threadSafe = true)
public class GenerateSpringBootEmbeddedWebhookMojo extends AbstractGenerateMojo {

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        getLog().info("Generating Server Stubs using Spring Boot Webhooks");
        execute("boat-webhooks", "boat-webhooks", true, false, false);
        //execute("spring", "webhooks", true, false, false);
    }

    @Override
    protected Collection<String> getGeneratorSpecificSupportingFiles() {
        return Set.of("BigDecimalCustomSerializer.java", "WebhookResponse.java", "ServletContent.java", "PosthookRequest.java", "PrehookRequest.java");
    }
}
