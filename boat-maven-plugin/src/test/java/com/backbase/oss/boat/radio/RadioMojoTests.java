package com.backbase.oss.boat.radio;

import com.backbase.oss.boat.bay.client.model.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import feign.auth.BasicAuthRequestInterceptor;
import java.io.File;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

@Slf4j
class RadioMojoTests {

    static MockWebServer mockBackEnd;

    ObjectMapper objectMapper = new ObjectMapper();

    @BeforeAll
    @SneakyThrows
    static void setUp() {
        mockBackEnd = new MockWebServer();
        mockBackEnd.start();
    }

    @Test
    @SneakyThrows
    void test_all_valid_inputs() {

        final String portalKey = "example";
        final String sourceKey = "pet-store-bom";
        final String specKey = "spec-key";
        final String specValue = "spec-name";
        final String groupId = "com.backbase.boat.samples";
        final String artifactId = "pet-store-bom";
        final String version = "2021.09";
        final String fileNameRequest = "one-client-*-v1.yaml";
        final String fileNameResolved = "one-client-api-v1.yaml";

        final BigDecimal reportId = BigDecimal.valueOf(10);
        final String reportGrade = "A";

        SpecConfig specConfig = new SpecConfig();
        specConfig.setKey(specKey);
        specConfig.setName(specValue);
        specConfig.setInputSpec(getFile("/bundler/folder/" + fileNameRequest));

        RadioMojo mojo = new RadioMojo();
        mojo.setGroupId(groupId);
        mojo.setArtifactId(artifactId);
        mojo.setVersion(version);
        mojo.setPortalKey(portalKey);
        mojo.setSourceKey(sourceKey);
        mojo.setSpecs(new SpecConfig[]{specConfig});
        mojo.setBoatBayUrl(String.format("http://localhost:%s", mockBackEnd.getPort()));

        final Dispatcher dispatcher = new Dispatcher() {
            @SneakyThrows
            @Override
            public MockResponse dispatch(RecordedRequest request) {
                switch (request.getPath()) {
                    case "/api/boat/portals/" + portalKey + "/boat-maven-plugin/" + sourceKey + "/upload":

                        UploadRequestBody requestBody = objectMapper.readValue(request.getBody().readUtf8(), UploadRequestBody.class);
                        UploadSpec uploadSpec = requestBody.getSpecs().get(0);

                        if (requestBody.getGroupId().equals(groupId) &&
                                requestBody.getArtifactId().equals(artifactId) &&
                                requestBody.getVersion().equals(version) &&
                                uploadSpec.getKey().equals(specKey) &&
                                uploadSpec.getName().equals(specValue) &&
                                uploadSpec.getFileName().equals(fileNameResolved) &&
                                uploadSpec.getOpenApi().length() > 0
                        ) {
                            log.info(uploadSpec.getOpenApi());
                            List<BoatLintReport> result = getSampleBoatLintReports(specKey, reportId, reportGrade,Changes.COMPATIBLE,Severity.HINT);
                            return new MockResponse().setResponseCode(200).setBody(objectMapper.writeValueAsString(result));
                        } else {
                            return new MockResponse().setResponseCode(400);
                        }
                }
                return new MockResponse().setResponseCode(404);
            }
        };

        mockBackEnd.setDispatcher(dispatcher);

        mojo.execute();

        File output = new File(mojo.getRadioOutput(), "radioOutput.json");

        assertTrue(output.exists());

        List<BoatLintReport> result = Arrays.asList(objectMapper.readValue(output, BoatLintReport[].class));

        assertEquals(1, result.size());
        assertEquals(reportId, result.get(0).getId());
        assertEquals(reportGrade, result.get(0).getGrade());

    }


