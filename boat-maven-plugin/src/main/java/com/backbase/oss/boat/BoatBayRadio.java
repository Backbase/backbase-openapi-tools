package com.backbase.oss.boat;

import com.backbase.oss.boat.bay.client.ApiClient;
import com.backbase.oss.boat.bay.client.api.UploadPluginApi;
import com.backbase.oss.boat.bay.client.model.*;
import com.backbase.oss.boat.loader.OpenAPILoader;
import io.swagger.v3.oas.models.OpenAPI;
import kotlin.ranges.IntRange;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.jetbrains.annotations.NotNull;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.zalando.zally.rule.api.Severity;
import org.zalando.zally.rule.api.Violation;

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
  private final File output;
  private String clientBasePath;
  protected MavenProject project;


  public BoatBayRadio(File inputSpec, File output, MavenProject project, String clientBasePath) {
    this.project = project;
    this.output = output;
    this.inputSpec = inputSpec;
    this.clientBasePath = clientBasePath;

    if (System.getenv().containsKey("BOAT_BAY_SERVER_URL"))
      this.clientBasePath= System.getenv("BOAT_BAY_SERVER_URL");

    boatbayUploadSpecClient = configureApiClient(this.clientBasePath).buildClient(UploadPluginApi.class);
  }

  private ApiClient configureApiClient(String clientBasePath){

    if (readTimeout == null)
      return new ApiClient().setBasePath(clientBasePath);

    return new ApiClient().setBasePath(clientBasePath);
  }


  public List<com.backbase.oss.boat.quay.model.BoatLintReport> upload(String sourceId )  {
    log.debug("uploading specs for source : {}" , sourceId);

    List<BoatLintReport> boatLintReports = boatbayUploadSpecClient.uploadSpec(sourceId, createRequestBody());

    log.info("\nSpecs linted.Lint reports can be found at:");

    List<com.backbase.oss.boat.quay.model.BoatLintReport> lintReports= boatLintReports.stream()
            .map(this::mapBoatBayLintToBoatLint)
            .collect(Collectors.toList());

    return lintReports;
  }


  private com.backbase.oss.boat.quay.model.BoatLintReport mapBoatBayLintToBoatLint(BoatLintReport bayLintReport){
    com.backbase.oss.boat.quay.model.BoatLintReport lintReport = new com.backbase.oss.boat.quay.model.BoatLintReport();

    lintReport.setOpenApi(bayLintReport.getOpenApi());
    lintReport.setVersion(bayLintReport.getVersion());
    lintReport.setTitle(bayLintReport.getName());
    lintReport.setAvailableRules(bayLintReport.getViolations().stream().map(BoatViolation::getRule).map(this::mapRule).collect(Collectors.toList()));
    lintReport.setViolations(bayLintReport.getViolations().stream().map(this::mapViolation).collect(Collectors.toList()));
    lintReport.setFilePath(getFilePath(bayLintReport.getSpec().getName()));

    //link to lint report may need updating before release
    log.info("\n\tSpec {}:     {}/lint-report/{}/view",
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


  private com.backbase.oss.boat.quay.model.BoatLintRule mapRule(BoatLintRule boatRule){
    com.backbase.oss.boat.quay.model.BoatLintRule boatLintRule = new com.backbase.oss.boat.quay.model.BoatLintRule();

    boatLintRule.setTitle(boatRule.getTitle());
    boatLintRule.setRuleSet(boatRule.getRuleSet());
    boatLintRule.setIgnored(!boatRule.getEnabled());
    boatLintRule.setUrl(boatRule.getUrl());
    boatLintRule.setSeverity(mapSeverity(boatRule.getSeverity()));

    return boatLintRule;
  }


  private com.backbase.oss.boat.quay.model.BoatViolation mapViolation(BoatViolation boatViolation){
    com.backbase.oss.boat.quay.model.BoatViolation violation = new com.backbase.oss.boat.quay.model.BoatViolation();

    violation.setDescription(boatViolation.getDescription());
    violation.setRule(mapRule(boatViolation.getRule()));
    violation.setSeverity(mapSeverity(boatViolation.getSeverity()));
    violation.setLines(mapRange(boatViolation.getLines()));
    violation.setPointer(boatViolation.getPointer());

    return violation;
  }

  private IntRange mapRange(com.backbase.oss.boat.bay.client.model.IntRange range){

    if (range.getEndInclusive()==null){
      return new IntRange(range.getStart(),range.getStart());
    }
    return new IntRange(range.getStart(),range.getEndInclusive());
  }

  private Severity mapSeverity(com.backbase.oss.boat.bay.client.model.Severity boatSeverity){
    return Severity.valueOf(boatSeverity.getValue());
  }


  private UploadRequestBody createRequestBody(){
    UploadRequestBody requestBody = new UploadRequestBody();

    try {
      requestBody.setSpecs(getSpecs());
    }catch (Exception e){
      throw new RuntimeException("unable to load specs", e);
    }

    requestBody.location(output.getParentFile().getPath());

    if (output == null)
      requestBody.location(new File("./target/boat-bay-lint").getAbsolutePath());

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

    uploadSpec.openApi(contents);

    log.debug("uploading api {} with contents {}", spec.getName(),openAPI.getOpenapi());

    uploadSpec.key(getSpecKey(spec));
    uploadSpec.name(spec.getName());

    return uploadSpec;
  }

  private String getSpecKey(File spec){
    String key;
    String spEL;
    ExpressionParser parser = new SpelExpressionParser();

    if(spec.getName().contains("-")){
      spEL = "name.substring(0,name.lastIndexOf('-'))";
    }else
      spEL = "name.substring(0,name.lastIndexOf('.'))";

    Expression value = parser.parseExpression(spEL);
    key = value.getValue(spec, String.class);

    return key;
  }

}
