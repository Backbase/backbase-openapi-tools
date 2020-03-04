package com.backbase.oss.boat;

import java.io.File;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.ResolutionScope;

@Mojo(name = "export", requiresDependencyResolution = ResolutionScope.COMPILE_PLUS_RUNTIME, threadSafe = true)
public class ExportMojo extends AbstractRamlToOpenApi {

    @Override
    public void execute() throws MojoExecutionException {

        List<org.apache.maven.artifact.Artifact> collect1 = project.getArtifacts().stream()
            .filter(dependency -> dependency.getGroupId().startsWith(includeGroupIds))
            .filter(dependency -> dependency.getArtifactId().endsWith("-spec"))
            .collect(Collectors.toList());

        for (Artifact artifact : collect1) {
            export(artifact);
        }

        writeSummary("Exported Project Dependencies");

        if (!success.isEmpty()) {
            writeSwaggerUrls(output);
        }
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
