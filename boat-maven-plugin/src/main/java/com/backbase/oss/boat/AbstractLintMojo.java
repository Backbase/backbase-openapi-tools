package com.backbase.oss.boat;

import com.backbase.oss.boat.quay.BoatLinter;
import com.backbase.oss.boat.quay.model.BoatLintReport;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;

@SuppressWarnings("FieldMayBeFinal")
@Mojo(name = "lint", requiresDependencyResolution = ResolutionScope.RUNTIME, threadSafe = true)
@Slf4j
/*
  Lint Specification
 */
public abstract class AbstractLintMojo extends AbstractMojo {

    @Parameter(name = "inputSpec", property = "inputSpec", required = true)
    protected File inputSpec;

    @Parameter(name = "failOnWarning", defaultValue = "false")
    protected boolean failOnWarning;

    @Parameter(name = "ignoreRules")
    protected String[] ignoreRules = new String[]{"219", "105", "M008", " M009", " M010", " M011", " H001", " H002", " S005", " S006", " S007"};


    protected List<BoatLintReport> lint() throws MojoExecutionException {
        List<BoatLintReport> boatLintReports = new ArrayList<>();

        File[] inputFiles;
        if (inputSpec.isDirectory()) {
            inputFiles = inputSpec.listFiles(pathname -> pathname.getName().endsWith(".yaml"));
            if (inputFiles == null) {
                throw new MojoExecutionException("No OpenAPI specs found in: " + inputSpec);
            }
            log.info("Found " + inputFiles.length + " specs to lint.");
        } else {
            inputFiles = new File[]{inputSpec};
        }

        for (File inputFile : inputFiles) {
            boatLintReports.add(lintOpenAPI(inputFile));
        }
        return boatLintReports;

    }

    private BoatLintReport lintOpenAPI(File inputFile) throws MojoExecutionException {
        try {
            BoatLinter boatLinter = new BoatLinter(ignoreRules);
            return boatLinter.lint(inputFile);

        } catch (IOException e) {
            throw new MojoExecutionException("Error transforming OpenAPI: " + inputFile, e);
        }
    }


    public void setInput(File input) {
        this.inputSpec = input;
    }

    public void setFailOnWarning(boolean failOnWarning) {
        this.failOnWarning = failOnWarning;
    }

    public void setIgnoreRules(String[] ignoreRules) {
        this.ignoreRules = ignoreRules;
    }
}
