package com.backbase.oss.boat;

import ch.qos.logback.classic.Level;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;

class BoatTerminalTest {

    @Test
    void testCLI() {
        assertThat(BoatTerminal.run(new String[]{"-V"}), is(0));
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
