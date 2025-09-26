package com.backbase.oss.boat;

import static java.util.Collections.singletonList;
import static java.util.Collections.singletonMap;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.backbase.oss.codegen.java.BoatJavaCodeGen;
import com.backbase.oss.codegen.java.BoatSpringCodeGen;
import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;
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

        mojo.setEnumNameMappings(singletonList("LARGE=SUPER-BIG"));
        mojo.setNameMappings(singletonList("size=petSize"));

        mojo.execute();

        assertGeneratedClientModels(mojo.output, "Pet.java", "petSize");
        assertGeneratedClientModels(mojo.output, "Size.java", "SUPER-BIG");

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
        assertEquals(
            "ApiClient.java,ApiKeyAuth.java,Authentication.java,BeanValidationException.java," +
                "HttpBasicAuth.java,HttpBearerAuth.java,JavaTimeFormatter.java," +
                "RFC3339DateFormat.java,ServerConfiguration.java,ServerVariable.java,StringUtil.java",
            mojo.supportingFilesToGenerate
        );
        assertThat(
            mojo.getGeneratorSpecificSupportingFiles(),
            containsInAnyOrder("BigDecimalCustomSerializer.java")
        );
    }

    @Test
    void useSpringBoatForSpringBootEmbedded() throws MojoExecutionException, MojoFailureException {
        GenerateMojo mojo = configure(new GenerateSpringBootEmbeddedMojo(), null);

        mojo.execute();

        assertThat(mojo.generatorName, equalTo(BoatSpringCodeGen.NAME));
        assertEquals(
            "ApiClient.java,ApiKeyAuth.java,Authentication.java,BeanValidationException.java," +
                "HttpBasicAuth.java,HttpBearerAuth.java,JavaTimeFormatter.java," +
                "RFC3339DateFormat.java,ServerConfiguration.java,ServerVariable.java,StringUtil.java",
            mojo.supportingFilesToGenerate
        );
        assertThat(
            mojo.getGeneratorSpecificSupportingFiles(),
            containsInAnyOrder("BigDecimalCustomSerializer.java")
        );
    }

    @Test
    void useJavaBoatForWebClientEmbedded() throws MojoExecutionException, MojoFailureException {
        GenerateMojo mojo = configure(new GenerateWebClientEmbeddedMojo(), null);

        mojo.execute();

        assertThat(mojo.generatorName, equalTo(BoatJavaCodeGen.NAME));
        assertEquals(
            "ApiClient.java,ApiKeyAuth.java,Authentication.java,BeanValidationException.java," +
                "HttpBasicAuth.java,HttpBearerAuth.java,JavaTimeFormatter.java," +
                "RFC3339DateFormat.java,ServerConfiguration.java,ServerVariable.java,StringUtil.java",
            mojo.supportingFilesToGenerate
        );
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

    private void assertGeneratedClientModels(File mojoOutput, String fileName, String... expectedProperties) {
        File dir = mojoOutput.getAbsoluteFile()
            .toPath()
            .resolve("src/main/java/org/openapitools/client/model")
            .toFile();
        File target = new File(dir, fileName);
        assertThat(target.exists(), is(true));
        try {
            String content = java.nio.file.Files.readString(
                target.toPath(),
                java.nio.charset.StandardCharsets.UTF_8
            );
            assertAll(Stream.of(expectedProperties).map(prop ->
                () -> assertThat("Expected content not found: " + prop, content.contains(prop), is(true))
            ));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
