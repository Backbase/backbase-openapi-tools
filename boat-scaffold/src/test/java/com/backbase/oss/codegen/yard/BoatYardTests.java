package com.backbase.oss.codegen.yard;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import org.junit.jupiter.api.Test;
import org.openapitools.codegen.ClientOptInput;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BoatYardTests {

    @Test
    void testBoatYard() throws IOException {

        File input = getFile("/boat-yard/example-portal.yaml");
        File output = new File("target/boat-yard");
        File specsBaseDir = new File("src/test/resources");


        BoatYardConfig config = new BoatYardConfig();
        config.setInputSpec(input.getAbsolutePath());
        config.setOutputDir(output.getAbsolutePath());
        config.setSpecsBaseDir(specsBaseDir);

        config.setTemplateDir("boat-yard");

        BoatYardGenerator boatYardGenerator = new BoatYardGenerator();
        boatYardGenerator.opts(new ClientOptInput().config(config));
        boatYardGenerator.generate();


        String[] actualDirectorySorted = output.list();
        Arrays.sort(actualDirectorySorted);
        String[] expectedDirectory = {"backbase-logo.svg", "boat-quay", "boat-wharf", "css", "index.html", "js"};
        assertArrayEquals(expectedDirectory, actualDirectorySorted);

        File index = new File("target/boat-yard/index.html");
        String generated = String.join(" ", Files.readAllLines(Paths.get(index.getPath())));
        assertTrue(generated.contains("<title>BOAT Developer Portal</title>"));
    }

    protected File getFile(String name) {
        URL resource = getClass().getResource(name);
        assert resource != null;
        return new File(resource.getFile());
    }
}
