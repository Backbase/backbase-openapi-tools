package com.backbase.oss.boat;


import lombok.extern.slf4j.Slf4j;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.Expand;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.impl.ArtifactResolver;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.resolution.ArtifactRequest;
import org.eclipse.aether.resolution.ArtifactResolutionException;
import org.eclipse.aether.resolution.ArtifactResult;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
public class InputMavenArtifactMojo extends AbstractMojo {

  /**
   * A maven artifact containing a spec, or multiple to be processed
   */
  @Parameter(name = "inputMavenArtifact", property = "inputMavenArtifact", required = false)
  protected InputArtifact inputMavenArtifact;

  /**
   * File input used for Linting and Validating
   * Can be directory or file
   */
  @Parameter(name = "input", required = false)
  protected File input;

  /**
   * Location of the OpenAPI spec, as URL or local file glob pattern.
   * <p>
   * If the input is a local file, the value of this property is considered a glob pattern that must
   * resolve to a unique file.
   * </p>
   * <p>
   * The glob pattern allows to express the input specification in a version neutral way. For
   * instance, if the actual file is {@code my-service-api-v3.1.4.yaml} the expression could be
   * {@code my-service-api-v*.yaml}.
   * </p>
   */
  @Parameter(name = "inputSpec", property = "openapi.generator.maven.plugin.inputSpec", required = false)
  protected String inputSpec;

  /**
   * The project being built.
   */
  @Parameter(readonly = true, required = true, defaultValue = "${project}")
  protected MavenProject project;

  /**
   * Used to set up artifact request
   */
  @Parameter(defaultValue = "${repositorySystemSession}", readonly = true)
  protected RepositorySystemSession repositorySession;

  /**
   * Used to look up Artifacts in the remote repository.
   */
  @Component
  protected ArtifactResolver artifactResolver;

  /**
   * List of Remote Repositories used by the resolver.
   */
  @Parameter(defaultValue = "${project.remoteProjectRepositories}", readonly = true)
  protected List<RemoteRepository> remoteRepositories;


  @Override
  public void execute() throws MojoExecutionException, MojoFailureException {

    if (inputMavenArtifact != null && inputMavenArtifact.getArtifactId() !=null){
      getArtifact();
    }

    if (input == null && inputSpec== null && inputMavenArtifact == null ){
      throw new MojoExecutionException("Missing input from plugin, input options are: inputMavenArtifact, input, inputSpec");
    }

    if (input == null){
      input = new File(inputSpec);
    }

  }


  private void getArtifact() throws MojoExecutionException {
    File specUnzipDirectory = new File(project.getBuild().getDirectory()
            + File.separator + "input-artifact" + File.separator
            + inputMavenArtifact.getVersion(), inputMavenArtifact.getArtifactId());


    ArtifactResult result = new ArtifactRepositoryResolver(artifactResolver, repositorySession, remoteRepositories).resolveArtifactFromRepositories(new DefaultArtifact(inputMavenArtifact.getGroupId()
            ,inputMavenArtifact.getArtifactId()
            ,inputMavenArtifact.getClassifier()
            ,inputMavenArtifact.getType()
            ,inputMavenArtifact.getVersion()));

    unzipSpec(result.getArtifact().getFile(), specUnzipDirectory);

    try (Stream<Path> walk = Files.walk(specUnzipDirectory.toPath())){

      List<String> paths = walk
              .filter(Files::isRegularFile)
              .filter(path -> path.endsWith(inputMavenArtifact.getFileName()))
              .map(Path::toString)
              .collect(Collectors.toList());

      if (paths.size()>1){
        log.info("found multiple files of matching {} in zip, using {}", inputMavenArtifact.getFileName() , paths.get(0));
      }else if(paths.isEmpty()){
        throw new MojoExecutionException("no file matching "+inputMavenArtifact.getFileName()+" was found in artifact zip");
      }

      inputSpec = paths.get(0);
      input = new File(paths.get(0));

    } catch (IOException e) {
      log.debug(e.getMessage());
      throw new MojoExecutionException("Could not search unzipped artifact directory");
    }

  }


  private void unzipSpec(File inputFile, File specUnzipDirectory) throws MojoExecutionException {
    specUnzipDirectory.mkdirs();
    try {
      unzip(inputFile, specUnzipDirectory);
    } catch (Exception e) {
      throw new MojoExecutionException("Error extracting spec: " + inputFile, e);
    }
  }

  private void unzip(File source, File out) throws Exception {
    Expand expand = new Expand();
    expand.setSrc(source);
    expand.setDest(out);
    expand.setOverwrite(true);
    expand.execute();
  }

  public void setInput(File input) {
    this.input = input;
  }

}




