package com.backbase.oss.boat;

import java.io.File;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.maven.plugin.MojoExecutionException;
import org.junit.Test;

@Slf4j
public class DiffMojoTests {

    @SneakyThrows
    public void testNonBreakingChange() {
        DiffMojo diffMojo = new DiffMojo();
        diffMojo.setOldFile(getFile("/oas-examples/petstore.yaml"));
        diffMojo.setNewFile(getFile("/oas-examples/petstore-new-non-breaking.yaml"));
        diffMojo.execute();
    }

    @SneakyThrows
    @Test
    public void testBreakingChange() {
        DiffMojo diffMojo = new DiffMojo();
        diffMojo.setOldFile(getFile("/oas-examples/petstore.yaml"));
        diffMojo.setNewFile(getFile("/oas-examples/petstore-new-breaking.yaml"));
        diffMojo.execute();

    }

    @Test(expected = MojoExecutionException.class)
    public void testBreakingChangeWithBreaking() throws MojoExecutionException {
        DiffMojo diffMojo = new DiffMojo();
        diffMojo.setOldFile(getFile("/oas-examples/petstore.yaml"));
        diffMojo.setNewFile(getFile("/oas-examples/petstore-new-breaking.yaml"));
        diffMojo.setBreakOnBreakingChanges(true);
        diffMojo.execute();

    }


    private File getFile(String fileName) {
        return new File(getClass().getResource(fileName).getFile());
    }
}
