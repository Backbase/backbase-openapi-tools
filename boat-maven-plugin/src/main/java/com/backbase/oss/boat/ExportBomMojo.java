package com.backbase.oss.boat;

import com.backbase.oss.boat.diff.BatchOpenApiDiff;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.maven.artifact.repository.metadata.io.xpp3.MetadataXpp3Reader;
import org.apache.maven.artifact.versioning.DefaultArtifactVersion;
import org.apache.maven.artifact.versioning.InvalidVersionSpecificationException;
import org.apache.maven.artifact.versioning.VersionRange;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.apache.maven.model.Profile;
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

@Mojo(name = "export-bom", requiresDependencyResolution = ResolutionScope.RUNTIME, threadSafe = true)
/**
 * Converts all RAML Spec dependencies to OpenAPI Specs.
 */
public class ExportBomMojo extends AbstractRamlToOpenApi {

    public static final String X_CHANGELOG = "x-changelog";

    private static final Logger log = LoggerFactory.getLogger(ExportBomMojo.class);

    @Parameter(property = "includeVersionsRegEx", defaultValue = "^(\\d+\\.)?(\\d+\\.)?(\\d+\\.)?(\\*|\\d+)$")
    private String includeVersionsRegEx;

    @Parameter(property = "includeArtifactIdRegEx", defaultValue = "(\\w-spec)$")
    private String includeArtifactIdRegEx;

    @Parameter(property = "spec-bom")
    private Dependency specBom;

    @Parameter(name = "addChangeLog", defaultValue = "true")
    private boolean addChangeLog;

    public void setSpecBom(Dependency specBom){this.specBom = specBom;}


