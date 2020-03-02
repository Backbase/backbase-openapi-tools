package com.backbase.oss.boat;

import com.backbase.oss.boat.diff.BatchOpenApiDiff;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.maven.artifact.repository.metadata.io.xpp3.MetadataXpp3Reader;
import org.apache.maven.artifact.versioning.DefaultArtifactVersion;
import org.apache.maven.artifact.versioning.InvalidVersionSpecificationException;
import org.apache.maven.artifact.versioning.VersionRange;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.metadata.DefaultMetadata;
import org.eclipse.aether.metadata.Metadata;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.resolution.ArtifactResult;
import org.eclipse.aether.resolution.MetadataRequest;
import org.eclipse.aether.resolution.MetadataResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mojo(name = "raml2openapi-bom", requiresDependencyResolution = ResolutionScope.RUNTIME, threadSafe = true)
public class ExportBomMojo extends AbstractRamlToOpenApi {

    public static final String X_CHANGELOG = "x-changelog";

    private static final Logger log = LoggerFactory.getLogger(ExportBomMojo.class);

    @Parameter(property = "includeGroupId")
    private String includeGroupIds;

    @Parameter(property = "includeVersionsRegEx", defaultValue = "^(\\d+\\.)?(\\d+\\.)?(\\d+\\.)?(\\*|\\d+)$")
    private String includeVersionsRegEx;

    @Parameter(property = "includeArtifactIdRegEx", defaultValue = "(\\w-spec)$")
    private String includeArtifactIdRegEx;

    @Parameter(property = "spec-bom")
    private Dependency specBom;

    @Parameter(name = "addChangeLog", defaultValue = "true")
    private boolean addChangeLog;


    @Override
    public void execute() throws MojoExecutionException {
        Set<MetadataRequest> metadataRequests = remoteRepositories.stream()
            .map(this::createMetadataRequest)
            .collect(Collectors.toSet());
        VersionRange versionRange = getVersionRange();

        Map<String, Map<String, List<File>>> specAndVersions = new TreeMap<>();

        List<MetadataResult> metadataResults = metadataResolver.resolveMetadata(repositorySession, metadataRequests);
        List<Pair<String, TreeMap<String, Set<ArtifactResult>>>> versionCapabilitySpecs = metadataResults.stream()
            .filter(MetadataResult::isResolved)
            .map(metadataResult -> metadataResult.getMetadata().getFile())
            .map(this::parseMetadataFile)
            .flatMap(metadata -> metadata.getVersioning().getVersions().stream())
            .filter(version -> version.matches(includeVersionsRegEx))
            .map(DefaultArtifactVersion::new)
            .filter(versionRange::containsVersion)
            .distinct()
            .peek(defaultArtifactVersion -> log.info("Resolving Specs for version: {}", defaultArtifactVersion))
            .map(this::convertToArtifact)
            .map(this::resolveArtifactFromRepositories)
            .map(this::parsePomFile)
            .map(this::groupArtifactsPerVersionAndCapability)
            .collect(Collectors.toList());

        export(versionCapabilitySpecs, specAndVersions);
        writeSummary("Converted RAML Specs to OpenAPI Summary");
        if (addChangeLog) {
            try {
                success.clear();
                failed.clear();
                BatchOpenApiDiff.diff(output.toPath(), success, failed, true, true);
                writeSummary("Calculated Change log for APIs");
            } catch (Exception e) {
                throw new MojoExecutionException("Cannot create diff", e);
            }
        }
    }


    private Pair<String, TreeMap<String, Set<ArtifactResult>>> groupArtifactsPerVersionAndCapability(Model model) {
        return Pair.of(model.getVersion(),
            model.getDependencyManagement().getDependencies().stream()
                .filter(this::isIncludedSpec)
                .map(this::createNewDefaultArtifact)
                .distinct()
                .map(this::resolveArtifactFromRepositories)
                .collect(Collectors.groupingBy(artifactResult -> artifactResult.getArtifact().getGroupId(), TreeMap::new, Collectors.toSet())));
    }

    protected void export(List<Pair<String, TreeMap<String, Set<ArtifactResult>>>> versionCapabilitySpecs, Map<String, Map<String, List<File>>> specAndVersions) {
        for (Pair<String, TreeMap<String, Set<ArtifactResult>>> versionSpecs : versionCapabilitySpecs) {
            String version = versionSpecs.getKey();

            versionSpecs.getValue().entrySet().stream().forEach(entry -> {
                String capabilty = entry.getKey();
                Set<ArtifactResult> specs = entry.getValue();

                File capabilityDirectory = new File(output, capabilty);
                capabilityDirectory.mkdirs();

                specs.stream().filter(ArtifactResult::isResolved)
                    .map(ArtifactResult::getArtifact)
                    .forEach(artifact -> {
                        try {
                            List<File> files = exportArtifact(artifact.getGroupId(), artifact.getArtifactId(), version, artifact.getFile(), capabilityDirectory);

                            log.info("Successfully exported artifact: {} to: {}", artifact, files);
                        } catch (MojoExecutionException e) {
                            log.error("Failed to export artifact: {}", artifact, e);
                        }
                    });

                specs.stream().filter(ArtifactResult::isMissing).forEach(artifactResult -> log.error("Failed to resolve: {}", artifactResult));

            });
        }

    }

