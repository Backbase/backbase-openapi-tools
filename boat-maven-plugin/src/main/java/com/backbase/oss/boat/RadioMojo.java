package com.backbase.oss.boat;

import com.backbase.oss.boat.bay.client.ApiClient;
import com.backbase.oss.boat.bay.client.api.BoatMavenPluginApi;
import com.backbase.oss.boat.bay.client.model.BoatLintReport;
import com.backbase.oss.boat.bay.client.model.UploadRequestBody;
import com.backbase.oss.boat.bay.client.model.UploadSpec;
import com.backbase.oss.boat.loader.OpenAPILoader;
import com.backbase.oss.boat.loader.OpenAPILoaderException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationConfig;
import feign.auth.BasicAuthRequestInterceptor;
import io.swagger.v3.oas.models.OpenAPI;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;

@Mojo(name = "radio", requiresDependencyResolution = ResolutionScope.RUNTIME, threadSafe = true)
@Slf4j
@Getter
@Setter
public class RadioMojo extends AbstractMojo {

    @Parameter(property = "groupId", defaultValue = "${project.groupId}", required = true)
    private String groupId;

    @Parameter(property = "artifactId", defaultValue = "${project.artifactId}", required = true)
    private String artifactId;

    @Parameter(property = "version", defaultValue = "${project.version}", required = true)
    private String version;

    @Parameter(property = "basePath", required = true)
    private String basePath;

    @Parameter(property = "portalKey", required = true)
    private String portalKey;

    @Parameter(property = "sourceKey", required = true)
    private String sourceKey;

    @Parameter(property = "username", required = false)
    private String username;

    @Parameter(property = "password", required = false)
    private String password;

    @Parameter(property = "specs")
    private SpecConfig[] specs;

    @Parameter(name = "radioOutput", defaultValue = "${project.build.directory}/target/boat-radio-report")
    private File radioOutput;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {

        BasicAuthRequestInterceptor basicAuthRequestInterceptor = null;

        ObjectMapper objectMapper = new ObjectMapper();

        if(username != null && !username.isEmpty() && password != null && !password.isEmpty()){
             basicAuthRequestInterceptor = new BasicAuthRequestInterceptor(username, password);
        }else{
            getLog().warn("No Authentication set");
        }

        List<UploadSpec> allSpecs = Arrays.stream(specs).map(this::mapToUploadSpec).collect(Collectors.toList());

        ApiClient apiClient = new ApiClient().setBasePath(basePath);
        if(basicAuthRequestInterceptor != null){
            apiClient.addAuthorization("Basic Auth", basicAuthRequestInterceptor);
        }

        BoatMavenPluginApi api = apiClient.buildClient(BoatMavenPluginApi.class);

        UploadRequestBody uploadRequestBody = new UploadRequestBody();
        uploadRequestBody.setGroupId(groupId);
        uploadRequestBody.setArtifactId(artifactId);
        uploadRequestBody.setVersion(version);
        uploadRequestBody.setSpecs(allSpecs);

        List<BoatLintReport> reports = api.uploadSpec(portalKey,sourceKey,uploadRequestBody);

        try {
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(new File(getOutput(), "radioOutput.json"),reports);
        } catch (IOException e) {
            throw new MojoFailureException("Failed to write output", e);
        }

    }

    private UploadSpec mapToUploadSpec(SpecConfig spec) {

        OpenAPI openApi = null;
        String contents = null;
        File file = spec.getInputSpec();
        try {
            contents = IOUtils.toString(spec.getInputSpec().toURI(), Charset.defaultCharset());
            openApi = OpenAPILoader.parse(contents);
        } catch ( IOException e) {
            String msg = "Invalid File Path: " + file.getName();
            getLog().error(msg);
            throw new RuntimeException(msg, e);
        } catch (OpenAPILoaderException e) {
            String msg = "Invalid Open Api file: " + file.getName();
            getLog().error(msg);
            throw new RuntimeException(msg, e);
        }

        String key = spec.getKey();
        if (key == null || key.isEmpty()) {
            key = file.getName().substring(0, file.getName().lastIndexOf("-"));
        }

        String name = spec.getName();
        if (name == null || name.isEmpty()) {
            name = file.getName();
        }

        UploadSpec uploadSpec = new UploadSpec();
        uploadSpec.setFileName(file.getName());
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
        if(!radioOutput.exists()){
            radioOutput.mkdir();
        }
        return radioOutput;
    }


}

@Data
class SpecConfig {

    private String key;

    private String name;

    private File inputSpec;

}
