package com.backbase.oss.boat;

import com.backbase.oss.boat.serializer.SerializerUtils;
import com.backbase.oss.boat.transformers.AdditionalPropertiesAdder;
import com.backbase.oss.boat.transformers.Decomposer;
import com.backbase.oss.boat.transformers.Deprecator;
import com.backbase.oss.boat.transformers.LicenseAdder;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.maven.model.Dependency;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.Expand;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.impl.ArtifactResolver;
import org.eclipse.aether.impl.MetadataResolver;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.resolution.ArtifactRequest;
import org.eclipse.aether.resolution.ArtifactResolutionException;
import org.eclipse.aether.resolution.ArtifactResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

abstract class AbstractRamlToOpenApi extends AbstractMojo {

    protected final Map<File, OpenAPI> success = new LinkedHashMap<>();
    protected final Map<String, String> failed = new LinkedHashMap<>();

    private static Logger log = LoggerFactory.getLogger(AbstractRamlToOpenApi.class);

    @Parameter(defaultValue = "${project}", readonly = true)
    protected MavenProject project;

    @Parameter(property = "includeGroupId", defaultValue = "com.backbase.")
    protected String includeGroupIds;

    @Parameter(property = "xLogoUrl")
    protected String xLogoUrl;

    @Parameter(property = "xLogoAltText")
    protected String xLogoAltText;

    @Parameter(property = "licenseName")
    protected String licenseName;

    @Parameter(property = "licenseUrl")
    protected String licenseUrl;

    @Parameter(property = "markdown")
    protected String markdownTop;

    @Parameter(property = "markdownBottom")
    protected String markdownBottom;

    @Parameter(property = "servers")
    protected List<Server> servers = Collections.emptyList();

    @Parameter(property = "convertJsonExamplesToYaml", defaultValue = "true")
    protected boolean convertJsonExamplesToYaml;

    @Parameter(property = "addJavaTypeExtensions", defaultValue = "true")
    protected boolean addJavaTypeExtensions;

    @Parameter(property = "appendDeprecatedMetadataInDescription", defaultValue = "true")
    protected boolean appendDeprecatedMetadataInDescription = true;

    @Parameter(property = "decompose", defaultValue = "true")
    protected boolean decompose;

    @Parameter(name = "removeDeprecated", defaultValue = "false")
    protected boolean removeDeprecated;

    @Parameter(property = "includeVersionInOutputDirectory", defaultValue = "true")
    protected boolean includeVersionInOutputDirectory;

    @Parameter(property = "addAdditionalProperties")
    protected List<String> addAdditionalProperties = new ArrayList<>();


    /**
     * Target directory for generated code. Use location relative to the project.baseDir. Default value is
     * "target/openapi".
     */
    @Parameter(property = "output", defaultValue = "${project.build.directory}/openapi")
    protected File output = new File("${project.build.directory}/openapi");

    @Component
    protected ArtifactResolver artifactResolver;

    @Component
    protected MetadataResolver metadataResolver;

    /**
     * Used to look up Artifacts in the remote repository.
     */
    @Parameter(defaultValue = "${repositorySystemSession}", readonly = true)
    protected RepositorySystemSession repositorySession;

    /**
     * List of Remote Repositories used by the resolver.
     */
    @Parameter(defaultValue = "${project.remoteProjectRepositories}", readonly = true)
    protected List<RemoteRepository> remoteRepositories;


    protected boolean isRamlSpec(File file) {
        return file.getName().endsWith("-api.raml")
            || file.getName().endsWith("service-api.raml")
            || file.getName().endsWith("client-api.raml");
    }

