package com.backbase.oss.boat.transformers;

import java.io.File;
import java.io.IOException;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.settings.Server;
import org.apache.maven.settings.Settings;
import org.apache.maven.settings.crypto.SettingsDecrypter;
import org.apache.maven.settings.crypto.SettingsDecryptionResult;
import org.codehaus.plexus.logging.console.ConsoleLogger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoSettings;
import org.sonatype.plexus.build.incremental.DefaultBuildContext;

import static java.util.Collections.emptyList;
import static org.apache.commons.io.FileUtils.deleteDirectory;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasKey;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@MockitoSettings
@SuppressWarnings("java:S5979")
class TransformMojoTest {
    private final DefaultBuildContext buildContext = new DefaultBuildContext();

    @Mock
    private MavenSession session;
    @Mock
    private SettingsDecrypter decrypter;

    @InjectMocks
    private TransformMojo mojo;

    @BeforeEach
    void beforeEach() throws IOException {
        this.buildContext.enableLogging(new ConsoleLogger(2, "BOAT"));

        this.mojo.inputs.add("src/test/resources/oas-examples/petstore.yaml");
        this.mojo.output = new File("target", "transform-mojo");

        deleteDirectory(this.mojo.output);
    }

    @Test
    void serverId() throws MojoExecutionException, MojoFailureException {
        this.mojo.serverId = "server-id";

        final Server server = mock(Server.class);
        final Settings settings = mock(Settings.class);
        final SettingsDecryptionResult result = mock(SettingsDecryptionResult.class);

        when(this.session.getSettings()).thenReturn(settings);
        when(settings.getServer(this.mojo.serverId)).thenReturn(server);
        when(this.decrypter.decrypt(any())).thenReturn(result);
        when(result.getProblems()).thenReturn(emptyList());
        when(result.getServer()).thenReturn(server);
        when(server.getUsername()).thenReturn("username");
        when(server.getPassword()).thenReturn("password");


        this.mojo.execute();

        assertThat(this.mojo.options, hasKey("authz"));
        assertThat(output("petstore-transformed").exists(), is(true));
    }

    @Test
    void empty() throws MojoExecutionException, MojoFailureException {
        this.mojo.execute();

        assertThat(output("petstore-transformed").exists(), is(true));
    }

    @Test
    void transform() throws MojoExecutionException, MojoFailureException {
        final Transformer tran = mock(Transformer.class);

        when(tran.transform(any(), any())).then(ivc -> ivc.getArgument(0));

        this.mojo.pipeline.add(tran);
        this.mojo.execute();

        verify(tran, times(1)).transform(any(), any());
        assertThat(output("petstore-transformed").exists(), is(true));
    }

    @Test
    void mappers() throws MojoExecutionException, MojoFailureException {
        this.mojo.mappers.add(new Merge("open--api"));
        this.mojo.mappers.add(new Prefix("prefix-"));
        this.mojo.mappers.add(new Suffix("-suffix"));
        this.mojo.mappers.add(new Regex("--", "-"));
        this.mojo.mappers.add(new Suffix(".yaml"));
        this.mojo.execute();

        assertThat(output("prefix-open-api-suffix").exists(), is(true));
    }

    @Test
    void merge() throws MojoExecutionException, MojoFailureException {
        this.mojo.mappers.add(new Merge("open--api.yaml"));
        this.mojo.execute();

        assertThat(output("open--api").exists(), is(true));
    }

    @Test
    void suffix() throws MojoExecutionException, MojoFailureException {
        this.mojo.mappers.add(new Suffix("-suffix"));
        this.mojo.execute();

        assertThat(output("petstore-suffix").exists(), is(true));
    }

    @Test
    void prefix() throws MojoExecutionException, MojoFailureException {
        this.mojo.mappers.add(new Prefix("prefix-"));
        this.mojo.execute();

        assertThat(output("prefix-petstore").exists(), is(true));
    }

    @Test
    void regex() throws MojoExecutionException, MojoFailureException {
        this.mojo.mappers.add(new Regex("(.+)tst(.+)", "$1te-st$2"));
        this.mojo.execute();

        assertThat(output("pete-store").exists(), is(true));
    }

    private File output(String name) {
        return new File(this.mojo.output, name + ".yaml");
    }
}
