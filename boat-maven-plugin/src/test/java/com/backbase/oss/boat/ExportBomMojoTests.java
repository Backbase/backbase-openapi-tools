package com.backbase.oss.boat;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.util.Collections;
import java.util.List;
import org.apache.maven.model.Build;
import org.apache.maven.model.Dependency;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.logging.console.ConsoleLogger;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.impl.ArtifactResolver;
import org.eclipse.aether.impl.MetadataResolver;
import org.eclipse.aether.metadata.Metadata;
import org.eclipse.aether.resolution.ArtifactResolutionException;
import org.eclipse.aether.resolution.ArtifactResult;
import org.eclipse.aether.resolution.MetadataResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sonatype.plexus.build.incremental.DefaultBuildContext;

@ExtendWith(MockitoExtension.class)
class ExportBomMojoTests {
  @Mock
  ArtifactResolver artifactResolver;
  @Mock
  ArtifactResult artifactResult;
  @Mock
  MetadataResolver metadataResolver;

  @Mock
  MetadataResult metadataResult;
  @Mock
  Metadata metadatamock;


  @Test
  void testExportBomEmptyMeta() throws MojoExecutionException {
    artifactResolver = mock(ArtifactResolver.class);
    artifactResult = mock( ArtifactResult.class);
    metadataResolver = mock(MetadataResolver.class);

    when(metadataResolver.resolveMetadata(any(),any())).thenReturn(Collections.singletonList(metadataResult));

    ExportBomMojo mojo = new ExportBomMojo();
    File output = new File("target/boat-bom-export");
    if (!output.exists()) {
      output.mkdirs();
    }

    DefaultBuildContext defaultBuildContext = new DefaultBuildContext();
    defaultBuildContext.enableLogging(new ConsoleLogger());

    mojo.getLog();
    mojo.artifactResolver = artifactResolver;
    mojo.metadataResolver = metadataResolver;
    mojo.setSpecBom(new Dependency());


    Build build = new Build();
    build.setDirectory("target");

    MavenProject project = new MavenProject();
    mojo.remoteRepositories = Collections.emptyList();
    project.setBuild(build);
    mojo.project = project;
    mojo.repositorySession = mock(RepositorySystemSession.class);
    mojo.execute();

    assertThat(output).isEmptyDirectory();

  }

  @Test
  void testExportBomUseOfArtifactResolver() throws MojoFailureException, MojoExecutionException, ArtifactResolutionException {
    File versionFile = getFile("/export-bom/maven-metadata-test-example.xml");
    String groupId="test.groupId";
    String artifactId = "raml-bom";
    String type = "pom";
    String version= "[1.0.0,)";

    File pomFile = getFile("/export-bom/raml-spec-bom/pom.xml");
    artifactResolver = mock(ArtifactResolver.class);
    artifactResult = mock( ArtifactResult.class);
    org.eclipse.aether.artifact.Artifact artifact; //= mock(org.eclipse.aether.artifact.Artifact.class);
    artifact = new DefaultArtifact(groupId, artifactId, "", type, version,Collections.emptyMap(), pomFile);


    when(artifactResolver.resolveArtifact(any(),any())).thenReturn(artifactResult);
    when(artifactResult.getArtifact()).thenReturn(artifact);
    //when(artifact.getFile()).thenReturn(pomFile);


    when(metadataResolver.resolveMetadata(any(),any())).thenReturn(Collections.singletonList(metadataResult));

    ExportBomMojo mojo = new ExportBomMojo();
    File output = new File("target/boat-bom-export");
    if (!output.exists()) {
      output.mkdirs();
    }
    when(metadataResult.isResolved()).thenReturn(true);


    doReturn(metadatamock).when(metadataResult).getMetadata();
    doReturn(versionFile).when(metadatamock).getFile();

    DefaultBuildContext defaultBuildContext = new DefaultBuildContext();
    defaultBuildContext.enableLogging(new ConsoleLogger());

    mojo.getLog();
    mojo.artifactResolver = artifactResolver;
    mojo.metadataResolver = metadataResolver;
    Dependency dependency =  new Dependency();

    dependency.setType(type);
    dependency.setGroupId(groupId);
    dependency.setArtifactId(artifactId);
    dependency.setVersion(version);
    mojo.setSpecBom(dependency);


    Build build = new Build();
    build.setDirectory("target");

    MavenProject project = new MavenProject();
    mojo.remoteRepositories = Collections.emptyList();

    project.setBuild(build);
    mojo.project = project;
    mojo.repositorySession = mock(RepositorySystemSession.class);
    mojo.execute();

    assertThat(output).isEmptyDirectory();

  }
  private File getFile(String fileName) {
    return new File(getClass().getResource(fileName).getFile());
  }

}