    @Test
    @SneakyThrows
    void test_empty_specKeyAndName() {

        final String portalKey = "example";
        final String sourceKey = "pet-store-bom";
        final String groupId = "com.backbase.boat.samples";
        final String artifactId = "pet-store-bom";
        final String version = "2021.09";
        final String fileNameRequest = "one-client-*-v2.yaml";
        final String fileNameResolved = "one-client-api-v2.yaml";

        final BigDecimal reportId = BigDecimal.valueOf(20);
        final String reportGrade = "B";

        SpecConfig specConfig = new SpecConfig();
        specConfig.setInputSpec(getFile("/bundler/folder/" + fileNameRequest));

        RadioMojo mojo = new RadioMojo();
        mojo.setGroupId(groupId);
        mojo.setArtifactId(artifactId);
        mojo.setVersion(version);
        mojo.setPortalKey(portalKey);
        mojo.setSourceKey(sourceKey);
        mojo.setSpecs(new SpecConfig[]{specConfig});
        mojo.setBoatBayUrl(String.format("http://localhost:%s", mockBackEnd.getPort()));

        final Dispatcher dispatcher = new Dispatcher() {
            @SneakyThrows
            @Override
            public MockResponse dispatch(RecordedRequest request) {
                switch (request.getPath()) {
                    case "/api/boat/portals/" + portalKey + "/boat-maven-plugin/" + sourceKey + "/upload":

                        UploadRequestBody requestBody = objectMapper.readValue(request.getBody().readUtf8(), UploadRequestBody.class);
                        UploadSpec uploadSpec = requestBody.getSpecs().get(0);

                        String expectedDefaultKey  = fileNameResolved.substring(0, fileNameResolved.lastIndexOf("-"));

                        if (requestBody.getGroupId().equals(groupId) &&
                                requestBody.getArtifactId().equals(artifactId) &&
                                requestBody.getVersion().equals(version) &&
                                uploadSpec.getKey().equals(expectedDefaultKey) &&
                                uploadSpec.getName().equals(fileNameResolved) &&
                                uploadSpec.getFileName().equals(fileNameResolved) &&
                                uploadSpec.getOpenApi().length() > 0
                        ) {
                            log.info(uploadSpec.getOpenApi());
                            List<BoatLintReport> result = getSampleBoatLintReports(expectedDefaultKey, reportId, reportGrade,Changes.COMPATIBLE,Severity.HINT);
                            return new MockResponse().setResponseCode(200).setBody(objectMapper.writeValueAsString(result));
                        } else {
                            return new MockResponse().setResponseCode(400);
                        }
                }
                return new MockResponse().setResponseCode(404);
            }
        };

        mockBackEnd.setDispatcher(dispatcher);

        mojo.execute();

        File output = new File(mojo.getRadioOutput(), "radioOutput.json");

        assertTrue(output.exists());

        List<BoatLintReport> result = Arrays.asList(objectMapper.readValue(output, BoatLintReport[].class));

        assertEquals(1, result.size());
        assertEquals(reportId, result.get(0).getId());
        assertEquals(reportGrade, result.get(0).getGrade());

    }

    @Test
    void test_multiple_file_found_error() {

        final String portalKey = "example";
        final String sourceKey = "pet-store-bom";
        final String groupId = "com.backbase.boat.samples";
        final String artifactId = "pet-store-bom";
        final String version = "2021.09";
        final String fileNameRequest = "one-client-api-*.yaml";

        SpecConfig specConfig = new SpecConfig();
        specConfig.setInputSpec(getFile("/bundler/folder/" + fileNameRequest));

        RadioMojo mojo = new RadioMojo();
        mojo.setGroupId(groupId);
        mojo.setArtifactId(artifactId);
        mojo.setVersion(version);
        mojo.setPortalKey(portalKey);
        mojo.setSourceKey(sourceKey);
        mojo.setSpecs(new SpecConfig[]{specConfig});
        mojo.setBoatBayUrl(String.format("http://localhost:%s", mockBackEnd.getPort()));

        final Dispatcher dispatcher = new Dispatcher() {
            @SneakyThrows
            @Override
            public MockResponse dispatch(RecordedRequest request) {
                switch (request.getPath()) {
                    case "/api/boat/portals/" + portalKey + "/boat-maven-plugin/" + sourceKey + "/upload":

                        return new MockResponse().setResponseCode(200);
                }
                return new MockResponse().setResponseCode(404);
            }
        };

        mockBackEnd.setDispatcher(dispatcher);

        assertThrows(MojoExecutionException.class, () -> mojo.execute());

    }

