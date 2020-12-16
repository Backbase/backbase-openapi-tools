package com.backbase.oss.boat;

import ch.qos.logback.classic.Level;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BoatTerminalTest {

    @Test
    void testCLI() {
        assertThat(BoatTerminal.run(new String[]{"-V"}), is(0));
    }
    @Test
    void testCLIOptions() {
        assertThat(BoatTerminal.run(new String[]{"-f=src/test/resources/raml-examples/backbase-wallet/presentation-client-api.raml","--directory=src/test/resources/raml-examples/converted","--output=src/test/resources/raml-examples/converted/openapi.yaml","--convert-examples=false"}), is(0));
        File output = new File("src/test/resources/raml-examples/converted/openapi.yaml");

        assertTrue(output.exists());

        assertThat(BoatTerminal.run(new String[]{"--file=src/test/resources/raml-examples/backbase-wallet/presentation-client-api.raml","-d=src/test/resources/raml-examples/converted","-o=src/test/resources/raml-examples/converted/openapi.yaml","--convert-examples=false"}), is(0));
        assertThat(BoatTerminal.run(new String[]{"--file=src/test/resources/raml-examples/backbase-wallet/presentation-client-api.raml","-d=src/test/resources/raml-examples/converted","-v"}),is(0));
    }


    @Test
    void testVerbose(){
        boolean[] testVerbose = {true,false};
        BoatTerminal boatTerminal = new BoatTerminal();
        boatTerminal.setVerbose(testVerbose);
        assertEquals(Level.DEBUG,boatTerminal.getRootLevel());

        testVerbose = new boolean[]{true};

        boatTerminal.setVerbose(testVerbose);
        assertEquals(Level.INFO,boatTerminal.getRootLevel());

        testVerbose = new boolean[]{true,true,true};
        boatTerminal.setVerbose(testVerbose);
        assertEquals(Level.TRACE,boatTerminal.getRootLevel());

    }


}