    @Override
    public void execute() throws MojoExecutionException {

        Set<MetadataRequest> metadataRequests = remoteRepositories.stream()
            .map(remoteRepository -> createMetadataRequest(remoteRepository, "maven-metadata.xml"))
            .collect(Collectors.toSet());
        metadataRequests.add(createMetadataRequest(null, "maven-metadata.xml"));
        VersionRange versionRange = getVersionRange();

        log.info("Checking BOM Meta Data for: {}:{}", specBom.getGroupId(), specBom.getArtifactId());

        List<MetadataResult> metadataResults = metadataResolver.resolveMetadata(repositorySession, metadataRequests);

        List<MetadataResult> remoteMetaData = metadataResults.stream()
            .filter(MetadataResult::isResolved)
            .collect(Collectors.toList());

        if (remoteMetaData.isEmpty()) {
            log.warn("Failed to resolve meta data for: {}:{}. Exiting", specBom.getGroupId(), specBom.getArtifactId());
            return;
        }

        List<File> metaDataList = remoteMetaData.stream()
            .map(metadataResult -> metadataResult.getMetadata().getFile())
            .collect(Collectors.toList());

        log.info("Resolved meta data for: {}:{} in: {}", specBom.getGroupId(), specBom.getArtifactId(), metaDataList);

        List<String> versions = metaDataList.stream()
            .map(this::parseMetadataFile)
            .flatMap(metadata -> metadata.getVersioning().getVersions().stream())
            .filter(version -> {
                    if (!StringUtils.isEmpty(includeVersionsRegEx)) {
                        return version.matches(includeVersionsRegEx);
                    } else {
                        return true;
                    }
                }
            )
            .collect(Collectors.toList());

        log.info("Resolved versions: {}", versions);

        List<Pair<String, TreeMap<String, Set<ArtifactResult>>>> versionCapabilitySpecs = versions.stream()
            .map(DefaultArtifactVersion::new)
            .filter(versionRange::containsVersion)
            .distinct()
            .map(this::convertToArtifact)
            .map(defaultArtifact -> new ArtifactRepositoryResolver(artifactResolver,repositorySession,remoteRepositories).resolveArtifactFromRepositories(defaultArtifact))
            .map(this::parsePomFile)
            .map(this::groupArtifactsPerVersionAndCapability)
            .collect(Collectors.toList());

        if (versionCapabilitySpecs.isEmpty()) {
            log.info("No specs found in bom!");
            return;
        }
        log.info("Converting {} RAML specs found in bom", versionCapabilitySpecs.size());

        export(versionCapabilitySpecs);
        writeSummary("Converted RAML Specs to OpenAPI Summary");
        if (addChangeLog && !versionCapabilitySpecs.isEmpty()) {
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

        Set<Dependency> dependencies = getAllDependenciesFromBom(model);

        TreeMap<String, Set<ArtifactResult>> collect = dependencies.stream()
            .filter(this::isIncludedSpec)
            .map(this::createNewDefaultArtifact)
            .distinct()
            .map(defaultArtifact ->  new ArtifactRepositoryResolver(artifactResolver,repositorySession,remoteRepositories).resolveArtifactFromRepositories(defaultArtifact))
            .collect(Collectors
                .groupingBy(artifactResult -> artifactResult.getArtifact().getGroupId(), TreeMap::new,
                    Collectors.toSet()));

        return Pair.of(model.getVersion(),
            collect);
    }

    private Set<Dependency> getAllDependenciesFromBom(Model model) {
        Set<Dependency> dependencies = new HashSet<>();
        if (model.getDependencyManagement() != null) {
            dependencies.addAll(model.getDependencyManagement().getDependencies());
        }
        if (model.getDependencies() != null) {
            dependencies.addAll(model.getDependencies());
        }
        if (model.getProfiles() != null) {
            model.getProfiles().forEach(profile -> dependencies.addAll(profile.getDependencies()));
        }
        return dependencies;
    }

    protected void export(List<Pair<String, TreeMap<String, Set<ArtifactResult>>>> versionCapabilitySpecs)
        throws MojoExecutionException {
        for (Pair<String, TreeMap<String, Set<ArtifactResult>>> versionSpecs : versionCapabilitySpecs) {
            String version = versionSpecs.getKey();

            for (Entry<String, Set<ArtifactResult>> entry : versionSpecs.getValue().entrySet()) {
                String capabilty = entry.getKey();
                Set<ArtifactResult> specs = entry.getValue();

                File capabilityDirectory = new File(output, capabilty);
                try {
                    Files.createDirectories(capabilityDirectory.toPath());
                } catch (IOException e) {
                    throw new MojoExecutionException("Cannot create output directory: " + capabilityDirectory);
                }

                specs.stream().filter(ArtifactResult::isResolved)
                    .map(ArtifactResult::getArtifact)
                    .forEach(artifact -> {
                        try {
                            List<File> files = exportArtifact(artifact.getGroupId(), artifact.getArtifactId(), version,
                                artifact.getFile(), capabilityDirectory);

                            log.info("Successfully exported artifact: {} to: {}", artifact, files);
                        } catch (MojoExecutionException e) {
                            log.error("Failed to export artifact: {}", artifact, e);
                        }
                    });

                specs.stream().filter(ArtifactResult::isMissing)
                    .forEach(artifactResult -> log.error("Failed to resolve: {}", artifactResult));

            }
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

    private MetadataRequest createMetadataRequest(RemoteRepository remoteRepository, String type) {
        MetadataRequest metadataRequest = new MetadataRequest();
        if (remoteRepository != null) {
            metadataRequest.setRepository(remoteRepository);
        }
        metadataRequest.setMetadata(new DefaultMetadata(specBom.getGroupId(), specBom.getArtifactId(),
            type, Metadata.Nature.RELEASE_OR_SNAPSHOT));
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
        Model model;
        try {
            model = reader.read(new FileReader(pomFile));
        } catch (Exception e) {
            throw new IllegalArgumentException("Cannot read pom file");
        }
        // Merge dependencies version numbers inside the poms
        Properties properties = model.getProperties();
        List<org.apache.maven.model.Dependency> dependencyManagementDependencies = new ArrayList<>();
        if (model.getDependencyManagement() != null) {
            dependencyManagementDependencies = model.getDependencyManagement().getDependencies().stream()
                .map(dependency -> resolvePropertyPlaceholderVersion(properties, dependency))
                .collect(Collectors.toList());
        }
        if (model.getDependencies() != null) {
            for (Dependency dependency : model.getDependencies()) {
                if (dependency.getVersion() == null) {
                    setManagedVersionDependency(dependencyManagementDependencies, dependency);
                } else {
                    resolvePropertyPlaceholderVersion(properties, dependency);
                }
            }

        }
        if (model.getProfiles() != null) {
            for (Profile profile : model.getProfiles()) {
                log.info("Exporting spec from profile: {}", profile.getId());
                List<Dependency> profileDependencies = profile.getDependencies().stream()
                    .map(dependency -> resolvePropertyPlaceholderVersion(properties, dependency))
                    .collect(Collectors.toList());

                for (Dependency dependency : profileDependencies) {
                    setManagedVersionDependency(dependencyManagementDependencies, dependency);
                }
            }
        }
        return model;
    }

    private Dependency resolvePropertyPlaceholderVersion(Properties properties, Dependency dependency) {
        if (dependency.getVersion() != null && dependency.getVersion().contains("$")) {
            dependency.setVersion(replacePlaceholders(properties, dependency.getVersion()));
        }
        return dependency;
    }

    private void setManagedVersionDependency
        (List<org.apache.maven.model.Dependency> dependencyManagementDependencies,
         org.apache.maven.model.Dependency profileDependency) {
        if(profileDependency.getVersion()!=null) {
            return;
        }
        Optional<org.apache.maven.model.Dependency> managedDependency = resolveDependencyVersion(
            dependencyManagementDependencies, profileDependency);
        managedDependency.ifPresent(dependency -> profileDependency.setVersion(dependency.getVersion()));
    }

    private Optional<org.apache.maven.model.Dependency> resolveDependencyVersion(
        List<org.apache.maven.model.Dependency> dependencyManagementDependencies,
        org.apache.maven.model.Dependency profileDependency) {
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
                matcher.appendReplacement(buffer,
                    replacement != null ? Matcher.quoteReplacement(replacement) : "null");
            }
        }
        matcher.appendTail(buffer);
        String s = buffer.toString();
        log.debug("Replaced placeholder: {} with: {}", template, s);
        return s;

    }

    protected DefaultArtifact convertToArtifact(DefaultArtifactVersion defaultArtifactVersion) {
        return new DefaultArtifact(specBom.getGroupId()
            , specBom.getArtifactId()
            ,
            (org.codehaus.plexus.util.StringUtils.isNotEmpty(specBom.getClassifier()) ? specBom.getClassifier()
                : null)
            , (org.codehaus.plexus.util.StringUtils.isNotEmpty(specBom.getType()) ? specBom.getType() : null)
            , defaultArtifactVersion.toString());
    }

}
