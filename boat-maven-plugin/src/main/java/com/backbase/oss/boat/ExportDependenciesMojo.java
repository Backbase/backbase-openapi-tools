package com.backbase.oss.boat;

import java.io.File;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.ResolutionScope;

/**
 * Exports project dependencies where the ArtifactId ends with "-spec".
 */
@Mojo(name = "export-dep", requiresDependencyResolution = ResolutionScope.COMPILE_PLUS_RUNTIME, threadSafe = true)
public class ExportDependenciesMojo extends AbstractRamlToOpenApi {

    @Override
    public void execute() throws MojoExecutionException {

        List<org.apache.maven.artifact.Artifact> artifacts = project.getArtifacts().stream()
            .filter(dependency -> dependency.getGroupId().startsWith(includeGroupIds))
            .filter(dependency -> dependency.getArtifactId().endsWith("-spec") || dependency.getArtifactId().endsWith("-specs"))
            .collect(Collectors.toList());

        for (Artifact artifact : artifacts) {
            export(artifact);
        }

        writeSummary("Exported Project Dependencies");
    }


    private void export(Artifact artifact) throws MojoExecutionException {
        File outputDirectory = new File(this.output, artifact.getGroupId().replace('.', '/'));
        exportArtifact(artifact.getGroupId(), artifact.getArtifactId(), artifact.getVersion(), artifact.getFile(), outputDirectory);
    }

    private void export(org.eclipse.aether.artifact.Artifact artifact) throws MojoExecutionException {
        File outputDirectory = new File(this.output, artifact.getGroupId().replace('.', '/'));
        exportArtifact(artifact.getGroupId(), artifact.getArtifactId(), artifact.getVersion(), artifact.getFile(), outputDirectory);
    }
}