    protected File export(String name, String version, File ramlFile, File outputDirectory)
        throws ExportException, IOException {
        getLog().info("Exporting " + name + " to: " + outputDirectory);

        OpenAPI openApi = convert(version, ramlFile);

        String yaml = SerializerUtils.toYamlString(openApi);

        if (!outputDirectory.exists()) {
            outputDirectory.mkdirs();
        }
        File file = new File(outputDirectory, "openapi.yaml");
        getLog().info("Writing Open API Specification to: " + file.getAbsolutePath());
        Files.write(file.toPath(), yaml.getBytes());

        File indexFile = new File(outputDirectory, "index.html");
        InputStream resourceAsStream = getClass().getResourceAsStream("/index.html");
        String index = IOUtils.toString(resourceAsStream, StandardCharsets.UTF_8);
        index = StringUtils.replace(index, "@title@", openApi.getInfo().getTitle());
        Files.write(indexFile.toPath(), index.getBytes());

        success.put(file, openApi);

        return file;
    }


    protected OpenAPI convert(String version, File ramlFile) throws ExportException {
        ExporterOptions options = new ExporterOptions()
            .addJavaTypeExtensions(addJavaTypeExtensions)
            .convertExamplesToYaml(convertJsonExamplesToYaml);

        if (removeDeprecated) {
            options.getTransformers().add(new Deprecator());
        }
        if (decompose) {
            options.getTransformers().add(new Decomposer());
        }
        if (!addAdditionalProperties.isEmpty()) {
            options.getTransformers().add(new AdditionalPropertiesAdder(addAdditionalProperties));
        }

        if (licenseName != null && licenseUrl != null) {
            options.getTransformers().add(new LicenseAdder(licenseName, licenseUrl));
        }

        OpenAPI openApi = Exporter.export(ramlFile, options);
        pimpInfo(version, openApi);
        if (appendDeprecatedMetadataInDescription) {
            // Iterate over all operations and update the description
            openApi.getPaths().values().forEach(pathItem -> pathItem.readOperationsMap().entrySet()
                .stream()
                .filter(this::isDeprecated)
                .forEach(httpMethodOperationEntry -> {
                        Operation operation = httpMethodOperationEntry.getValue();
                        Optional<String> deprecatedInformationOptional = generateMarkdownForDeprecationExtention(operation);

                        deprecatedInformationOptional.ifPresent(deprecatedInformation -> {
                            log.debug("Inserting deprecated information: \n{}", deprecatedInformation);
                            if (operation.getDescription() == null) {
                                operation.setDescription(deprecatedInformation);
                            } else {
                                operation.setDescription(operation.getDescription() + "\n" + deprecatedInformation);
                            }
                        });
                        pathItem.operation(httpMethodOperationEntry.getKey(), operation);
                    }
                ));
        }
        return openApi;
    }

    private boolean isDeprecated(Map.Entry<PathItem.HttpMethod, Operation> httpMethodOperationEntry) {
        Operation value = httpMethodOperationEntry.getValue();
        return value.getDeprecated() != null && value.getDeprecated().equals(Boolean.TRUE);
    }

    private Optional<String> generateMarkdownForDeprecationExtention(Operation operation) {
        if (operation.getExtensions() == null) {
            return Optional.empty();
        }

        String deprecatedFromVersion = (String) operation.getExtensions()
            .get("x-BbApiDeprecation-deprecatedFromVersion");
        String removedFromVersion = (String) operation.getExtensions().get("x-BbApiDeprecation-removedFromVersion");
        String reason = (String) operation.getExtensions().get("x-BbApiDeprecation-reason");
        String description = (String) operation.getExtensions().get("x-BbApiDeprecation-description");

        if (deprecatedFromVersion == null && removedFromVersion == null && reason == null && description == null) {
            return Optional.empty();
        }

        StringBuilder sb = new StringBuilder("## Deprecated\n");
        if (deprecatedFromVersion != null) {
            sb.append("* **Deprecated from:** ").append(deprecatedFromVersion).append("\n");
        }
        if (removedFromVersion != null) {
            sb.append("* **Removed from:** ").append(removedFromVersion).append("\n");
        }
        if (reason != null) {
            sb.append("* **Reason:** ").append(reason).append("\n");
        }
        if (description != null) {
            sb.append("\n### Migration Information\n ");
            sb.append(description);
        }
        return Optional.of(sb.toString());

    }

