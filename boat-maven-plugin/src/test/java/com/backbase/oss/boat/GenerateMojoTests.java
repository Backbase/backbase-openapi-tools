package com.backbase.oss.boat;

import static java.util.Collections.singletonMap;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;

import com.backbase.oss.codegen.SpringBoatCodeGen;
import com.backbase.oss.codegen.StaticHtml2BoatGenerator;
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
    private final GenerateMojo mojo = new GenerateMojo();
    private final MavenProject project = new MavenProject();

    @Before
    public void setUp() {
        buildContext.enableLogging(new ConsoleLogger());
        mojo.buildContext = buildContext;
        mojo.project = project;
        mojo.inputSpec = "src/test/resources/oas-examples/petstore.yaml";
    }

    @Test
    public void addTestCompileSourceRoot() throws MojoExecutionException {
        mojo.addCompileSourceRoot = false;
        mojo.addTestCompileSourceRoot = true;

        mojo.generatorName = DefaultCodegen.class.getName();
        mojo.output = new File("target/add-test-compile-source-root");
        mojo.configOptions = singletonMap("sourceFolder", "here-i-am");

        int testRoots = mojo.project.getTestCompileSourceRoots().size();

        mojo.execute();

        assertThat(mojo.project.getTestCompileSourceRoots(), hasSize(testRoots + 1));

        String testRoot = mojo.project.getTestCompileSourceRoots().get(testRoots);

        assertThat(testRoot, endsWith("/here-i-am"));
    }

    @Test
    public void useSpringBoat() throws MojoExecutionException {
        mojo.generatorName = "spring";
        mojo.output = new File("target/use-spring-boat");

        mojo.execute();

        assertThat(mojo.generatorName, equalTo(SpringBoatCodeGen.NAME));
    }

    @Test
    public void useHtml2Boat() throws MojoExecutionException {
        mojo.generatorName = "html2";
        mojo.output = new File("target/use-html2-boat");

        mojo.execute();

        assertThat(mojo.generatorName, equalTo(StaticHtml2BoatGenerator.NAME));
    }
}


