package com.backbase.oss.boat;

import static java.util.Collections.singletonMap;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.backbase.oss.codegen.java.BoatJavaCodeGen;
import com.backbase.oss.codegen.java.BoatSpringCodeGen;
import java.io.File;
import java.util.HashMap;
import java.util.Map;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.logging.console.ConsoleLogger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openapitools.codegen.DefaultCodegen;
import org.sonatype.plexus.build.incremental.DefaultBuildContext;

class GenerateMojoTests {
    private final DefaultBuildContext buildContext = new DefaultBuildContext();
    private final MavenProject project = new MavenProject();

    @BeforeEach
    void setUp() {
        buildContext.enableLogging(new ConsoleLogger());
    }

    @Test
    void addTestCompileSourceRoot() throws MojoExecutionException, MojoFailureException {
        GenerateMojo mojo = configure(new GenerateMojo(), DefaultCodegen.class.getName());

        mojo.addCompileSourceRoot = false;
        mojo.addTestCompileSourceRoot = true;
        mojo.configOptions = singletonMap("sourceFolder", "here-i-am");

        int testRoots = mojo.project.getTestCompileSourceRoots().size();

        mojo.execute();

        assertThat(mojo.project.getTestCompileSourceRoots(), hasSize(testRoots + 1));

        String testRoot = mojo.project.getTestCompileSourceRoots().get(testRoots);

        assertThat(testRoot, endsWith(File.separator + "here-i-am"));
    }

    @Test
    void useJavaBoat() throws MojoExecutionException, MojoFailureException {
        GenerateMojo mojo = configure(new GenerateMojo(), "java");

        mojo.execute();

        assertThat(mojo.generatorName, equalTo(BoatJavaCodeGen.NAME));
    }

    @Test
    void useSpringBoat() throws MojoExecutionException, MojoFailureException {
        GenerateMojo mojo = configure(new GenerateMojo(), "spring");

        mojo.execute();

        assertThat(mojo.generatorName, equalTo(BoatSpringCodeGen.NAME));
    }

    @Test
    void useJavaBoatForRestTemplateEmbedded() throws MojoExecutionException, MojoFailureException {
        GenerateMojo mojo = configure(new GenerateRestTemplateEmbeddedMojo(), null);

        mojo.execute();

        assertThat(mojo.generatorName, equalTo(BoatJavaCodeGen.NAME));
    }

    @Test
    void useSpringBoatForSpringBootEmbedded() throws MojoExecutionException, MojoFailureException {
        GenerateMojo mojo = configure(new GenerateSpringBootEmbeddedMojo(), null);

        mojo.execute();

        assertThat(mojo.generatorName, equalTo(BoatSpringCodeGen.NAME));
    }

    @Test
    void useJavaBoatForWebClientEmbedded() throws MojoExecutionException, MojoFailureException {
        GenerateMojo mojo = configure(new GenerateWebClientEmbeddedMojo(), null);

        mojo.execute();

        assertThat(mojo.generatorName, equalTo(BoatJavaCodeGen.NAME));
    }

    @Test
    void shouldApplyDefaultConfigOptionsForSpringBoot() throws MojoExecutionException, MojoFailureException {
        GenerateMojo mojo = configure(new GenerateSpringBootEmbeddedMojo(), null);

        mojo.execute();

        Map<String, String> expectedOpts = new HashMap<>();
        expectedOpts.put("java8", "true");
        expectedOpts.put("dateLibrary", "java8");
        expectedOpts.put("performBeanValidation", "true");
        expectedOpts.put("skipDefaultInterface", "true");
        expectedOpts.put("interfaceOnly", "true");
        expectedOpts.put("useTags", "true");
        expectedOpts.put("useBeanValidation", "true");
        expectedOpts.put("useClassLevelBeanValidation", "false");
        expectedOpts.put("useOptional", "false");
        expectedOpts.put("useJakartaEe", "true");
        expectedOpts.put("useSpringBoot3", "true");
        expectedOpts.put("containerDefaultToNull", "false");

        assertNotNull(mojo.configOptions);
        expectedOpts.forEach((key, value) -> {
            assertEquals(value, mojo.configOptions.get(key));
        });
    }

    @Test
    void shouldOverrideDefaultConfigOptionsForSpringBoot() throws MojoExecutionException, MojoFailureException {
        GenerateMojo mojo = configure(new GenerateSpringBootEmbeddedMojo(), null);
        // add overrides
        mojo.configOptions = Map.of(
            "containerDefaultToNull", "true",
            "useOptional", "true"
        );

        mojo.execute();

        Map<String, String> expectedOpts = new HashMap<>();
        expectedOpts.put("java8", "true");
        expectedOpts.put("dateLibrary", "java8");
        expectedOpts.put("performBeanValidation", "true");
        expectedOpts.put("skipDefaultInterface", "true");
        expectedOpts.put("interfaceOnly", "true");
        expectedOpts.put("useTags", "true");
        expectedOpts.put("useBeanValidation", "true");
        expectedOpts.put("useClassLevelBeanValidation", "false");
        expectedOpts.put("useOptional", "true");
        expectedOpts.put("useJakartaEe", "true");
        expectedOpts.put("useSpringBoot3", "true");
        expectedOpts.put("containerDefaultToNull", "true");

        assertNotNull(mojo.configOptions);
        expectedOpts.forEach((key, value) -> {
            assertEquals(value, mojo.configOptions.get(key));
        });
    }

    private <T extends GenerateMojo> T configure(T mojo, String generatorName) {
        mojo.buildContext = buildContext;
        mojo.project = project;
        mojo.inputSpec = "src/test/resources/oas-examples/petstore.yaml";
        mojo.output = new File("target/generate-mojo-tests");
        mojo.generatorName = generatorName;
        return mojo;
    }
}
