package com.backbase.oss.boat;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.backbase.oss.codegen.asyncapi.BoatSpringCloudStreamCodegen;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.io.FileUtils;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.logging.console.ConsoleLogger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.sonatype.plexus.build.incremental.DefaultBuildContext;

class AsyncApiMojoTests {

    private static final File SHARED_EVENTS_DIR =
        new File("../boat-trail-resources/src/main/resources/asyncapi/shared-events");
    private static final File SHARED_EVENTS = new File(SHARED_EVENTS_DIR, "asyncapi.yaml");
    private static final File CLOUD_EVENT =
        new File("../boat-trail-resources/src/main/resources/asyncapi/cloud-event/events/adoption-decision-v1/asyncapi.yaml");

    private final DefaultBuildContext buildContext = new DefaultBuildContext();

    @TempDir
    Path dir;

    @BeforeEach
    void setUp() {
        buildContext.enableLogging(new ConsoleLogger());
    }

    @Test
    void validatesDirectoryWithOpenApiAndAsyncApi() throws IOException {
        File input = copySharedEvents();

        ValidateMojo mojo = new ValidateMojo();
        mojo.setInput(input);
        mojo.setFailOnWarning(true);

        assertDoesNotThrow(mojo::execute);
    }

    @Test
    void failsOnInvalidAsyncApi() throws IOException {
        File input = dir.resolve("asyncapi.yaml").toFile();
        Files.write(input.toPath(), "asyncapi: 3.0.0\ninfo:\n  title: Missing version\n".getBytes(StandardCharsets.UTF_8));

        ValidateMojo mojo = new ValidateMojo();
        mojo.setInput(input);
        mojo.setFailOnWarning(true);

        assertThrows(MojoFailureException.class, mojo::execute);
    }

    @Test
    void lintSkipsAsyncApi() throws Exception {
        File input = copySharedEvents();

        LintMojo asyncApi = new LintMojo();
        asyncApi.setInput(new File(input, "asyncapi.yaml"));
        asyncApi.setFailOnWarning(true);
        asyncApi.output = dir.resolve("lint").toFile();
        assertDoesNotThrow(asyncApi::execute);

        LintMojo directory = new LintMojo();
        directory.setInput(input);
        directory.setWriteLintReport(true);
        directory.output = dir.resolve("lint-directory").toFile();
        assertDoesNotThrow(directory::execute);
        try (Stream<Path> reports = Files.walk(directory.output.toPath())) {
            List<String> paths = reports.map(Path::toString).collect(Collectors.toList());
            assertThat(paths, hasItem(containsString("index.html")));
            assertThat(paths, not(hasItem(containsString("asyncapi"))));
        }
    }

    @Test
    void bundlesAsyncApi() throws Exception {
        File input = copySharedEvents();
        File output = dir.resolve("bundled").toFile();

        BundleMojo mojo = new BundleMojo();
        mojo.setInput(input);
        mojo.setOutput(output);
        mojo.setIncludes(new String[]{"*.yaml"});
        mojo.setExcludes(new String[]{"lib/**", "messages/**"});
        mojo.execute();

        String asyncApi = read(new File(output, "asyncapi.yaml"));
        assertThat(asyncApi, containsString("asyncapi: 3.0.0"));
        assertThat(asyncApi, containsString("PaymentSettledPayload:"));
        assertThat(asyncApi, not(containsString("payment-settled.yaml")));
        assertThat(read(new File(output, "openapi.yaml")), containsString("openapi: 3.0.3"));
    }

    @Test
    void bundlesAsyncApiOntoTheProjectionOpenApiVersion() throws Exception {
        BundleMojo openApi31 = new BundleMojo();
        openApi31.setInput(CLOUD_EVENT);
        openApi31.setOutput(dir.resolve("openapi-31.yaml").toFile());
        openApi31.setProjectionOpenApiVersion("3.1.0");
        openApi31.execute();

        BundleMojo openApi30 = new BundleMojo();
        openApi30.setInput(CLOUD_EVENT);
        openApi30.setOutput(dir.resolve("openapi-30.yaml").toFile());
        openApi30.execute();

        assertThat(read(openApi31.getOutput()), containsString("const:"));
        assertThat(read(openApi30.getOutput()), not(containsString("const:")));
    }

