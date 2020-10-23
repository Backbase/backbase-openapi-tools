package com.backbase.oss.boat;

import com.backbase.oss.boat.diff.OpenApiCompare;
import com.backbase.oss.boat.diff.model.ChangedOpenApi;
import com.backbase.oss.boat.diff.output.ConsoleRender;
import com.backbase.oss.boat.diff.output.HtmlRender;
import com.backbase.oss.boat.diff.output.JsonRender;
import com.backbase.oss.boat.diff.output.MarkdownRender;
import com.backbase.oss.boat.diff.output.Render;
import java.io.File;
import java.io.IOException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
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

    @Parameter(name = "writeChangelog", defaultValue = "false")
    private boolean writeChangelog;

    @Parameter(name = "changeLogRenderer", defaultValue = "markdown")
    private String changelogRenderer;

    @Parameter(name = "changelogOutput", defaultValue = "${project.build.directory}/changelog")
    private File changelogOutput;

    @Override
    public void execute() throws MojoExecutionException {
        ChangedOpenApi changedOpenApi = OpenApiCompare.fromFiles(oldFile, newFile);

        ConsoleRender consoleRender = new ConsoleRender();
        if (changedOpenApi.isIncompatible()) {
            log.error("\n{}", consoleRender.render(changedOpenApi));
            if (breakOnBreakingChanges) {
                throw new MojoExecutionException("newFile: " + newFile + " contains breaking changes!");
            }
        } else {
            log.info("\n{}", consoleRender.render(changedOpenApi));
        }

        if (writeChangelog) {
            Render render;

            String extension;
            if ("markdown".equals(changelogRenderer)) {
                render = new MarkdownRender();
                extension = "md";
            } else if ("html".equals(changelogRenderer)) {
                render = new HtmlRender();
                extension = "html";
            } else if ("json".equals(changelogRenderer)) {
                render = new JsonRender();
                extension = "json";
            } else {
                throw new MojoExecutionException("Invalid changelogRender. Supported types are 'markdown' and 'html");
            }
            try {
                FileUtils.write(new File(changelogOutput, "changelog." + extension), render.render(changedOpenApi));
            } catch (IOException e) {
                throw new MojoExecutionException("Failed to write markdown output", e);
            }
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

    public boolean isWriteChangelog() {
        return writeChangelog;
    }

    public void setWriteChangelog(boolean writeChangelog) {
        this.writeChangelog = writeChangelog;
    }

    public String getChangelogRenderer() {
        return changelogRenderer;
    }

    public void setChangelogRenderer(String changelogRenderer) {
        this.changelogRenderer = changelogRenderer;
    }

    public File getChangelogOutput() {
        return changelogOutput;
    }

    public void setChangelogOutput(File changelogOutput) {
        this.changelogOutput = changelogOutput;
    }
}
