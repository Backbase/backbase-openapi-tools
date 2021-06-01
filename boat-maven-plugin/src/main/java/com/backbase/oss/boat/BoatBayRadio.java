package com.backbase.oss.boat;

import com.backbase.oss.boat.bay.client.ApiClient;
import com.backbase.oss.boat.bay.client.ApiException;
import com.backbase.oss.boat.bay.client.api.UploadPluginApi;
import com.backbase.oss.boat.bay.client.model.BoatLintReport;
import com.backbase.oss.boat.bay.client.model.UploadRequestBody;
import com.backbase.oss.boat.bay.client.model.UploadSpec;
import com.backbase.oss.boat.loader.OpenAPILoader;
import io.swagger.v3.oas.models.OpenAPI;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import java.io.File;
import java.nio.charset.Charset;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
public class BoatBayRadio {

  @Parameter(name = "readTimeout", defaultValue = "null")
  public Duration readTimeout;

  private UploadPluginApi boatbayUploadSpecClient;
  private File inputSpec;
  private File output;
  protected MavenProject project;

  public BoatBayRadio(File inputSpec, File output, MavenProject project) {
    this.project = project;
    this.output = output;
    this.inputSpec = inputSpec;
    this.boatbayUploadSpecClient =  new UploadPluginApi(new ApiClient().setReadTimeout(readTimeout));;
  }


  public List<BoatLintReport> upload(String sourceId ) throws ApiException {

    if (sourceId.isEmpty()){
      throw new RuntimeException("Source must be set up first");
    }
    try {
      return boatbayUploadSpecClient.uploadSpec(sourceId, createRequestBody());
    } catch (Exception e) {
      throw new ApiException(e.getMessage());
    }

  }

  private UploadRequestBody createRequestBody(){
    UploadRequestBody requestBody = new UploadRequestBody();
    try {
      requestBody.setSpecs(getSpecs());
    }catch (Exception e){
      throw new RuntimeException("unable to load specs", e);
    }

    requestBody.location(project.getBasedir().toPath().resolve(inputSpec.toPath()).toString());
    if (output != null){
      requestBody.location(output.getParentFile().getPath());
    }

    requestBody.projectId(project.getGroupId());
    requestBody.setArtifactId(project.getArtifactId());
    requestBody.version(project.getVersion());

    return requestBody;
  }

  private List<UploadSpec> getSpecs() throws MojoExecutionException {
    File[] inputFiles;
    if (inputSpec.isDirectory()) {
      inputFiles = inputSpec.listFiles(pathname -> pathname.getName().endsWith(".yaml"));
      if (inputFiles == null || inputFiles.length == 0) {
        throw new MojoExecutionException("No OpenAPI specs found in: " + inputSpec);
      }
      log.info("Found " + inputFiles.length + " specs to lint.");
    } else {
      inputFiles = new File[]{inputSpec};
    }

    List<UploadSpec> specs = Arrays.stream(inputFiles).map(this::mapToUploadSpec).collect(Collectors.toList());
    return specs;
  }

  private UploadSpec mapToUploadSpec(File spec) {
    OpenAPI openAPI;
    try {
      String contents = IOUtils.toString(spec.toURI(), Charset.defaultCharset());
      openAPI= OpenAPILoader.parse(contents);
    }catch (Exception e){
      throw new RuntimeException("unable to read api files: "+e.getMessage());
    }

    UploadSpec uploadSpec = new UploadSpec();
    uploadSpec.fileName(spec.getName());
    uploadSpec.openApi(openAPI.getOpenapi());
    uploadSpec.key(spec.getName().substring(0, spec.getName().lastIndexOf('-')));
    uploadSpec.name(spec.getName());
    return uploadSpec;
  }

}
