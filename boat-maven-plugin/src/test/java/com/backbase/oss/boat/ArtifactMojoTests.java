package com.backbase.oss.boat;

import org.apache.maven.model.Build;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.logging.console.ConsoleLogger;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.impl.ArtifactResolver;
import org.eclipse.aether.resolution.ArtifactResolutionException;
import org.eclipse.aether.resolution.ArtifactResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sonatype.plexus.build.incremental.DefaultBuildContext;

import java.io.File;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class ArtifactMojoTests {

  ArtifactResolver artifactResolver;
  ArtifactResult artifactResult;

  @Test
  void testArtifactResolver(){

    GenerateDocMojo mojo = new GenerateDocMojo();
    File output = new File("target/boat-docs");
    if (!output.exists()) {
      output.mkdirs();
    }

    DefaultBuildContext defaultBuildContext = new DefaultBuildContext();
    defaultBuildContext.enableLogging(new ConsoleLogger());

    mojo.getLog();
    mojo.buildContext = defaultBuildContext;
    mojo.project = new MavenProject();
    mojo.output = output;
    mojo.skip = false;
    mojo.skipIfSpecIsUnchanged = false;
    mojo.bundleSpecs = true;
    mojo.dereferenceComponents = true;

    assertThrows(MojoExecutionException.class, mojo::execute);

  }

  @Test
  void testArtifactInputMojo() throws ArtifactResolutionException, MojoFailureException, MojoExecutionException {
    File file = getFile("/oas-examples/openapi-zips-1.0.0-SNAPSHOT-api.zip");
    artifactResolver = mock(ArtifactResolver.class);
    artifactResult = mock( ArtifactResult.class);
    org.eclipse.aether.artifact.Artifact artifact = mock(org.eclipse.aether.artifact.Artifact.class);

    when(artifactResolver.resolveArtifact(any(),any())).thenReturn(artifactResult);
    when(artifactResult.getArtifact()).thenReturn(artifact);
    when(artifact.getFile()).thenReturn(file);

    GenerateDocMojo mojo = new GenerateDocMojo();
    File output = new File("target/boat-docs");
    if (!output.exists()) {
      output.mkdirs();
    }

    DefaultBuildContext defaultBuildContext = new DefaultBuildContext();
    defaultBuildContext.enableLogging(new ConsoleLogger());
    InputArtifact inputArtifact = new InputArtifact();
    inputArtifact.setVersion("1.0.0-SNAPSHOT");
    inputArtifact.setGroupId("test.groupId");
    inputArtifact.setArtifactId("openapi-zips");
    inputArtifact.setClassifier("api");
    inputArtifact.setType("zip");
    inputArtifact.setFileName("presentation-integration-api/openapi.yaml");

    mojo.inputMavenArtifact=inputArtifact;
    mojo.getLog();
    mojo.buildContext = defaultBuildContext;
    mojo.artifactResolver = artifactResolver;
    Build build = new Build();
    build.setDirectory("target");


    MavenProject project = new MavenProject();

    project.setBuild(build);
    mojo.project = project;
    mojo.repositorySession = mock(RepositorySystemSession.class);
    mojo.output = output;
    mojo.skip = false;
    mojo.skipIfSpecIsUnchanged = false;
    mojo.bundleSpecs = true;
    mojo.dereferenceComponents = true;
    mojo.execute();

    assertThat(output.list()).containsExactlyInAnyOrder("index.html", ".openapi-generator-ignore", ".openapi-generator");

  }
  private File getFile(String fileName) {
    return new File(getClass().getResource(fileName).getFile());
  }



}
