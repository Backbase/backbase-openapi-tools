package com.backbase.oss.codegen.lint;

import com.backbase.oss.boat.loader.OpenAPILoaderException;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import org.jetbrains.annotations.NotNull;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import org.openapitools.codegen.ClientOptInput;

class BoatLintTests {

    @Test
    void testBoatLint() throws IOException {

        File input = getFile("/oas-examples/petstore.yaml");
        File output = generate(input);

        String[] actualDirectorySorted = output.list();
        Arrays.sort(actualDirectorySorted);
        String[] expectedDirectory = {"backbase-logo.svg", "css", "index.html", "js"};
        assertArrayEquals(expectedDirectory, actualDirectorySorted);

        File index = new File("target/boat-lint/index.html");
        String generated = String.join(" ", Files.readAllLines(Paths.get(index.getPath())));
        assertTrue(generated.startsWith("<html"));
    }

    @Test
    void testUnreadable() {
        File input = getFile("/oas-examples/unable-to-parse.yaml");
        assertThrows(IllegalArgumentException.class, () -> generate(input));
    }

    @NotNull
    private File generate(File input) {
        File output = new File("target/boat-lint");

        BoatLintConfig config = new BoatLintConfig();
        config.setInputSpec(input.getAbsolutePath());
        config.setOutputDir(output.getAbsolutePath());

        config.setTemplateDir("boat-lint");


        BoatLintGenerator boatLintGenerator = new BoatLintGenerator();
        boatLintGenerator.opts(new ClientOptInput().config(config));
        boatLintGenerator.generate();
        return output;
    }


    protected File getFile(String name) {
        URL resource = getClass().getResource(name);
        assert resource != null;
        return new File(resource.getFile());
    }
}
