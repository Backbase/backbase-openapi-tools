package org.openapitools.codegen.languages;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasEntry;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;
import java.util.Map;

import io.swagger.v3.oas.models.media.ArraySchema;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import org.checkerframework.checker.units.qual.A;
import org.openapitools.codegen.languages.BoatSwift5Codegen;
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
        gen.setLibrary("dbsDataProvider");

        gen.processOpts();
        gen.postProcess();

        assertThat(gen.additionalProperties(), hasEntry("useDBSDataProvider", true));
    }
    @Test
    public void testGetTypeDeclaration() {

        final ArraySchema childSchema = new ArraySchema().items(new StringSchema());

        final BoatSwift5Codegen codegen = new BoatSwift5Codegen();

        assertEquals(codegen.getTypeDeclaration(childSchema),"[String]");
    }
}