    @Test
    void test_no_file_found_error() {

        final String portalKey = "example";
        final String sourceKey = "pet-store-bom";
        final String groupId = "com.backbase.boat.samples";
        final String artifactId = "pet-store-bom";
        final String version = "2021.09";
        final String invalidFileName = "invalid-client-*.yaml";

        SpecConfig specConfig = new SpecConfig();
        specConfig.setInputSpec(getFile("/bundler/folder/" + invalidFileName));

        RadioMojo mojo = new RadioMojo();
        mojo.setGroupId(groupId);
        mojo.setArtifactId(artifactId);
        mojo.setVersion(version);
        mojo.setPortalKey(portalKey);
        mojo.setSourceKey(sourceKey);
        mojo.setSpecs(new SpecConfig[]{specConfig});
        mojo.setBoatBayUrl(String.format("http://localhost:%s", mockBackEnd.getPort()));

        final Dispatcher dispatcher = new Dispatcher() {
            @SneakyThrows
            @Override
            public MockResponse dispatch(RecordedRequest request) {
                switch (request.getPath()) {
                    case "/api/boat/portals/" + portalKey + "/boat-maven-plugin/" + sourceKey + "/upload":

                        return new MockResponse().setResponseCode(200);
                }
                return new MockResponse().setResponseCode(404);
            }
        };

        mockBackEnd.setDispatcher(dispatcher);

        assertThrows(MojoExecutionException.class, () -> mojo.execute());

    }

    @Test
    void test_invalid_open_api_file() {

        final String portalKey = "example";
        final String sourceKey = "pet-store-bom";
        final String groupId = "com.backbase.boat.samples";
        final String artifactId = "pet-store-bom";
        final String version = "2021.09";
        final String invalidFile = "logback.xml";

        SpecConfig specConfig = new SpecConfig();
        specConfig.setInputSpec(getFile("/" + invalidFile));

        RadioMojo mojo = new RadioMojo();
        mojo.setGroupId(groupId);
        mojo.setArtifactId(artifactId);
        mojo.setVersion(version);
        mojo.setPortalKey(portalKey);
        mojo.setSourceKey(sourceKey);
        mojo.setSpecs(new SpecConfig[]{specConfig});
        mojo.setBoatBayUrl(String.format("http://localhost:%s", mockBackEnd.getPort()));

        final Dispatcher dispatcher = new Dispatcher() {
            @SneakyThrows
            @Override
            public MockResponse dispatch(RecordedRequest request) {
                switch (request.getPath()) {
                    case "/api/boat/portals/" + portalKey + "/boat-maven-plugin/" + sourceKey + "/upload":

                        return new MockResponse().setResponseCode(200);
                }
                return new MockResponse().setResponseCode(404);
            }
        };

        mockBackEnd.setDispatcher(dispatcher);

        assertThrows(MojoExecutionException.class, () -> mojo.execute());

    }

    @Test
    void test_invalid_parent_folder() {

        final String portalKey = "example";
        final String sourceKey = "pet-store-bom";
        final String groupId = "com.backbase.boat.samples";
        final String artifactId = "pet-store-bom";
        final String version = "2021.09";
        final String validName = "one-client-api-v1.yaml";

        SpecConfig specConfig = new SpecConfig();
        specConfig.setInputSpec(getFile("/invalid-parent/" + validName));

        RadioMojo mojo = new RadioMojo();
        mojo.setGroupId(groupId);
        mojo.setArtifactId(artifactId);
        mojo.setVersion(version);
        mojo.setPortalKey(portalKey);
        mojo.setSourceKey(sourceKey);
        mojo.setSpecs(new SpecConfig[]{specConfig});
        mojo.setBoatBayUrl(String.format("http://localhost:%s", mockBackEnd.getPort()));

        final Dispatcher dispatcher = new Dispatcher() {
            @SneakyThrows
            @Override
            public MockResponse dispatch(RecordedRequest request) {
                switch (request.getPath()) {
                    case "/api/boat/portals/" + portalKey + "/boat-maven-plugin/" + sourceKey + "/upload":

                        return new MockResponse().setResponseCode(200);
                }
                return new MockResponse().setResponseCode(404);
            }
        };

        mockBackEnd.setDispatcher(dispatcher);

        Exception exception = assertThrows(MojoExecutionException.class, () -> mojo.execute());

        assertTrue(exception.getMessage().startsWith("Invalid parent spec folder"));

    }

