package com.backbase.boat;

import java.io.File;
import java.io.IOException;
import org.junit.Assert;
import org.junit.Test;

public class Raml2OpenApiTest {
    @Test
    public void testExistingInputFile() {
        File inputFile = getFile("/api.raml");
        String[] args = {"-f", inputFile.getAbsolutePath()};
        BoatTerminal.main(args);
        Assert.assertTrue(true);
    }

    @Test
    public void testOutputFile() throws IOException {
        File inputFile = getFile("/api.raml");
        File outputFile = File.createTempFile("oas3", "yaml");
        String[] args = {"-f", inputFile.getAbsolutePath(), "-o", outputFile.getAbsolutePath()};
        BoatTerminal.main(args);
        Assert.assertTrue(outputFile.exists());
    }


    private File getFile(String name) {
        return new File(getClass().getResource(name).getFile());
    }


}
