package com.backbase.oss.boat;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectHelper;
import org.codehaus.plexus.archiver.util.DefaultFileSet;
import org.codehaus.plexus.archiver.zip.ZipArchiver;
import org.codehaus.plexus.util.DirectoryScanner;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

import static java.util.Arrays.stream;

/**
 Zips all API's and attach it to the the Maven Project Reactor for publishing
 */
@Mojo(name = "ship", requiresDependencyResolution = ResolutionScope.RUNTIME, threadSafe = true)
@Slf4j
@Getter
@Setter
public class ShipMojo extends AbstractMojo {

    @Component
    protected MavenProjectHelper projectHelper;


    @Parameter(name = "input", required = true, defaultValue = "${project.basedir}/src/main/resources")
    protected File input;

    @Parameter(name = "includes", defaultValue = "**/openapi.yaml, **/*api*.yaml")
    protected String[] includes;

    @Parameter(name = "project", defaultValue = "${project}", readonly = true)
    protected MavenProject project;

    @Parameter(defaultValue = "${project.build.finalName}")
    private String finalName;

    @Parameter(defaultValue = "${project.basedir}")
    private File baseDir;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        final String[] inputFiles;
        if (input.isDirectory()) {
            log.info("Scanning directory: {} with include patterns: {}", input, includes);
            DirectoryScanner directoryScanner = new DirectoryScanner();
            directoryScanner.setBasedir(input);
            directoryScanner.setIncludes(includes);
            directoryScanner.scan();
            inputFiles = directoryScanner.getIncludedFiles();
            if(inputFiles.length == 0) {
                throw new MojoExecutionException("No API's found in directory: " + input + " with include patterns: " + Arrays.toString(includes));
            }
            log.info("Found " + inputFiles.length + " specs to sip");
        } else {
            inputFiles = new String[]{input.toPath().relativize(project.getBasedir().toPath()).toString()};
        }

        try {
            File apiZipFile = zip(inputFiles);
            log.info("Attaching API's to project reactor as {} ready to ship!", apiZipFile);
            projectHelper.attachArtifact(project, "zip", "api",apiZipFile);
        } catch (IOException e) {
            throw new MojoExecutionException("Failed to zip files due to: " + e.getMessage(), e);
        }
    }

    private File zip(String[] inputFiles) throws IOException {
        File api = createFileName(new File(project.getBuild().getOutputDirectory()), finalName, "api");
        log.info("Zipping OpenApi Files: {} to: {}", inputFiles, api);

        DefaultFileSet fileSet = new DefaultFileSet();
        fileSet.setDirectory(input);
        fileSet.setUsingDefaultExcludes(true);
        fileSet.setIncludes(inputFiles);

        ZipArchiver zipArchiver = new ZipArchiver();
        zipArchiver.addFileSet(fileSet);
        zipArchiver.setDestFile(api);
        zipArchiver.createArchive();
        return zipArchiver.getDestFile();
    }

    /**
     * Returns the Zip file to generate with classifier to be attached to the project
     *
     * @param basedir         the output directory
     * @param resultFinalName the name of the file
     * @param classifier      an optional classifier
     * @return the file to generate
     */
    public static File createFileName(File basedir, String resultFinalName, String classifier) {
        if (basedir == null) {
            throw new IllegalArgumentException("basedir is not allowed to be null");
        }
        if (resultFinalName == null) {
            throw new IllegalArgumentException("finalName is not allowed to be null");
        }
        String fileName = resultFinalName + "-" + classifier + ".zip";
        return new File(basedir, fileName);
    }
}