    @SneakyThrows
    @Test
    void test_auth() {

        final String portalKey = "example";
        final String sourceKey = "pet-store-bom";
        final String specKey = "spec-key";
        final String groupId = "com.backbase.boat.samples";
        final String artifactId = "pet-store-bom";
        final String version = "2021.09";
        final String validName = "one-client-api-v1.yaml";
        final String username = "admin";
        final String password = "admin";
        final BigDecimal reportId = BigDecimal.valueOf(10);
        final String reportGrade = "A";

        SpecConfig specConfig = new SpecConfig();
        specConfig.setInputSpec(getFile("/bundler/folder/" + validName));

        RadioMojo mojo = new RadioMojo();
        mojo.setGroupId(groupId);
        mojo.setArtifactId(artifactId);
        mojo.setVersion(version);
        mojo.setPortalKey(portalKey);
        mojo.setSourceKey(sourceKey);
        mojo.setBoatBayUsername(username);
        mojo.setBoatBayPassword(password);
        mojo.setSpecs(new SpecConfig[]{specConfig});
        mojo.setBoatBayUrl(String.format("http://localhost:%s", mockBackEnd.getPort()));

        final Dispatcher dispatcher = new Dispatcher() {
            @SneakyThrows
            @Override
            public MockResponse dispatch(RecordedRequest request) {
                switch (request.getPath()) {
                    case "/api/boat/portals/" + portalKey + "/boat-maven-plugin/" + sourceKey + "/upload":

                        if(request.getHeader("Authorization")!=null && request.getHeader("Authorization").length()>0){
                            List<BoatLintReport> result = getSampleBoatLintReports(specKey, reportId, reportGrade,Changes.COMPATIBLE,Severity.HINT);
                            return new MockResponse().setResponseCode(200).setBody(objectMapper.writeValueAsString(result));
                        }
                        return new MockResponse().setResponseCode(401);
                }
                return new MockResponse().setResponseCode(404);
            }
        };

        mockBackEnd.setDispatcher(dispatcher);

        mojo.execute();

        File output = new File(mojo.getRadioOutput(), "radioOutput.json");

        assertTrue(output.exists());

    }

    @SneakyThrows
    @Test
    void test_build_fail_on_breaking_changes() {

        final String portalKey = "example";
        final String sourceKey = "pet-store-bom";
        final String groupId = "com.backbase.boat.samples";
        final String artifactId = "pet-store-bom";
        final String specKey = "spec-key";
        final String version = "2021.09";
        final BigDecimal reportId = BigDecimal.valueOf(10);
        final String reportGrade = "A";
        final String validName = "one-client-api-v1.yaml";

        SpecConfig specConfig = new SpecConfig();
        specConfig.setInputSpec(getFile("/bundler/folder/" + validName));

        RadioMojo mojo = new RadioMojo();
        mojo.setGroupId(groupId);
        mojo.setArtifactId(artifactId);
        mojo.setVersion(version);
        mojo.setPortalKey(portalKey);
        mojo.setSourceKey(sourceKey);
        mojo.setSpecs(new SpecConfig[]{specConfig});
        mojo.setBoatBayUrl(String.format("http://localhost:%s", mockBackEnd.getPort()));

        final Dispatcher dispatcher = new Dispatcher() {
            @SneakyThrows
            @Override
            public MockResponse dispatch(RecordedRequest request) {
                switch (request.getPath()) {
                    case "/api/boat/portals/" + portalKey + "/boat-maven-plugin/" + sourceKey + "/upload":
                        List<BoatLintReport> result = getSampleBoatLintReports(specKey, reportId, reportGrade,Changes.BREAKING,Severity.HINT);
                        return new MockResponse().setResponseCode(200).setBody(objectMapper.writeValueAsString(result));
                }
                return new MockResponse().setResponseCode(404);
            }
        };

        mockBackEnd.setDispatcher(dispatcher);


        // Build will not fail if failOnBreakingChange is false  which is default.
        mojo.execute();
        File output = new File(mojo.getRadioOutput(), "radioOutput.json");
        assertTrue(output.exists());

        // Build will fail if failOnBreakingChange is true
        mojo.setFailOnBreakingChange(true);
        Exception exception = assertThrows(MojoFailureException.class, () -> mojo.execute());
        assertTrue(exception.getMessage().startsWith("Specs have Breaking Changes"));

    }

