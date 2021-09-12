package com.backbase.oss.boat;

import com.backbase.oss.boat.bay.client.ApiClient;
import com.backbase.oss.boat.bay.client.api.BoatMavenPluginApi;
import com.backbase.oss.boat.bay.client.model.BoatLintReport;
import com.backbase.oss.boat.bay.client.model.UploadRequestBody;
import com.backbase.oss.boat.bay.client.model.UploadSpec;
import com.backbase.oss.boat.loader.OpenAPILoader;
import com.backbase.oss.boat.loader.OpenAPILoaderException;
import com.fasterxml.jackson.databind.ObjectMapper;
import feign.auth.BasicAuthRequestInterceptor;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;

import static java.lang.String.format;

/**
 * Uploads specs (one of more) to Boat-Bay.
 */
@Mojo(name = "radio", requiresDependencyResolution = ResolutionScope.RUNTIME, threadSafe = true)
@Slf4j
@Getter
@Setter
public class RadioMojo extends AbstractMojo {

    /**
     * Project GroupId in Boat-Bay. Defaults to {@code ${project.groupId}}
     */
    @Parameter(property = "groupId", defaultValue = "${project.groupId}", required = true)
    private String groupId;

    /**
     * Project ArtifactId in Boat-Bay. Defaults to {@code ${project.artifactId}}
     */
    @Parameter(property = "artifactId", defaultValue = "${project.artifactId}", required = true)
    private String artifactId;

    /**
     * Project Version in Boat-Bay. Defaults to {@code ${project.version}}
     */
    @Parameter(property = "version", defaultValue = "${project.version}", required = true)
    private String version;

    /**
     * Boat-Bay domain. eg. https://boatbay.mycompany.eu
     */
    @Parameter(property = "basePath", required = true)
    private String basePath;

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
    @Parameter(property = "username", required = false)
    private String username;

    /**
     * Defines the password of the username which can access the Boat-Bay upload API. Required if boat-bay APIs are protected.
     */
    @Parameter(property = "password", required = false)
    private String password;

    /**
     * Array of specs to be uploaded.
     */
    @Parameter(property = "specs")
    private SpecConfig[] specs;

    /**
     * Output directory for boat-radio report.
     */
    @Parameter(name = "radioOutput", defaultValue = "${project.build.directory}/target/boat-radio-report")
    private File radioOutput;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {

        BasicAuthRequestInterceptor basicAuthRequestInterceptor = null;

        ObjectMapper objectMapper = new ObjectMapper();

        if (username != null && !username.isEmpty() && password != null && !password.isEmpty()) {
            getLog().info("Basic Authentication set for username "+username);
            basicAuthRequestInterceptor = new BasicAuthRequestInterceptor(username, password);
        } else {
            getLog().info("No Authentication set");
        }

        List<UploadSpec> allSpecs = new ArrayList<>();

        for (SpecConfig spec : specs) {
            allSpecs.add(mapToUploadSpec(spec));
        }

        ApiClient apiClient = new ApiClient().setBasePath(basePath);
        if (basicAuthRequestInterceptor != null) {
            apiClient.addAuthorization("Basic Auth", basicAuthRequestInterceptor);
        }

        BoatMavenPluginApi api = apiClient.buildClient(BoatMavenPluginApi.class);

        UploadRequestBody uploadRequestBody = new UploadRequestBody();
        uploadRequestBody.setGroupId(groupId);
        uploadRequestBody.setArtifactId(artifactId);
        uploadRequestBody.setVersion(version);
        uploadRequestBody.setSpecs(allSpecs);

        List<BoatLintReport> reports = api.uploadSpec(portalKey, sourceKey, uploadRequestBody);

        try {
            File outputFile = new File(getOutput(), "radioOutput.json");
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(outputFile, reports);
            getLog().info("UPLOAD TO BOAT-BAY SUCCESSFUL, check the report: " + outputFile.getCanonicalPath());
        } catch (IOException e) {
            throw new MojoFailureException("Failed to write output", e);
        }

    }

    private UploadSpec mapToUploadSpec(SpecConfig spec) throws MojoExecutionException {

        //Validate is the spec file path is valid and unique.
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

        //Validate is the spec file is valid open-api spec
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
        UploadSpec uploadSpec = new UploadSpec();
        uploadSpec.setFileName(inputSpecFile.getName());
        uploadSpec.setKey(key);
        uploadSpec.setName(name);
        uploadSpec.setOpenApi(contents);


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

@Data
class SpecConfig {

    /**
     * Spec Key in Boat-Bay. Defaults to {@code inputSpecFile.getName().lastIndexOf("-")}.
     * For example - By default {@code my-service-api-v3.1.4.yaml} would be evaluated to {@code my-service-api}
     */
    private String key;

    /**
     * Spec Name in Boat-Bay. Defaults to filename.
     */
    private String name;

    /**
     * Location of the OpenAPI spec, as URL or local file glob pattern.
     * <p>
     * If the input is a local file, the value of this property is considered a glob pattern that must
     * resolve to a unique file.
     * </p>
     * <p>
     * The glob pattern allows to express the input specification in a version neutral way. For
     * instance, if the actual file is {@code my-service-api-v3.1.4.yaml} the expression could be
     * {@code my-service-api-v*.yaml}.
     * </p>
     */
    private String inputSpec;

}
