package com.backbase.oss.boat;

import com.backbase.oss.boat.loader.OpenAPILoaderException;
import com.backbase.oss.boat.quay.BoatLinter;
import com.backbase.oss.boat.quay.model.BoatLintReport;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Parameter;

@SuppressWarnings("FieldMayBeFinal")
@Slf4j
/*
  Lint Specification
 */
public abstract class AbstractLintMojo extends InputMavenArtifactMojo {


    /**
     * Set this to <code>true</code> to fail in case a warning is found.
     */
    @Parameter(name = "failOnWarning", defaultValue = "false")
    protected boolean failOnWarning;


    /**
     * Set this to <code>true</code> to show the list of ignored rules..
     */
    @Parameter(name = "showIgnoredRules", defaultValue = "false")
    protected boolean showIgnoredRules;

    /**
     * List of rules ids which will be ignored.
     */
    @Parameter(name = "ignoreRules")
    protected String[] ignoreRules = new String[]{"150","219","215","218","166","136","174","235","107","171","224","143",
        "151","129","146","147","172","145","115","132","120", "134","183","154","105","104","130","118","110","153",
        "101","176","116","M009","H002","M010","H001","M008","S005","S006","S007","M011","B007U","B009U"};

    protected List<BoatLintReport> lint() throws MojoExecutionException, MojoFailureException {

        super.execute();

        List<BoatLintReport> boatLintReports = new ArrayList<>();

        File[] inputFiles;
        if (input.isDirectory()) {
            inputFiles = input.listFiles(pathname -> pathname.getName().endsWith(".yaml"));
            if (inputFiles == null || inputFiles.length == 0) {
                throw new MojoExecutionException("No OpenAPI specs found in: " + inputSpec);
            }
            log.info("Found " + inputFiles.length + " specs to lint.");
        } else {
            inputFiles = new File[]{input};
        }

        for (File inputFile : inputFiles) {
            boatLintReports.add(lintOpenAPI(inputFile));
        }
        return boatLintReports;

    }

    private BoatLintReport lintOpenAPI(File inputFile) throws MojoExecutionException {
        try {
            if(showIgnoredRules) {
                log.info("These rules will be ignored: {}", Arrays.toString(ignoreRules));
            }
            BoatLinter boatLinter = new BoatLinter(ignoreRules);
            return boatLinter.lint(inputFile);
        } catch (IOException e) {
            throw new MojoExecutionException("Error transforming OpenAPI: " + inputFile, e);
        } catch (OpenAPILoaderException e) {
            throw new MojoExecutionException("Cannot load OpenAPI: " + inputFile, e);
        }
    }

    @Override
    public void setInput(File input) {
        this.input = input;
    }

    public void setFailOnWarning(boolean failOnWarning) {
        this.failOnWarning = failOnWarning;
    }

    public void setIgnoreRules(String[] ignoreRules) {
        this.ignoreRules = ignoreRules;
    }
}
