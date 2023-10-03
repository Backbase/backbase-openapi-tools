package com.backbase.oss.boat;


import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.openapitools.openapidiff.core.OpenApiCompare;
import org.openapitools.openapidiff.core.model.ChangedOpenApi;
import org.openapitools.openapidiff.core.output.ConsoleRender;
import org.openapitools.openapidiff.core.output.HtmlRender;
import org.openapitools.openapidiff.core.output.MarkdownRender;
import org.openapitools.openapidiff.core.output.Render;

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

    @Parameter(name = "changelogRenderer", defaultValue = "markdown")
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
            } else {
                throw new MojoExecutionException("Invalid changelogRender. Supported types are 'markdown' and 'html");
            }
            try {
                FileUtils.write(new File(changelogOutput, "changelog." + extension), render.render(changedOpenApi), StandardCharsets.UTF_8);
            } catch (IOException e) {
                throw new MojoExecutionException("Failed to write output", e);
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