    @SneakyThrows
    @Test
    void test_build_fail_on_must_violation() {

        final String portalKey = "example";
        final String sourceKey = "pet-store-bom";
        final String groupId = "com.backbase.boat.samples";
        final String artifactId = "pet-store-bom";
        final String specKey = "spec-key";
        final String version = "2021.09";
        final BigDecimal reportId = BigDecimal.valueOf(10);
        final String reportGrade = "A";
        final String validName = "one-client-api-v1.yaml";

        SpecConfig specConfig = new SpecConfig();
        specConfig.setInputSpec(getFile("/bundler/folder/" + validName));

        RadioMojo mojo = new RadioMojo();
        mojo.setGroupId(groupId);
        mojo.setArtifactId(artifactId);
        mojo.setVersion(version);
        mojo.setPortalKey(portalKey);
        mojo.setSourceKey(sourceKey);
        mojo.setSpecs(new SpecConfig[]{specConfig});
        mojo.setBoatBayUrl(String.format("http://localhost:%s", mockBackEnd.getPort()));

        final Dispatcher dispatcher = new Dispatcher() {
            @SneakyThrows
            @Override
            public MockResponse dispatch(RecordedRequest request) {
                switch (request.getPath()) {
                    case "/api/boat/portals/" + portalKey + "/boat-maven-plugin/" + sourceKey + "/upload":
                        List<BoatLintReport> result = getSampleBoatLintReports(specKey, reportId, reportGrade,Changes.COMPATIBLE,Severity.MUST);
                        return new MockResponse().setResponseCode(200).setBody(objectMapper.writeValueAsString(result));
                }
                return new MockResponse().setResponseCode(404);
            }
        };

        mockBackEnd.setDispatcher(dispatcher);


        // Build will not fail if failOnLintViolation is false  which is default.
        mojo.execute();
        File output = new File(mojo.getRadioOutput(), "radioOutput.json");
        assertTrue(output.exists());

        // Build will fail if failOnLintViolation is true
        mojo.setFailOnLintViolation(true);
        Exception exception = assertThrows(MojoFailureException.class, () -> mojo.execute());
        assertTrue(exception.getMessage().startsWith("Specs have Must Violations"));

    }

    @SneakyThrows
    @Test
    void test_when_boat_bay_is_unavailable_and_failOnBoatBayError_is_true() {

        final String portalKey = "example";
        final String sourceKey = "pet-store-bom";
        final String groupId = "com.backbase.boat.samples";
        final String artifactId = "pet-store-bom";
        final String specKey = "spec-key";
        final String version = "2021.09";
        final BigDecimal reportId = BigDecimal.valueOf(10);
        final String reportGrade = "A";
        final String validName = "one-client-api-v1.yaml";

        SpecConfig specConfig = new SpecConfig();
        specConfig.setInputSpec(getFile("/bundler/folder/" + validName));

        RadioMojo mojo = new RadioMojo();
        mojo.setGroupId(groupId);
        mojo.setArtifactId(artifactId);
        mojo.setVersion(version);
        mojo.setPortalKey(portalKey);
        mojo.setSourceKey(sourceKey);
        mojo.setFailOnBoatBayErrorResponse(true);
        mojo.setSpecs(new SpecConfig[]{specConfig});
        //Set invalid domain
        mojo.setBoatBayUrl(String.format("http://invalid-domain:%s", mockBackEnd.getPort()));

        final Dispatcher dispatcher = new Dispatcher() {
            @SneakyThrows
            @Override
            public MockResponse dispatch(RecordedRequest request) {
                switch (request.getPath()) {
                    case "/api/boat/portals/" + portalKey + "/boat-maven-plugin/" + sourceKey + "/upload":
                        return new MockResponse().setResponseCode(200);
                }
                return new MockResponse().setResponseCode(404);
            }
        };

        mockBackEnd.setDispatcher(dispatcher);


        // Build will fail, when failOnBoatBayErrorResponse is true (Defualt)
        Exception exception = assertThrows(MojoFailureException.class, () -> mojo.execute());
        assertTrue(exception.getMessage().startsWith("BoatBay error"));

        // Build will not fail if failOnBoatBayErrorResponse is false
        mojo.setFailOnBoatBayErrorResponse(false);
        //No Exception is thrown
        assertDoesNotThrow(() -> mojo.execute());
        File output = new File(mojo.getRadioOutput(), "radioOutput.json");
        //But No output file present
        assertTrue(!output.exists());

    }


    private String getFile(String glob) {
        return (new File("src/test/resources").getAbsolutePath() + glob);
    }

    @NotNull
    private List<BoatLintReport> getSampleBoatLintReports(String expectedDefaultKey, BigDecimal reportId, String reportGrade,
                                                          Changes typeOfChange, Severity sampleSeverityInResponse) {
        BoatLintReport boatLintReport = new BoatLintReport();
        boatLintReport.setId(reportId);
        boatLintReport.setGrade(reportGrade);
        boatLintReport.violations(List.of(BoatViolation.builder().severity(sampleSeverityInResponse).build()));
        boatLintReport.setSpec(BoatSpec.builder().key(expectedDefaultKey).changes(typeOfChange).build());
        List<BoatLintReport> result = new ArrayList<>();
        result.add(boatLintReport);
        return result;
    }

}