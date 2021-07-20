package com.backbase.oss.boat.transformers;

import com.backbase.oss.boat.loader.OpenAPILoader;
import com.backbase.oss.boat.serializer.SerializerUtils;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.parser.core.models.AuthorizationValue;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.SneakyThrows;
import org.apache.commons.io.FilenameUtils;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.settings.Server;
import org.apache.maven.settings.building.SettingsProblem;
import org.apache.maven.settings.crypto.DefaultSettingsDecryptionRequest;
import org.apache.maven.settings.crypto.SettingsDecrypter;
import org.apache.maven.settings.crypto.SettingsDecryptionRequest;
import org.apache.maven.settings.crypto.SettingsDecryptionResult;
import org.codehaus.plexus.components.io.filemappers.FileMapper;

import static java.lang.String.format;
import static java.util.Optional.ofNullable;
import static org.apache.commons.lang3.StringUtils.isEmpty;

/**
 * Apply transformers to an existing specification.
 */
@Mojo(name = "transform", defaultPhase = LifecyclePhase.GENERATE_SOURCES, threadSafe = true)
public class TransformMojo extends AbstractMojo {

    /**
     * Whether to skip the execution of this goal.
     */
    @Parameter(property = "boat.transform.skip", alias = "codegen.skip")
    boolean skip;

    /**
     * A list of input specifications.
     */
    @Parameter(property = "boat.transform.inputs", required = true)
    final List<String> inputs = new ArrayList<>();

    /**
     * Target directory of the transformed specifications.
     */
    @Parameter(property = "boat.transform.output", defaultValue = "${project.build.directory}")
    File output;

    /**
     * The list of transformers to be applied to each input specification.
     */
    @Parameter(required = true)
    final List<Transformer> pipeline = new ArrayList<>();

    /**
     * File name mappers used to generate the output file name, instances of
     * {@code org.codehaus.plexus.components.io.filemappers.FileMapper}.
     *
     * <p>
     * The following mappers can be used without needing to specify the FQCN of the implementation.
     * <dl>
     * <dt><b>regexp</b></dt>
     * <dd>{@code org.codehaus.plexus.components.io.filemappers.RegExpFileMapper}</dd></dt>
     * <dt><b>merge</b></dt>
     * <dd>{@code org.codehaus.plexus.components.io.filemappers.MergeFileMapper}</dd></dt>
     * <dt><b>prefix</b></dt>
     * <dd>{@code org.codehaus.plexus.components.io.filemappers.PrefixFileMapper}</dd></dt>
     * <dt><b>suffix</b></dt>
     * <dd>{@code org.codehaus.plexus.components.io.filemappers.SuffixFileMapper}</dd></dt>
     * </dl>
     * </p>
     *
     * The parameter defaults to <code>
     * <pre>
     *  &lt;mappers&gt;
     *    &lt;suffix&gt;-transformed&lt;/suffix&gt;
     *  &lt;/mappers&gt;
     * </pre>
     * </code>
     *
     * @see org.codehaus.plexus.components.io.filemappers.FileMapper
     */
    @Parameter
    final List<FileMapper> mappers = new ArrayList<>();

    /**
     * Additional options passed to transformers.
     */
    @Parameter
    final Map<String, Object> options = new HashMap<>();

    /**
     * Retrieves authorization from Maven's {@code settings.xml}.
     */
    @Parameter(name = "serverId", property = "boat.transform.serverId")
    String serverId;

    @Parameter(defaultValue = "${session}", readonly = true)
    private MavenSession session;
    @Component
    private SettingsDecrypter decrypter;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        if (this.skip) {
            getLog().info("Skipping Transform Mojo.");

            return;
        }

        if (this.mappers.isEmpty()) {
            this.mappers.add(new Suffix("-transformed"));
        }

        final List<AuthorizationValue> authz = buildAuthorization();

        try {
            this.inputs.forEach(input -> transform(input, authz));
        } catch (final RuntimeException e) {
            final Throwable cause = e.getCause();

            if (cause instanceof MojoExecutionException) {
                throw (MojoExecutionException) cause;
            }
            if (cause instanceof MojoFailureException) {
                throw (MojoFailureException) cause;
            }

            throw new MojoFailureException("Transformation failed", e);
        }
    }

    @SneakyThrows
    private void transform(String input, List<AuthorizationValue> authz) {
        OpenAPI openAPI = OpenAPILoader.load(input, false, false, authz);

        for (final Transformer t : this.pipeline) {
            openAPI = t.transform(openAPI, this.options);
        }

        String destName = FilenameUtils.getName(input);

        for (final FileMapper fm : this.mappers) {
            destName = fm.getMappedFileName(destName);
        }

        destName = FilenameUtils.separatorsToSystem(destName);

        File destFile = new File(destName);

        if (!destFile.isAbsolute()) {
            destFile = new File(this.output, destName);
        }

        destFile.getParentFile().mkdirs();

        Files.write(destFile.toPath(),
            SerializerUtils
                .toYamlString(openAPI)
                .getBytes(StandardCharsets.UTF_8),
            StandardOpenOption.CREATE);
    }

    private List<AuthorizationValue> buildAuthorization() throws MojoExecutionException {
        return ofNullable(readAuthorization())
            .map(authz -> new AuthorizationValue("Authorization", "header", authz))
            .map(authz -> {
                this.options.put("authz", authz);

                return authz;
            })
            .map(Arrays::asList)
            .orElse(null);
    }

    private String readAuthorization() throws MojoExecutionException {
        if (isEmpty(this.serverId)) {
            return null;
        }

        final Server server = this.session.getSettings().getServer(this.serverId);

        if (server == null) {
            throw new MojoExecutionException(format("Cannot find serverId \"%s\" in Maven settings", this.serverId));
        }

        final SettingsDecryptionRequest request = new DefaultSettingsDecryptionRequest(server);
        final SettingsDecryptionResult result = this.decrypter.decrypt(request);

        // Un-encrypted passwords are passed through, so a problem indicates a real issue.
        // If there are any ERROR or FATAL problems reported, then decryption failed.
        for (final SettingsProblem problem : result.getProblems()) {
            switch (problem.getSeverity()) {
                case ERROR:
                case FATAL:
                    throw new MojoExecutionException(
                        format("Unable to decrypt serverId \"%s\":%s ", this.serverId, problem));

                default:
                    getLog().warn(format("Decrypting \"%s\": %s", this.serverId, problem));
            }
        }

        final Server resultServer = result.getServer();
        final String username = resultServer.getUsername();
        final String password = resultServer.getPassword();
        final String auth = username + ":" + password;

        return "Basic " + Base64.getEncoder().encodeToString(auth.getBytes(StandardCharsets.UTF_8));
    }
}

