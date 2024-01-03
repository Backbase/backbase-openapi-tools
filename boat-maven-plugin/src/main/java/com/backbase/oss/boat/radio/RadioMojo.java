package com.backbase.oss.boat.radio;

import com.backbase.oss.boat.Utils;
import com.backbase.oss.boat.bay.client.ApiClient;
import com.backbase.oss.boat.bay.client.api.BoatMavenPluginApi;
import com.backbase.oss.boat.bay.client.model.*;
import com.backbase.oss.boat.loader.OpenAPILoader;
import com.backbase.oss.boat.loader.OpenAPILoaderException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import feign.auth.BasicAuthRequestInterceptor;
import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.List;
import java.util.stream.Stream;

import static java.lang.String.format;

/**
 * Upload specs (one of more) to Boat-Bay.
 */
@Mojo(name = "radio", requiresDependencyResolution = ResolutionScope.RUNTIME, threadSafe = true)
@Slf4j
@Getter
@Setter
public class RadioMojo extends AbstractMojo {

    /**
     * Project GroupId in Boat-Bay. Defaults to {@code ${project.groupId}}
     */
    @Parameter(property = "groupId", defaultValue = "${project.groupId}")
    private String groupId;

    /**
     * Project ArtifactId in Boat-Bay. Defaults to {@code ${project.artifactId}}
     */
    @Parameter(property = "artifactId", defaultValue = "${project.artifactId}")
    private String artifactId;

    /**
     * Project Version in Boat-Bay. Defaults to {@code ${project.version}}
     */
    @Parameter(property = "version", defaultValue = "${project.version}")
    private String version;

    /**
     * Boat-Bay domain. eg. https://boatbay.mycompany.eu
     */
    @Parameter(property = "boat.bay.url", required = true)
    private String boatBayUrl;

    /**
     * Fail the build for breaking changes in specs
     */
    @Parameter(property = "failOnBreakingChange", defaultValue="false")
    private boolean failOnBreakingChange;

    /**
     * Fail the build if the spec has lint violation (Violation with Severity.MUST)
     */
    @Parameter(property = "failOnLintViolation", defaultValue="false")
    private boolean failOnLintViolation;

    /**
     * Fail the build if boatbay server returns an error
     */
    @Parameter(property = "failOnBoatBayErrorResponse", defaultValue="false")
    private boolean failOnBoatBayErrorResponse;

    /**
     * Project portal Identifier in Boat-Bay.
     */
    @Parameter(property = "portalKey", required = true)
    private String portalKey;

    /**
     * Project source identifier in Boat-Bay.
     */
    @Parameter(property = "sourceKey", required = true)
    private String sourceKey;

    /**
     * Defines the username which can access Boat-Bay upload API. Required if boat-bay APIs are protected.
     */
    @Parameter(property = "boat.bay.username")
    private String boatBayUsername;

    /**
     * Defines the password of the username which can access the Boat-Bay upload API. Required if boat-bay APIs are protected.
     */
    @Parameter(property = "boat.bay.password")
    private String boatBayPassword;

    /**
     * <p>
     * Array of spec to be uploaded. Spec fields:
     * </p>
     * <p>
     * {@code key} :
     * Spec Key in Boat-Bay. Defaults to {@code filename.lastIndexOf("-")}.
     * For example - By default {@code my-service-api-v3.1.4.yaml} would be evaluated to {@code my-service-api}
     * </p>
     * <p>
     * {@code name} :
     * Spec Name in Boat-Bay. Defaults to filename.
     * </p>
     * <p>
     * {@code inputSpec} :
     * Location of the OpenAPI spec, as URL or local file glob pattern.
     * If the input is a local file, the value of this property is considered a glob pattern that must
     * resolve to a unique file.
     * The glob pattern allows to express the input specification in a version neutral way. For
     * instance, if the actual file is {@code my-service-api-v3.1.4.yaml} the expression could be
     * {@code my-service-api-v*.yaml}.
     * </p>
     */
    @Parameter(property = "specs", required = true)
    private SpecConfig[] specs;

