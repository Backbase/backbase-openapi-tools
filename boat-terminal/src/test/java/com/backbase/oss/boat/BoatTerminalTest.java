package com.backbase.oss.boat;

import ch.qos.logback.classic.Level;
import org.junit.jupiter.api.Test;

import java.io.File;

import static java.lang.String.format;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.*;

class BoatTerminalTest {

    @Test
    void testCLI() {
        assertThat(BoatTerminal.run("-V"), is(0));
    }

    @Test
    void testCLIOptions() {
        final String target = "target/test-cli-options";
        final String openapi = format("%s/openapi.yaml", target);

        assertThat(
            BoatTerminal.run(
                "-f=src/test/resources/raml-examples/backbase-wallet/presentation-client-api.raml",
                "--directory=" + target,
                "--output=" + openapi,
                "--convert-examples=false"),
            is(0));

        assertTrue(new File(openapi).exists());

        assertThat(BoatTerminal.run(
            "--file=src/test/resources/raml-examples/backbase-wallet/presentation-client-api.raml",
            "-d=" + target,
            "-o=" + openapi,
            "--convert-examples=false"),
            is(0));
        assertThat(BoatTerminal.run(
            "--file=src/test/resources/raml-examples/backbase-wallet/presentation-client-api.raml",
            "-d=src/test/resources/raml-examples/converted",
            "-v"),
            is(0));
    }

    @Test
    void testCLIErrorCatching() {
        assertThat(BoatTerminal.run(
            "-f=src/test/resources/raml-examples/backbase-wallet/file-not-found ",
            "--directory=src/test/resources/raml-examples/converted",
            "--output=src/test/resources/raml-examples/converted/openapi.yaml",
            "--convert-examples=false"),
            is(-2));
        assertThat(BoatTerminal.run(
            "-f src/test/resources/raml-examples/backbase-wallet/presentation-client-api.raml",
            "--directory src/test/resources/raml-examples/converted",
            "--output src/test/resources/raml-examples/converted/openapi.yaml",
            "--convert-examples false"),
            is(2));
    }


    @Test
    void testVerbose() {
        boolean[] testVerbose = {true, false};
        final BoatTerminal boatTerminal = new BoatTerminal();
        boatTerminal.setVerbose(testVerbose);
        assertEquals(Level.DEBUG, boatTerminal.getRootLevel());

        testVerbose = new boolean[] {true};

        boatTerminal.setVerbose(testVerbose);
        assertEquals(Level.INFO, boatTerminal.getRootLevel());

        testVerbose = new boolean[] {true, true, true};
        boatTerminal.setVerbose(testVerbose);
        assertEquals(Level.TRACE, boatTerminal.getRootLevel());

    }


}
