package com.backbase.oss.boat;

import java.io.File;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.maven.plugin.MojoExecutionException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Slf4j
@SuppressWarnings("java:S2699")
class DiffMojoTests {

    @SneakyThrows
    @Test
    void testNonBreakingChange() {
        DiffMojo diffMojo = new DiffMojo();
        diffMojo.setOldFile(getFile("/oas-examples/petstore.yaml"));
        diffMojo.setNewFile(getFile("/oas-examples/petstore-really-non-breaking.yaml"));
        diffMojo.setWriteChangelog(true);
        diffMojo.setChangelogOutput(new File("target"));
        diffMojo.setChangelogRenderer("markdown");
        diffMojo.execute();

        assertTrue(new File(diffMojo.getChangelogOutput(), "changelog.md").exists());
    }

    @Test
    void testBreakingChange() throws MojoExecutionException {
        DiffMojo diffMojo = new DiffMojo();
        diffMojo.setOldFile(getFile("/oas-examples/petstore.yaml"));
        diffMojo.setNewFile(getFile("/oas-examples/petstore-new-breaking.yaml"));
        diffMojo.setWriteChangelog(true);
        diffMojo.setChangelogOutput(new File("target"));
        diffMojo.setChangelogRenderer("html");
        diffMojo.execute();
        assertTrue(new File(diffMojo.getChangelogOutput(), "changelog.html").exists());
    }

    @Test
    void testBreakingChangeWithBreaking() throws MojoExecutionException {
        DiffMojo diffMojo = new DiffMojo();
        diffMojo.setOldFile(getFile("/oas-examples/petstore.yaml"));
        diffMojo.setNewFile(getFile("/oas-examples/petstore-new-breaking.yaml"));
        diffMojo.setBreakOnBreakingChanges(true);
        assertThrows(MojoExecutionException.class, diffMojo::execute);
    }


    private File getFile(String fileName) {
        return new File(getClass().getResource(fileName).getFile());
    }
}