    private void pimpInfo(String version, OpenAPI openApi) {
        Info info = openApi.getInfo();
        if (version != null) {
            log.info("Overriding version: {}", version);
            info.setVersion(version);

        }
        if (StringUtils.isNotEmpty(xLogoUrl)) {
            Map<String, String> xLogo = new HashMap<>();
            xLogo.put("url", xLogoUrl);
            if (StringUtils.isNotEmpty(xLogoAltText)) {
                xLogo.put("altText", xLogoAltText);
            }
            info.addExtension("x-logo", xLogo);
        }

        if (StringUtils.isNotEmpty(markdownTop)) {
            if (info.getDescription() != null) {
                info.setDescription(markdownTop + "\n" + info.getDescription());
            } else {
                info.setDescription(markdownTop);
            }
        }

        if (StringUtils.isNotEmpty(markdownBottom)) {
            if (info.getDescription() != null) {
                info.setDescription(info.getDescription() + "\n" + markdownBottom);
            } else {
                info.setDescription(markdownBottom);
            }
        }

        if (!servers.isEmpty()) {
            openApi.setServers(servers);
        }
    }

    protected void writeSummary(String title) {
        int total = success.size() + failed.size();

        if (total == 0) {
            getLog().warn("Nothing to export");
            return;
        }

        int percent = (failed.size() * 100) / total;

        log.info(title);
        log.info("\t Total: {} Success: {} Failed: {} ({}%)", total, success.size(), failed.size(), percent);
        if (!success.isEmpty()) {
            log.info("Success:");
            for (Map.Entry<File, OpenAPI> entry : success.entrySet()) {
                log.info("\t{} --> {}", entry.getKey(), entry.getValue().getInfo().getTitle());
            }
        }

        if (!failed.isEmpty()) {
            log.warn("Failed:");
            for (Map.Entry<String, String> entry : failed.entrySet()) {
                log.warn("\t{} Failed. Exception: {}", entry.getKey(), entry.getValue());
            }
        }

    }

    /**
     * @param outputDir The output directory to write to.
     * @throws MojoExecutionException
     */
    protected void writeSwaggerUrls(File outputDir) throws MojoExecutionException {
        List<Map> swaggerUrls = new LinkedList<>();
        Path current = outputDir.toPath();
        success.forEach((file, openAPI) -> {
            Path relative = current.relativize(file.toPath());
            Map entry = new HashMap();
            entry.put("url", relative.toString());
            entry.put("name", openAPI.getInfo().getTitle());
            swaggerUrls.add(entry);
        });

        ObjectMapper objectMapper = new ObjectMapper();
        try {
            File swaggerUrlsFile = new File(output, "swaggerUrls.json");
            String jsonList = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(swaggerUrls);
            Files.write(swaggerUrlsFile.toPath(), jsonList.getBytes());
        } catch (IOException e) {
            throw new MojoExecutionException("Cannot write swagger results");
        }
    }

    public void setProject(MavenProject project) {
        this.project = project;
    }

    public Map<File, OpenAPI> getSuccess() {
        return success;
    }

    public Map<String, String> getFailed() {
        return failed;
    }


    protected List<File> exportArtifact(String groupId, String artifactId, String version, File artifactFile,
        File outputDirectory) throws MojoExecutionException {

        log.info("Converting RAML specs from Artifact {}:{}:{}", groupId, artifactId, version);

        File specUnzipDirectory = new File(project.getBuild().getDirectory() + "/raml/" + version, artifactId);
        unzipSpec(artifactFile, specUnzipDirectory);
        File[] files = specUnzipDirectory.listFiles(this::isRamlSpec);
        assert files != null;
        List<File> exported = new ArrayList<>();
        for (File file : files) {
            String ramlName = StringUtils.substringBeforeLast(file.getName(), ".");
            try {
                File parent = new File(outputDirectory, artifactId);
                File openApiOutputDirectory = new File(parent, ramlName);
                if (includeVersionInOutputDirectory) {
                    openApiOutputDirectory = new File(openApiOutputDirectory, version);
                }
                String name = artifactId + ":" + version + ":" + ramlName;
                File exportedTo = export(name, version, file, openApiOutputDirectory);
                exported.add(exportedTo);
                getLog().info("Exported RAML Spec: " + artifactId + " to: " + file);
            } catch (Exception e) {
                getLog().warn(
                    "Failed to export RAML Spec: " + artifactId + " due to: [" + e.getClass() + "] " + e.getMessage());
                failed.put(artifactId + ":" + ramlName, e.getMessage());
            }
        }
        return exported;
    }

