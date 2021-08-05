package com.backbase.oss.boat;

import org.apache.maven.model.Build;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectHelper;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class ShipMojoTests {

    @Test
    void testInputDirectoryAndOutputFile() {
        ShipMojo mojo = new ShipMojo();
        MavenProject mavenProject = new MavenProject();
        Build build = new Build();
        build.setFinalName("api-1.0-SNAPSHOT");
        build.setOutputDirectory("target");
        mavenProject.setBuild(build);

        mojo.setFinalName("petstore-1.0-SNAPSHOT");
        File parentFile = new File(getClass().getResource("/bundler/folder/one-client-api-v1.yaml").getFile()).getParentFile();
        mojo.setInput(parentFile);
        mojo.setIncludes(new String[]{"**/*.yaml"});
        mojo.setBaseDir(parentFile);
        mojo.setProject(mavenProject);
        mojo.setProjectHelper(new MavenProjectHelper() {
            @Override
            public void attachArtifact(MavenProject project, File artifactFile, String artifactClassifier) {

            }

            @Override
            public void attachArtifact(MavenProject project, String artifactType, File artifactFile) {

            }

            @Override
            public void attachArtifact(MavenProject project, String artifactType, String artifactClassifier, File artifactFile) {

            }

            @Override
            public void addResource(MavenProject project, String resourceDirectory, List<String> includes, List<String> excludes) {

            }

            @Override
            public void addTestResource(MavenProject project, String resourceDirectory, List<String> includes, List<String> excludes) {

            }
        });
        mojo.setInput(new File("."));
        assertDoesNotThrow(mojo::execute);
    }


}
