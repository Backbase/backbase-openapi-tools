package com.backbase.oss.boat;

import com.backbase.oss.boat.diff.BatchOpenApiDiff;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Calculates a Change log for APIs.
 */
@Mojo(name = "diff", requiresDependencyResolution = ResolutionScope.RUNTIME, threadSafe = true)
public class DiffMojo extends AbstractRamlToOpenApi {

    public static final String X_CHANGELOG = "x-changelog";

    private static final Logger log = LoggerFactory.getLogger(DiffMojo.class);

    @Parameter(name = "insertIntoSpec", defaultValue = "true")
    private boolean insertIntoSpec;

    @Parameter(name = "writeChangeLogToSeparateFile", defaultValue = "true")
    private boolean writeChangeLogToSeparateFile;

    @Override
    public void execute() throws MojoExecutionException {
        if (output.exists()) {
            try {
                BatchOpenApiDiff.diff(output.toPath(), success, failed, insertIntoSpec, writeChangeLogToSeparateFile);
                writeSummary("Calculated Change log for APIs");
            } catch (Exception e) {
                throw new MojoExecutionException("Cannot create diff", e);
            }
        }
    }

}
