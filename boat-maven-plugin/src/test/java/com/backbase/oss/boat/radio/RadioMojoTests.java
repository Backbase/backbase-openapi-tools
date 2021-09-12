package com.backbase.oss.boat.radio;

import com.backbase.oss.boat.bay.client.model.*;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
                            BoatLintReport boatLintReport = new BoatLintReport();
                            boatLintReport.setId(reportId);
                            boatLintReport.setGrade(reportGrade);
                            boatLintReport.setSpec(BoatSpec.builder().key(specKey).changes(Changes.COMPATIBLE).build());
                            List<BoatLintReport> result = new ArrayList<>();
                            result.add(boatLintReport);
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

        assertTrue(result.size() == 1);
        assertTrue(result.get(0).getId().equals(reportId));
        assertTrue(result.get(0).getGrade().equals(reportGrade));

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
                            BoatLintReport boatLintReport = new BoatLintReport();
                            boatLintReport.setId(reportId);
                            boatLintReport.setGrade(reportGrade);
                            boatLintReport.setSpec(BoatSpec.builder().key(expectedDefaultKey).changes(Changes.COMPATIBLE).build());
                            List<BoatLintReport> result = new ArrayList<>();
                            result.add(boatLintReport);
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

        assertTrue(result.size() == 1);
        assertTrue(result.get(0).getId().equals(reportId));
        assertTrue(result.get(0).getGrade().equals(reportGrade));

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


    private String getFile(String glob) {
        return (new File("src/test/resources").getAbsolutePath() + glob);
    }

}