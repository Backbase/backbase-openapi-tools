package com.backbase.oss.boat;

import static java.util.Collections.singletonMap;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;

import com.backbase.oss.codegen.BoatJavaCodeGen;
import com.backbase.oss.codegen.BoatSpringCodeGen;
import java.io.File;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.logging.console.ConsoleLogger;
import org.junit.Before;
import org.junit.Test;
import org.openapitools.codegen.DefaultCodegen;
import org.sonatype.plexus.build.incremental.DefaultBuildContext;

public class GenerateMojoTests {
    private final DefaultBuildContext buildContext = new DefaultBuildContext();
    private final MavenProject project = new MavenProject();

    @Before
    public void setUp() {
        buildContext.enableLogging(new ConsoleLogger());
    }

    @Test
    public void addTestCompileSourceRoot() throws MojoExecutionException {
        GenerateMojo mojo = configure(new GenerateMojo(), DefaultCodegen.class.getName());

        mojo.addCompileSourceRoot = false;
        mojo.addTestCompileSourceRoot = true;
        mojo.configOptions = singletonMap("sourceFolder", "here-i-am");

        int testRoots = mojo.project.getTestCompileSourceRoots().size();

        mojo.execute();

        assertThat(mojo.project.getTestCompileSourceRoots(), hasSize(testRoots + 1));

        String testRoot = mojo.project.getTestCompileSourceRoots().get(testRoots);

        assertThat(testRoot, endsWith("/here-i-am"));
    }

    @Test
    public void useJavaBoat() throws MojoExecutionException {
        GenerateMojo mojo = configure(new GenerateMojo(), "java");

        mojo.execute();

        assertThat(mojo.generatorName, equalTo(BoatJavaCodeGen.NAME));
    }

    @Test
    public void useSpringBoat() throws MojoExecutionException {
        GenerateMojo mojo = configure(new GenerateMojo(), "spring");

        mojo.execute();

        assertThat(mojo.generatorName, equalTo(BoatSpringCodeGen.NAME));
    }

    @Test
    public void useJavaBoatForRestTemplateEmbedded() throws MojoExecutionException {
        GenerateMojo mojo = configure(new GenerateRestTemplateEmbeddedMojo(), null);

        mojo.execute();

        assertThat(mojo.generatorName, equalTo(BoatJavaCodeGen.NAME));
    }

    @Test
    public void useSpringBoatForSpringBootEmbedded() throws MojoExecutionException {
        GenerateMojo mojo = configure(new GenerateSpringBootEmbeddedMojo(), null);

        mojo.execute();

        assertThat(mojo.generatorName, equalTo(BoatSpringCodeGen.NAME));
    }

    @Test
    public void useJavaBoatForWebClientEmbedded() throws MojoExecutionException {
        GenerateMojo mojo = configure(new GenerateWebClientEmbeddedMojo(), null);

        mojo.execute();

        assertThat(mojo.generatorName, equalTo(BoatJavaCodeGen.NAME));
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


