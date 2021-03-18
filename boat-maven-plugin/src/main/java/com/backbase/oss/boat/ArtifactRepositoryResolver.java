package com.backbase.oss.boat;

import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.impl.ArtifactResolver;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.resolution.ArtifactRequest;
import org.eclipse.aether.resolution.ArtifactResolutionException;
import org.eclipse.aether.resolution.ArtifactResult;

import java.util.List;
public class ArtifactRepositoryResolver {

  private ArtifactResolver artifactResolver;
  private RepositorySystemSession repositorySession;
  private List<RemoteRepository> remoteRepositories;

  public ArtifactRepositoryResolver(ArtifactResolver artifactResolver, RepositorySystemSession repositorySession, List<RemoteRepository> remoteRepositories) {
    this.artifactResolver = artifactResolver;
    this.repositorySession = repositorySession;
    this.remoteRepositories = remoteRepositories;
  }


  public ArtifactResult resolveArtifactFromRepositories(org.eclipse.aether.artifact.Artifact artifact) {
    ArtifactRequest artifactRequest = getArtifactRequest(artifact);

    ArtifactResult artifactResult = null;
    try {
      artifactResult = artifactResolver.resolveArtifact(repositorySession, artifactRequest);
    } catch (ArtifactResolutionException e) {
      throw new IllegalArgumentException("Cannot resolve artifact: " + artifact);
    }
    return artifactResult;

  }


  private ArtifactRequest getArtifactRequest(org.eclipse.aether.artifact.Artifact artifact) {

    return new ArtifactRequest(artifact, remoteRepositories, null);
  }

}
