package com.backbase.oss.boat;

import com.backbase.oss.boat.bay.client.ApiException;
import com.backbase.oss.boat.bay.client.api.UploadPluginApi;
import com.backbase.oss.boat.loader.OpenAPILoader;
import com.backbase.oss.boat.loader.OpenAPILoaderException;
import com.backbase.oss.boat.quay.model.BoatLintReport;
import com.backbase.oss.codegen.lint.BoatLintConfig;
import com.backbase.oss.codegen.lint.BoatLintGenerator;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;

@SuppressWarnings("FieldMayBeFinal")
@Mojo(name = "lint", requiresDependencyResolution = ResolutionScope.RUNTIME, threadSafe = true)
@Slf4j
/**
 * API lint which provides checks for compliance with many of Backbase's API standards.
 */
public class LintMojo extends AbstractLintMojo {

    /**
     * Output directory for lint reports.
     */
    @Parameter(name = "output", defaultValue = "${project.build.directory}/boat-lint-reports")
    public File output;

    /**
     * Set this to <code>true</code> to generate lint report.
     */
    @Parameter(name = "writeLintReport", defaultValue = "true")
    private boolean writeLintReport;

    @Parameter(readonly = true, required = true, defaultValue = "${project}")
    protected MavenProject project;

    @Parameter(name = "sourceId")
    private String sourceId;

    private UploadLint boatBaySpecUpload;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        boolean boatbay = false;



        List<BoatLintReport> boatLintReports = null;
        try {

//            if(boatbay){
//                new UploadLint(inputSpec,output,project).upload(sourceId);
//            }
            boatLintReports = lint();
        } catch (MojoExecutionException e) {
            if (failOnWarning) {
                throw e;
            }
        }
        if (boatLintReports == null) {
            log.warn("No reports generated for input: {}", inputSpec);
            return;
        }
        boolean isSingleLint = boatLintReports.size() == 1;
        boolean isFailed = false;
        for (BoatLintReport report : boatLintReports) {
            log.info("Lint report for: {}", report.getTitle());

            if (!report.hasViolations()) {
                log.info("OpenAPI: {}, is valid! No warnings!", report.getFilePath());
            } else {
                isFailed = true;
                log.warn("OpenAPI: {} has Linting issues: ", report.getFilePath());
                report.getViolations().forEach(result -> log.warn("{}", result.displayString()));
            }

            if (writeLintReport) {
                generateLintReport(isSingleLint, report);
            }
        }
        if (isFailed && failOnWarning) {
            throw new MojoFailureException("Linting " + inputSpec + " failed. Please correct the found issues and try again");
        }

    }

    private void generateLintReport(boolean isSingleLint, BoatLintReport report) {
        BoatLintConfig config = new BoatLintConfig();
        File reportOutputDir = getOutput();
        config.setOutputDir(reportOutputDir.toString());
        if (!isSingleLint) {

            String lintReportOutput = new File(reportOutputDir, report.getFileName()).toString();
            log.info("Writing LINT Report for: {} to: {}", report.getTitle(), lintReportOutput);
            config.setOutputDir(lintReportOutput);
        }
        BoatLintGenerator boatLintGenerator = new BoatLintGenerator(config);
        boatLintGenerator.generate(report);
    }

    private File getOutput() {
        if (this.output == null) {
            output = new File("./target/boat-lint");
        }
        return this.output;
    }

//    private List<BoatLintReport> uploadAndLint() throws IOException, OpenAPILoaderException {
//        OkHttpClient client = new OkHttpClient();
//
//        Request request = new Request.Builder()
//                .url(new URL("http://localhost:8080/api/boat-maven-plugin/"+sourceId+"/upload"))
//                .put(createRequest())
//                .build();
//
//
//        Response response = client.newCall(request).execute();
//        log.debug("response {}", response.body().toString() );
//        ObjectMapper objectMapper = new ObjectMapper();
//        List<BoatLintReport> reports = objectMapper.readValue( response.body().toString(), List.class);
//        return reports;
//    }


//        URL client = new URL("http://localhost:8080/api/boat-maven-plugin/"+sourceId+"/upload");
//
//        HttpURLConnection connection = (HttpURLConnection) client.openConnection();
//        connection.setDoOutput(true);
//        connection.setRequestMethod(PUT);
//        connection.setRequestProperty("Authorization", "Basic YWRtaW46YWRtaW4=");
//        connection.setRequestProperty("Content-Type", "application/json");
//        OutputStreamWriter out = new OutputStreamWriter(connection.getOutputStream());
//        out.write(createRequest());
//        out.close();
//        if (connection.getResponseCode() != HttpURLConnection.HTTP_OK){
//            throw new HttpResponseException(connection.getResponseCode(),"Unsuccessful lint request");
//        }
//        HttpEntity<List<BoatLintReport>> responseBody;
//        InputStreamReader response = new InputStreamReader(connection.getInputStream());
////        String responseEntity = new BufferedReader(response).lines().collect(Collectors.joining("\n"));
////        ResponseEntity reports = new ObjectMapper().readValue(responseEntity, ResponseEntity.class);
////        Object body = reports.getBody();
////        if (body.getClass().isInstance(List.class)){
////            List list = (List) body;
////            if (list.get(0).getClass().isInstance())
////        }
//        connection.getResponseCode()
//
//        return new ObjectMapper().readValue(response,List.class);
//
//
//
//
//    }
//
//    private RequestBody createRequest() throws OpenAPILoaderException {
//        MediaType JSON = MediaType.parse("application/json; charset=utf-8");
//        String request = String.format("{\"specs\": [ {" +
//                        "\"key\": \"%s\" , " +
//                        "\"name\" : \"%s\", " +
//                        "\"openApi\" : \"%s\", " +
//                        "\"filename\": \"%s\", " +
//                        "} ], " +
//                        "\"location\": \"%s\", " +
//                        "\"projectId\": \"%s\"," +
//                        "\"artifactId\": \"%s\"," +
//                        "\"version\": \"%s\"}",
//                inputSpec.getName().substring(0, inputSpec.getName().lastIndexOf('-')),
//                inputSpec.getName(),
//                OpenAPILoader.load(inputSpec).getOpenapi(),
//                inputSpec.getName(),
//                output.getParentFile().getPath(),
//                project.getGroupId(),
//                project.getArtifactId() ,
//                project.getVersion()
//        );
//
//        return RequestBody.create(JSON, request);
//    }



    public void setWriteLintReport(boolean writeLintReport) {
        this.writeLintReport = writeLintReport;
    }
}
