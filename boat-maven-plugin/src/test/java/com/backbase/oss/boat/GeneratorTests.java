package com.backbase.oss.boat;

import com.backbase.oss.boat.loader.OpenAPILoader;
import com.backbase.oss.boat.loader.OpenAPILoaderException;
import io.swagger.v3.oas.models.OpenAPI;
import java.io.File;
import java.util.HashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.logging.console.ConsoleLogger;
import org.junit.Ignore;
import org.junit.Test;
import org.sonatype.plexus.build.incremental.DefaultBuildContext;

@Slf4j
public class GeneratorTests {

    @Test
    @Ignore
    public void testBundleSpec() throws MojoExecutionException {
        GenerateMojo mojo = new GenerateMojo();

        String inputFile = getClass().getResource("/oas-examples/petstore.yaml").getFile();
        File input = new File(inputFile);
        File output = new File("target");
        if (!output.exists()) {
            output.mkdirs();
        }

        DefaultBuildContext defaultBuildContext = new DefaultBuildContext();
        defaultBuildContext.enableLogging(new ConsoleLogger());

        mojo.getLog();
        mojo.buildContext = defaultBuildContext;
        mojo.project = new MavenProject();
        mojo.generatorName = "html2";
        mojo.inputSpec = input.getAbsolutePath();
        mojo.output = output;
        mojo.skip = false;
        mojo.skipIfSpecIsUnchanged = false;
        mojo.execute();

    }

    @Test
    public void testHTML2() throws MojoExecutionException, OpenAPILoaderException {
        GenerateMojo mojo = new GenerateMojo();

        String inputFile = getClass().getResource("/backbase/arrangement-inbound-api/openapi.yaml").getFile();
        File input = new File(inputFile);
        File output = new File("target");
        if (!output.exists()) {
            output.mkdirs();
        }

        DefaultBuildContext defaultBuildContext = new DefaultBuildContext();
        defaultBuildContext.enableLogging(new ConsoleLogger());

        OpenAPI openApi = OpenAPILoader.load(input);

        mojo.getLog();
        mojo.buildContext = defaultBuildContext;
        mojo.project = new MavenProject();
        mojo.generatorName = "html2";
        mojo.inputSpec = input.getAbsolutePath();
        mojo.output = output;
        mojo.skip = false;
        mojo.skipIfSpecIsUnchanged = false;
        mojo.execute();

    }

    /**
     * <configuration>
     *               <output>${project.build.directory}/generated-sources/</output>
     *               <generateSupportingFiles>true</generateSupportingFiles>
     *               <generatorName>spring</generatorName>
     *               <strictSpec>true</strictSpec>
     *               <generateApiTests>false</generateApiTests>
     *               <generateModelTests>false</generateModelTests>
     *               <inputSpec>${project.build.directory}/yaml/legalentity-presentation-service-spec.yaml</inputSpec>
     *               <configOptions>
     *                 <library>spring-mvc</library>
     *                 <dateLibrary>legacy</dateLibrary>
     *                 <interfaceOnly>true</interfaceOnly>
     *                 <skipDefaultInterface>true</skipDefaultInterface>
     *                 <useBeanValidation>false</useBeanValidation>
     *                 <useClassLevelBeanValidation>true</useClassLevelBeanValidation>
     *                 <useTags>true</useTags>
     *                 <java8>true</java8>
     *                 <useOptional>false</useOptional>
     *                 <apiPackage>com.backbase.accesscontrol.service.rest.spec.api</apiPackage>
     *                 <>com.backbase.accesscontrol.service.rest.spec.model</modelPackage>
     *               </configOptions>
     *             </configuration>
     * @throws MojoExecutionException
     */
    @Test
    public void testBeanValidation() throws MojoExecutionException {
        GenerateMojo mojo = new GenerateMojo();

        String inputFile = getClass().getResource("/oas-examples/petstore.yaml").getFile();
        File input = new File(inputFile);
        File output = new File("target");
        if (!output.exists()) {
            output.mkdirs();
        }

        DefaultBuildContext defaultBuildContext = new DefaultBuildContext();
        defaultBuildContext.enableLogging(new ConsoleLogger());

        Map<String,String> configOption = new HashMap<>();
        configOption.put("library", "spring-mvc");
        configOption.put("dateLibrary", "java8");
        configOption.put("apiPackage", "com.backbase.accesscontrol.service.rest.spec.api");
        configOption.put("modelPackage", "com.backbase.accesscontrol.service.rest.spec.model");
        configOption.put("useBeanValidation", "true");
        configOption.put("useClassLevelBeanValidation", "false");


        mojo.getLog();
        mojo.buildContext = defaultBuildContext;
        mojo.project = new MavenProject();
        mojo.generatorName = "spring";
        mojo.configOptions = configOption;
        mojo.inputSpec = input.getAbsolutePath();
        mojo.output = new File("target/spring-mvc");
        mojo.skip = false;
        mojo.skipIfSpecIsUnchanged = false;
        mojo.execute();

    }

    @Test
    public void testWebClient() throws MojoExecutionException {
        GenerateWebClientEmbeddedMojo mojo = new GenerateWebClientEmbeddedMojo();

        String inputFile = getClass().getResource("/oas-examples/petstore.yaml").getFile();
        File input = new File(inputFile);
        File output = new File("target/webclient");
        if (!output.exists()) {
            output.mkdirs();
        }

        DefaultBuildContext defaultBuildContext = new DefaultBuildContext();
        defaultBuildContext.enableLogging(new ConsoleLogger());

        mojo.buildContext = defaultBuildContext;
        mojo.project = new MavenProject();
        mojo.inputSpec = input.getAbsolutePath();
        mojo.output = output;
        mojo.skip = false;
        mojo.skipIfSpecIsUnchanged = false;
        mojo.execute();

    }
}
