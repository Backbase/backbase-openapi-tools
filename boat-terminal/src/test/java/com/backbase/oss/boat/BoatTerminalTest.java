package com.backbase.oss.boat;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import org.junit.Test;

public class BoatTerminalTest {

    @Test
    public void testCLI() {
        assertThat(new BoatTerminal().run("-V"), is(0));
    }
}
