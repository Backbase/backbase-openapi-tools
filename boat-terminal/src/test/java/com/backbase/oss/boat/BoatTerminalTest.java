package com.backbase.oss.boat;

import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

class BoatTerminalTest {

    @Test
    void testCLI() {
        assertThat(BoatTerminal.run(new String[]{"-V"}), is(0));
    }
}
