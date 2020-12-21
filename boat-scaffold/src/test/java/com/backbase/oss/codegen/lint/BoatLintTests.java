package com.backbase.oss.codegen.lint;

import com.backbase.oss.codegen.yard.BoatYardConfig;
import com.backbase.oss.codegen.yard.BoatYardGenerator;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.net.URL;
import java.nio.file.*;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BoatLintTests {

    @Test
    void testBoatLint() throws IOException {

        File input = getFile("/oas-examples/petstore.yaml");
        File output = new File("target/boat-lint");

        BoatLintConfig config = new BoatLintConfig();
        config.setInputSpec(input.getAbsolutePath());
        config.setOutputDir(output.getAbsolutePath());

        config.setTemplateDir("boat-lint");

        new BoatLintGenerator(config).generate();

        String[] actualDirectorySorted =output.list();
        Arrays.sort(actualDirectorySorted);
        String[] expectedDirectory= {"backbase-logo.svg","css","index.html","js"};
        assertArrayEquals(expectedDirectory,actualDirectorySorted);

        File index = new File("target/boat-lint/index.html");
        String generated = String.join( " ", Files.readAllLines(Paths.get(index.getPath())));
        assertTrue(generated.contains("<title>BOAT Lint Report - Swagger Petstore</title>"));
    }

    protected File getFile(String name) {
        URL resource = getClass().getResource(name);
        assert resource != null;
        return new File(resource.getFile());
    }
}
