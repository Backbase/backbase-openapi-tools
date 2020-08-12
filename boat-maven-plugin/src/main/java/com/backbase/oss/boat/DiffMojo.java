package com.backbase.oss.boat;

import com.qdesrame.openapi.diff.OpenApiCompare;
import com.qdesrame.openapi.diff.model.ChangedOpenApi;
import com.qdesrame.openapi.diff.output.ConsoleRender;
import java.io.File;
import lombok.extern.slf4j.Slf4j;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;

/**
 * Calculates a Change log for APIs.
 */
@Mojo(name = "diff", requiresDependencyResolution = ResolutionScope.RUNTIME, threadSafe = true)
@Slf4j
public class DiffMojo extends AbstractMojo {

    public static final String X_CHANGELOG = "x-changelog";

    @Parameter(name = "oldFile", defaultValue = "true")
    private File oldFile;

    @Parameter(name = "newFile", defaultValue = "true")
    private File newFile;

    @Parameter(name = "breakOnBreakingChanges", defaultValue = "true")
    private boolean breakOnBreakingChanges;

    @Override
    public void execute() throws MojoExecutionException {
        ChangedOpenApi changedOpenApi = OpenApiCompare.fromFiles(oldFile, newFile);

        ConsoleRender consoleRender = new ConsoleRender();
        if (!changedOpenApi.isDiffBackwardCompatible()) {
            log.error("\n{}", consoleRender.render(changedOpenApi));
            if (breakOnBreakingChanges) {
                throw new MojoExecutionException("newFile: " + newFile + " contains breaking changes!");
            }
        } else {
            log.info("\n{}", consoleRender.render(changedOpenApi));
        }

    }

    public void setNewFile(File newFile) {
        this.newFile = newFile;
    }

    public void setOldFile(File oldFile) {
        this.oldFile = oldFile;
    }

    public void setBreakOnBreakingChanges(boolean breakOnBreakingChanges) {
        this.breakOnBreakingChanges = breakOnBreakingChanges;
    }
}