    private void unzipSpec(File inputFile, File unzipDirectory) throws MojoExecutionException {

        unzipDirectory.mkdirs();
        try {
            unzip(inputFile, unzipDirectory);
        } catch (Exception e) {
            throw new MojoExecutionException("Error extracting spec: " + inputFile, e);
        }
    }

    private void unzip(File source, File out) throws Exception {
        Expand expand = new Expand();
        expand.setSrc(source);
        expand.setDest(out);
        expand.setOverwrite(true);
        expand.execute();
    }

    public static void setLog(Logger log) {
        AbstractRamlToOpenApi.log = log;
    }

    public void setIncludeGroupIds(String includeGroupIds) {
        this.includeGroupIds = includeGroupIds;
    }

    public void setxLogoUrl(String xLogoUrl) {
        this.xLogoUrl = xLogoUrl;
    }

    public void setxLogoAltText(String xLogoAltText) {
        this.xLogoAltText = xLogoAltText;
    }

    public void setLicenseName(String licenseName) {
        this.licenseName = licenseName;
    }

    public void setLicenseUrl(String licenseUrl) {
        this.licenseUrl = licenseUrl;
    }

    public void setMarkdownTop(String markdownTop) {
        this.markdownTop = markdownTop;
    }

    public void setMarkdownBottom(String markdownBottom) {
        this.markdownBottom = markdownBottom;
    }

    public void setConvertJsonExamplesToYaml(boolean convertJsonExamplesToYaml) {
        this.convertJsonExamplesToYaml = convertJsonExamplesToYaml;
    }

    public void setAppendDeprecatedMetadataInDescription(boolean appendDeprecatedMetadataInDescription) {
        this.appendDeprecatedMetadataInDescription = appendDeprecatedMetadataInDescription;
    }

    public void setDecompose(boolean decompose) {
        this.decompose = decompose;
    }

    public void setOutput(File output) {
        this.output = output;
    }

    public List<Server> getServers() {
        return servers;
    }

    public void setServers(List<Server> servers) {
        this.servers = servers;
    }

    public ArtifactResult resolveArtifactFromRepositories(org.eclipse.aether.artifact.Artifact artifact) {
        ArtifactRequest artifactRequest = getArtifactRequest(artifact);

        ArtifactResult artifactResult = null;
        try {
            artifactResult = artifactResolver.resolveArtifact(repositorySession, artifactRequest);
        } catch (ArtifactResolutionException e) {
            throw new IllegalArgumentException("Cannot resolve artifact: " + artifact);
        }
        return artifactResult;

    }

    private ArtifactRequest getArtifactRequest(org.eclipse.aether.artifact.Artifact artifact) {
        return new ArtifactRequest(artifact, remoteRepositories, null);
    }

    protected DefaultArtifact createNewDefaultArtifact(Dependency dependency) {
        return new DefaultArtifact(dependency.getGroupId()
            , dependency.getArtifactId()
            , (org.codehaus.plexus.util.StringUtils.isNotEmpty(dependency.getClassifier()) ? dependency.getClassifier()
            : null)
            , (org.codehaus.plexus.util.StringUtils.isNotEmpty(dependency.getType()) ? dependency.getType() : null)
            , dependency.getVersion());
    }


}
