package com.backbase.oss.boat;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.Test;

public class BoatTerminalTest {

    @Test
    public void testCLI() {
        assertThat(BoatTerminal.run(new String[] {"-V"}), is(0));
    }
}