    /**
     * Output directory for boat-radio report.
     */
    @Parameter(name = "radioOutput", defaultValue = "${project.build.directory}/target/boat-radio-report")
    private File radioOutput;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {

        ApiClient apiClient = new ApiClient().setBasePath(boatBayUrl);
        applyAuth(apiClient);
        BoatMavenPluginApi api = apiClient.buildClient(BoatMavenPluginApi.class);

        UploadRequestBody uploadRequestBody = UploadRequestBody.builder()
                .groupId(groupId).artifactId(artifactId).version(version).build();
        for (SpecConfig spec : specs) {
            uploadRequestBody.getSpecs().add(mapToUploadSpec(spec));
        }

        try {
            List<BoatLintReport> reports = api.uploadSpec(portalKey, sourceKey, uploadRequestBody);
            writeReportFile(reports);

            if (failOnBreakingChange) {
                boolean doesSpecsHaveBreakingChanges = reports.stream()
                        .anyMatch(report -> report.getSpec().getChanges().equals(Changes.BREAKING));
                if (doesSpecsHaveBreakingChanges)
                    throw new MojoFailureException("Specs have Breaking Changes. Check full report.");
            }

            if (failOnLintViolation) {
                boolean doesSpecsHaveMustViolations = reports.stream()
                        .anyMatch(report -> report.getViolations().stream()
                                .anyMatch(violation -> violation.getSeverity().equals(Severity.MUST)));
                if (doesSpecsHaveMustViolations)
                    throw new MojoFailureException("Specs have Must Violations. Check full report.");
            }
        } catch (Exception e){
            getLog().error("BoatBay error :: " + e.getMessage());
            if (failOnBoatBayErrorResponse) {
                throw new MojoFailureException("BoatBay error", e);
            }
        }
    }

    private void writeReportFile(List<BoatLintReport> reports) throws IOException {
        ObjectMapper objectMapper = JsonMapper.builder()
                .findAndAddModules()
                .build();
        File outputFile = new File(getOutput(), "radioOutput.json");
        objectMapper.writerWithDefaultPrettyPrinter().writeValue(outputFile, reports);
        // Log summary of report
        reports.forEach(report -> {
            getLog().info(format("Spec %s summary :", report.getSpec().getKey()));
            getLog().info(format("Changes are %s ", report.getSpec().getChanges()));
            getLog().info("Number of Violations:" + report.getViolations().size());
        });
        // Log link to reports
        getLog().info("UPLOAD TO BOAT-BAY SUCCESSFUL, check the full report: " + outputFile.getCanonicalPath());
    }

    private void applyAuth(ApiClient apiClient) {
        if (StringUtils.isNotEmpty(boatBayUsername) && StringUtils.isNotEmpty(boatBayPassword)) {
            getLog().info("Basic Authentication set for username " + boatBayUsername);
            apiClient.addAuthorization(
                    "Basic Auth", new BasicAuthRequestInterceptor(boatBayUsername, boatBayPassword));
        } else {
            getLog().info("No Authentication set");
        }
    }

    private UploadSpec mapToUploadSpec(SpecConfig spec) throws MojoExecutionException {

        //Validate if the spec file path is valid and unique.
        File inputSpecFile = new File(spec.getInputSpec());
        File inputParent = inputSpecFile.getParentFile();

        if (inputParent.isDirectory()) {
            try {
                String[] files = Utils.selectInputs(inputParent.toPath(), inputSpecFile.getName());

                switch (files.length) {
                    case 0:
                        String noFileMessage = format("Input spec %s doesn't match any local file", spec.getInputSpec());
                        getLog().error(noFileMessage);
                        throw new MojoExecutionException(noFileMessage);

                    case 1:
                        inputSpecFile = new File(inputParent, files[0]);
                        spec.setInputSpec(inputSpecFile.getAbsolutePath());
                        break;

                    default:
                        String message = format("Input spec %s matches more than one single file", spec.getInputSpec());
                        getLog().error(message);
                        Stream.of(files).forEach(f -> getLog().error(format("    %s", f)));
                        throw new MojoExecutionException(message);
                }
            } catch (IOException e) {
                throw new MojoExecutionException("Cannot find input " + spec.getInputSpec());
            }
        } else {
            String message = format("Invalid parent spec folder %s ", spec.getInputSpec());
            getLog().error(message);
            throw new MojoExecutionException(message);
        }

        //Validate if the spec file is valid open-api spec
        String contents;
        try {
            contents = IOUtils.toString(inputSpecFile.toURI(), Charset.defaultCharset());
            OpenAPILoader.parse(contents);
        } catch (IOException e) {
            String msg = "Invalid File Path: " + inputSpecFile.getName();
            getLog().error(msg);
            throw new MojoExecutionException(msg, e);
        } catch (OpenAPILoaderException e) {
            String msg = "Invalid Open Api file: " + inputSpecFile.getName();
            getLog().error(msg);
            throw new MojoExecutionException(msg, e);
        }

        String key = spec.getKey();
        if (key == null || key.isEmpty()) {
            key = inputSpecFile.getName().substring(0, inputSpecFile.getName().lastIndexOf("-"));
        }

        String name = spec.getName();
        if (name == null || name.isEmpty()) {
            name = inputSpecFile.getName();
        }

        //Validation Complete. Prepare UploadSpec.
        UploadSpec uploadSpec = UploadSpec.builder()
                .fileName(inputSpecFile.getName()).key(key).name(name).openApi(contents).build();

        return uploadSpec;

    }

    @SneakyThrows
    private File getOutput() {
        if (radioOutput == null) {
            radioOutput = new File("./target/boat-radio-report");
        }
        if (!radioOutput.exists()) {
            radioOutput.mkdirs();
        }
        return radioOutput;
    }


}

