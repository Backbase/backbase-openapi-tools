package com.backbase.oss.boat;

import java.io.File;
import java.util.Collections;
import java.util.Locale;
import java.util.TimeZone;

import lombok.extern.slf4j.Slf4j;
import org.apache.maven.artifact.DefaultArtifact;
import org.apache.maven.artifact.handler.DefaultArtifactHandler;
import org.apache.maven.model.Build;
import org.apache.maven.model.Dependency;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.logging.console.ConsoleLogger;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.sonatype.plexus.build.incremental.DefaultBuildContext;

import static org.junit.jupiter.api.Assertions.assertTrue;

@Slf4j
class ExportDependencyMojoTests {

    @BeforeAll
    static void setupLocale() {
        Locale.setDefault(Locale.ENGLISH);
        TimeZone.setDefault(TimeZone.getTimeZone("Europe/London"));
    }

    @Test
    void testAggregatedInputFile() throws MojoExecutionException, ExportException {

        ExportDependenciesMojo mojo = new ExportDependenciesMojo();

        DefaultBuildContext defaultBuildContext = new DefaultBuildContext();
        defaultBuildContext.enableLogging(new ConsoleLogger(2, "BOAT"));

        String groupId = "test.groupId";
        String artifactId = "artifact-spec";
        String version = "2.19.0";
        String scope = "provided";
        String type = "jar";

        Dependency specDependency = new Dependency();
        specDependency.setGroupId(groupId);
        specDependency.setArtifactId(artifactId);
        specDependency.setVersion(version);

        DefaultArtifactHandler artifactHandler = new DefaultArtifactHandler();
        DefaultArtifact specArtifact = new DefaultArtifact(groupId, artifactId, version, scope, type, null,
            artifactHandler);
        specArtifact.setFile(getFile("/raml-examples/aggregated-spec.zip"));

        Build build = new Build();
        build.setDirectory("target");


        MavenProject project = new MavenProject();
        project.setArtifacts(Collections.singleton(specArtifact));
        project.setDependencies(Collections.singletonList(specDependency));

        project.setBuild(build);

        mojo.project = project;
        mojo.includeGroupIds = "test.";
        mojo.ramlFileFilters = "**/*-api.raml,**/api.raml";
        mojo.output = new File("target/export-aggregated-dep");
        mojo.continueOnError = false;
        mojo.execute();

        assertTrue(new File("target/export-aggregated-dep/test/groupId/artifact-spec/backbase-wallet/presentation-client-api/openapi.yaml").exists());

    }

    @Test
    void testInputFile() throws MojoExecutionException, ExportException {
        String groupId = "test.groupId";
        String artifactId = "artifact-spec";
        String version = "2.19.0";
        String scope = "provided";
        String type = "jar";

        testExportDep(groupId, artifactId, version, scope, type);
    }

    @Test
    void testInputFile2() throws MojoExecutionException, ExportException {
        String groupId = "test.groupId";
        String artifactId = "artifact-specs";
        String version = "2.19.0";
        String scope = "provided";
        String type = "jar";

        testExportDep(groupId, artifactId, version, scope, type);
    }

    private void testExportDep(String groupId, String artifactId, String version, String scope, String type)
        throws MojoExecutionException {
        ExportDependenciesMojo mojo = new ExportDependenciesMojo();

        DefaultBuildContext defaultBuildContext = new DefaultBuildContext();
        defaultBuildContext.enableLogging(new ConsoleLogger(2, "BOAT"));

        Dependency specDependency = new Dependency();
        specDependency.setGroupId(groupId);
        specDependency.setArtifactId(artifactId);
        specDependency.setVersion(version);

        DefaultArtifactHandler artifactHandler = new DefaultArtifactHandler();
        DefaultArtifact specArtifact = new DefaultArtifact(groupId, artifactId, version, scope, type, null,
            artifactHandler);
        specArtifact.setFile(getFile("/raml-examples/backbase-wallet.zip"));

        Build build = new Build();
        build.setDirectory("target");

        MavenProject project = new MavenProject();
        project.setArtifacts(Collections.singleton(specArtifact));
        project.setDependencies(Collections.singletonList(specDependency));

        project.setBuild(build);

        mojo.project = project;
        mojo.includeGroupIds = "test.";
        mojo.ramlFileFilters = "**/*-api.raml,**/api.raml";
        mojo.output = new File("target/export-dep");
        mojo.continueOnError = false;
        mojo.execute();

        assertTrue(new File("target/export-dep/test/groupId/artifact-spec/backbase-wallet/presentation-client-api/openapi.yaml").exists());
    }

    private File getFile(String fileName) {
        return new File(getClass().getResource(fileName).getFile());
    }


}