    @Test
    void generatesModelsOnlyWithOtherGenerators() throws Exception {
        File output = dir.resolve("spring").toFile();
        File classes = dir.resolve("classes").toFile();

        GenerateMojo mojo = configure(new GenerateMojo(), output);
        mojo.generatorName = "spring";
        mojo.generateApis = true;
        mojo.copyTo = new File(classes, "META-INF/openapi/openapi.yaml");
        mojo.execute();

        assertFalse(mojo.generateApis);
        assertTrue(new File(classes, "META-INF/asyncapi/asyncapi.yaml").isFile());
        assertFalse(new File(classes, "META-INF/openapi/openapi.yaml").exists());
        assertTrue(new File(output, ".boat/asyncapi-projected.yaml").isFile());

        List<String> sources = javaFiles(output);
        assertThat(sources, hasItem("Order.java"));
        assertThat(sources, hasItem("OrderPlacedHeaders.java"));
        assertThat(sources, hasItem("PaymentSettledPayload.java"));
        assertThat(sources.stream().filter(name -> name.endsWith("Api.java")).collect(Collectors.toList()), empty());
        assertThat(sources, hasItem("BigDecimalCustomSerializer.java"));
        assertThat(sources, not(hasItem("HomeController.java")));
        assertThat(sources, not(hasItem("OpenApiGeneratorApplication.java")));
        assertFalse(new File(output, "src/main/resources/openapi.yaml").exists());
    }

    @Test
    void generatesSpringCloudStreamEmbedded() throws Exception {
        File output = dir.resolve("scs").toFile();

        GenerateMojo mojo = configure(new GenerateSpringCloudStreamEmbeddedMojo(), output);
        mojo.apiPackage = "com.example.events";
        mojo.modelPackage = "com.example.model";
        mojo.execute();

        assertThat(mojo.generatorName, equalTo(BoatSpringCloudStreamCodegen.NAME));
        List<String> sources = javaFiles(output);
        assertThat(sources, hasItem("OrdersHandler.java"));
        assertThat(sources, hasItem("OrdersFunctionsConfiguration.java"));
        assertThat(sources, hasItem("OrdersPublisher.java"));
        assertThat(sources, hasItem("AsyncApiHeaders.java"));
        assertThat(sources, hasItem("Order.java"));
        assertTrue(new File(output, "src/main/resources/asyncapi-bindings.properties").isFile());
        assertFalse(new File(output, "pom.xml").exists());
    }

    @Test
    void asyncApiCopyToKeepsCustomLocations() {
        File custom = new File("target/custom/events.yaml");
        assertEquals(custom, GenerateMojo.asyncApiCopyTo(custom));
        assertEquals(new File("target/classes/META-INF/asyncapi/asyncapi.yaml"),
            GenerateMojo.asyncApiCopyTo(new File("target/classes/META-INF/openapi/openapi.yaml")));
    }

    private <T extends GenerateMojo> T configure(T mojo, File output) {
        mojo.buildContext = buildContext;
        mojo.project = new MavenProject();
        mojo.inputSpec = SHARED_EVENTS.getAbsolutePath();
        mojo.output = output;
        mojo.copyTo = null;
        return mojo;
    }

    private File copySharedEvents() throws IOException {
        File copy = dir.resolve("shared-events").toFile();
        FileUtils.copyDirectory(SHARED_EVENTS_DIR, copy);
        return copy;
    }

    private static List<String> javaFiles(File output) throws IOException {
        try (Stream<Path> files = Files.walk(output.toPath())) {
            return files.map(path -> path.getFileName().toString())
                .filter(name -> name.endsWith(".java"))
                .collect(Collectors.toList());
        }
    }

    private static String read(File file) throws IOException {
        return new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
    }
}
