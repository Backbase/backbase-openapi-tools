package com.backbase.oss.codegen.swift5;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasEntry;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Map;

import com.backbase.oss.codegen.swift5.BoatSwift5Codegen;
import org.junit.jupiter.api.Test;


public class BoatSwift5CodegenTests {
    BoatSwift5Codegen boatSwift5CodeGen = new BoatSwift5Codegen();

    @Test
    public void testGeneratorName() {
        assertEquals(boatSwift5CodeGen.getName(), "boat-swift5");
    }
    @Test
    public void testGetHelpMessage() {
        assertEquals(boatSwift5CodeGen.getHelp(), "Generates a BOAT Swift 5.x client library.");
    }

    @Test
    public void testProcessOptsSetDBSDataProvider() {
        final BoatSwift5Codegen gen = new BoatSwift5Codegen();
        final Map<String, Object> options = gen.additionalProperties();
        gen.setLibrary("dbsDataProvider");

        gen.processOpts();
        gen.postProcess();

        assertThat(gen.additionalProperties(), hasEntry("useDBSDataProvider", true));
    }
}
