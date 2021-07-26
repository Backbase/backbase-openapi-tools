package com.backbase.oss.boat;

import com.backbase.oss.boat.bay.client.ApiClient;
import com.backbase.oss.boat.bay.client.api.UploadPluginApi;
import com.backbase.oss.boat.bay.client.model.*;
import com.backbase.oss.boat.loader.OpenAPILoader;
import com.backbase.oss.boat.mapper.LintReportMapperImpl;
import com.fasterxml.jackson.databind.introspect.TypeResolutionContext.Basic;
import feign.auth.BasicAuthRequestInterceptor;
import io.swagger.v3.oas.models.OpenAPI;
import kotlin.ranges.IntRange;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.zalando.zally.rule.api.Severity;

import java.io.File;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
public class BoatBayRadio {

  @Parameter(name = "readTimeout", defaultValue = "null")
  public Integer readTimeout;

  private final UploadPluginApi boatbayUploadSpecClient;
  private final File inputSpec;
  private String clientBasePath;
  private LintReportMapperImpl reportMapper;
  protected MavenProject project;


  public BoatBayRadio(File inputSpec, MavenProject project, String clientBasePath) {
    this.project = project;
    this.reportMapper = new LintReportMapperImpl();
    this.inputSpec = inputSpec;
    this.clientBasePath = clientBasePath;

    if (System.getenv().containsKey("BOAT_BAY_SERVER_URL"))
      this.clientBasePath= System.getenv("BOAT_BAY_SERVER_URL");

    boatbayUploadSpecClient = configureApiClient(this.clientBasePath).buildClient(UploadPluginApi.class);
  }

  private ApiClient configureApiClient(String clientBasePath){

    if (readTimeout == null){
      BasicAuthRequestInterceptor basicAuthRequestInterceptor = new BasicAuthRequestInterceptor("admin","admin");
      ApiClient apiClient = new ApiClient().setBasePath(clientBasePath);
      apiClient.addAuthorization("Basic",basicAuthRequestInterceptor);
      return apiClient;
    }

    return new ApiClient().setBasePath(clientBasePath);
  }

  public List<com.backbase.oss.boat.quay.model.BoatLintReport> upload(String sourceKey )  {
    log.debug("uploading specs for source : {}" , sourceKey);


    List<BoatLintReport> boatLintReports = boatbayUploadSpecClient.uploadSpec(sourceKey, createRequestBody());

    log.info("\nSpecs linted.Lint reports can be found at:");

    List<com.backbase.oss.boat.quay.model.BoatLintReport> lintReports= boatLintReports.stream()
            .map(this::mapBoatBayLintToBoatLint)
            .collect(Collectors.toList());

    return lintReports;
  }


  private com.backbase.oss.boat.quay.model.BoatLintReport mapBoatBayLintToBoatLint(BoatLintReport bayLintReport){

    com.backbase.oss.boat.quay.model.BoatLintReport lintReport = reportMapper.bayReportToBoatReport(bayLintReport);
    lintReport.setFilePath(getFilePath(lintReport.getFilePath()));

    //link to lint report may need updating before release should perhaps
    //this link structure is for testing
    //be something like this:
    // https://boat-bay.proto.backbasecloud.com/lint-reports/repo/digital-banking/lint-report/167
    log.info("\n\tSpec {}:     {}lint-report/{}/view",
            bayLintReport.getSpec().getName(),
            clientBasePath,
            bayLintReport.getId());

    return lintReport;
  }

  private String getFilePath(String fileName) {
    File inputFile = inputSpec.toPath().resolve(fileName).toFile();
    File workingDirectory = new File(".");
    Path relativize;

    try {
      relativize = workingDirectory.toPath().relativize(inputFile.toPath());
    } catch (RuntimeException exception) {
      log.warn("Failed to get relative path for: {} in working directory: {}", inputFile, workingDirectory);
      return inputFile.toString();
    }

    return relativize.toString();
  }


  private UploadRequestBody createRequestBody(){
    UploadRequestBody requestBody = new UploadRequestBody();

    try {
      requestBody.setSpecs(getSpecs());
    }catch (Exception e){
      throw new RuntimeException("unable to load specs", e);
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

      if (inputFiles == null || inputFiles.length == 0)
        throw new MojoExecutionException("No OpenAPI specs found in: " + inputSpec);


      log.info("Found " + inputFiles.length + " specs to lint.");
    } else {
      inputFiles = new File[]{inputSpec};
    }

    return Arrays.stream(inputFiles).map(this::mapToUploadSpec).collect(Collectors.toList());
  }

  private UploadSpec mapToUploadSpec(File spec) {
    OpenAPI openAPI;
    String contents;

    try {

      contents = IOUtils.toString(spec.toURI(), Charset.defaultCharset());
      openAPI= OpenAPILoader.parse(contents);

    }catch (Exception e){
      throw new RuntimeException("unable to read api files: "+e.getMessage());
    }

    UploadSpec uploadSpec = new UploadSpec();
    uploadSpec.fileName(spec.getName());
    uploadSpec.version(openAPI.getInfo().getVersion());
    uploadSpec.openApi(contents);

    log.debug("uploading api {} with contents {}", spec.getName(),openAPI.getOpenapi());

    uploadSpec.name(spec.getName());

    return uploadSpec;
  }


}