    private boolean isIncludedSpec(Dependency dependency) {
        String artifactId = dependency.getArtifactId();
        return artifactId.endsWith("-spec");
    }


    private VersionRange getVersionRange() throws MojoExecutionException {
        try {
            return VersionRange.createFromVersionSpec(specBom.getVersion());
        } catch (InvalidVersionSpecificationException e) {
            throw new MojoExecutionException("Cannot parse version: " + specBom.getVersion());
        }
    }

    private MetadataRequest createMetadataRequest(RemoteRepository remoteRepository) {
        MetadataRequest metadataRequest = new MetadataRequest();
        metadataRequest.setRepository(remoteRepository);
        metadataRequest.setMetadata(new DefaultMetadata(specBom.getGroupId(), specBom.getArtifactId(), "maven-metadata.xml", Metadata.Nature.RELEASE));
        return metadataRequest;
    }

    private org.apache.maven.artifact.repository.metadata.Metadata parseMetadataFile(File mavenDataFile) {
        MetadataXpp3Reader metadataXpp3Reader = new MetadataXpp3Reader();
        try {
            FileInputStream in = new FileInputStream(mavenDataFile);
            return metadataXpp3Reader.read(in);
        } catch (IOException | XmlPullParserException e) {
            throw new IllegalArgumentException("Cannot read metadata from: " + mavenDataFile, e);
        }
    }

    public Model parsePomFile(ArtifactResult pom) {
        File pomFile = pom.getArtifact().getFile();
        MavenXpp3Reader reader = new MavenXpp3Reader();
        Model model = null;
        try {
            model = reader.read(new FileReader(pomFile));
        } catch (Exception e) {
            throw new IllegalArgumentException("Cannot read pom file");
        }

        // Merge dependencies version numbers inside the poms

        Properties properties = model.getProperties();
        List<org.apache.maven.model.Dependency> dependencyManagementDependencies = model.getDependencyManagement().getDependencies();
        dependencyManagementDependencies.forEach(dependency -> dependency.setVersion(replacePlaceholders(properties, dependency.getVersion())));

        model.getProfiles().forEach(profile -> profile.getDependencies().forEach(profileDependency ->
            setManagedVersionDependency(dependencyManagementDependencies, profileDependency)
        ));

        return model;

    }

    private void setManagedVersionDependency(List<org.apache.maven.model.Dependency> dependencyManagementDependencies, org.apache.maven.model.Dependency profileDependency) {
        Optional<org.apache.maven.model.Dependency> managedDependency = resolveDependencyVersion(dependencyManagementDependencies, profileDependency);
        managedDependency.ifPresent(dependency -> profileDependency.setVersion(dependency.getVersion()));
    }

    private Optional<org.apache.maven.model.Dependency> resolveDependencyVersion(List<org.apache.maven.model.Dependency> dependencyManagementDependencies, org.apache.maven.model.Dependency profileDependency) {
        return dependencyManagementDependencies.stream().
            filter(dependency ->
                profileDependency.getArtifactId().equals(dependency.getArtifactId())
                    && profileDependency.getGroupId().equals(dependency.getGroupId())
                    && profileDependency.getType().equals(dependency.getType())

            ).findFirst();
    }

    private String replacePlaceholders(Properties properties, String template) {
        Pattern pattern = Pattern.compile("\\$\\{(.+?)\\}");
        Matcher matcher = pattern.matcher(template);
        // StringBuilder cannot be used here because Matcher expects StringBuffer
        StringBuffer buffer = new StringBuffer();
        while (matcher.find()) {
            if (properties.containsKey(matcher.group(1))) {
                String replacement = properties.get(matcher.group(1)).toString();
                // quote to work properly with $ and {,} signs
                matcher.appendReplacement(buffer, replacement != null ? Matcher.quoteReplacement(replacement) : "null");
            }
        }
        matcher.appendTail(buffer);
        return buffer.toString();

    }

    protected DefaultArtifact convertToArtifact(DefaultArtifactVersion defaultArtifactVersion) {
        return new DefaultArtifact(specBom.getGroupId()
            , specBom.getArtifactId()
            , (org.codehaus.plexus.util.StringUtils.isNotEmpty(specBom.getClassifier()) ? specBom.getClassifier() : null)
            , (org.codehaus.plexus.util.StringUtils.isNotEmpty(specBom.getType()) ? specBom.getType() : null)
            , defaultArtifactVersion.toString());
    }

}
