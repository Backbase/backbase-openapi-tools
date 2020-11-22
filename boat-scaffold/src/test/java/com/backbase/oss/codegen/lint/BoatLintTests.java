package com.backbase.oss.codegen.lint;

import com.backbase.oss.codegen.yard.BoatYardConfig;
import com.backbase.oss.codegen.yard.BoatYardGenerator;
import java.io.File;
import java.net.URL;
import org.junit.Test;

public class BoatLintTests {

    @Test
    public void testBoatLint() {

        File input = getFile("/oas-examples/petstore.yaml");
        File output = new File("target/boat-lint");

        BoatLintConfig config = new BoatLintConfig();
        config.setInputSpec(input.getAbsolutePath());
        config.setOutputDir(output.getAbsolutePath());

        config.setTemplateDir("boat-lint");

        new BoatLintGenerator(config).generate();
    }

    protected File getFile(String name) {
        URL resource = getClass().getResource(name);
        assert resource != null;
        return new File(resource.getFile());
    }
}
