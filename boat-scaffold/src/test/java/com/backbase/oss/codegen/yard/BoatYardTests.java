package com.backbase.oss.codegen.yard;

import java.io.File;
import java.net.URL;
import org.junit.jupiter.api.Test;

public class BoatYardTests {

    @Test
    public void testBoatYard() {

        File input = getFile("/boat-yard/example-portal.yaml");
        File output = new File("target/boat-yard");
        File specsBaseDir = new File("src/test/resources");


        BoatYardConfig config = new BoatYardConfig();
        config.setInputSpec(input.getAbsolutePath());
        config.setOutputDir(output.getAbsolutePath());
        config.setSpecsBaseDir(specsBaseDir);

        config.setTemplateDir("boat-yard");

        new BoatYardGenerator(config).generate();
    }

    protected File getFile(String name) {
        URL resource = getClass().getResource(name);
        assert resource != null;
        return new File(resource.getFile());
    }
}
