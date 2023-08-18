package com.backbase.oss.codegen.swift5;

import org.junit.jupiter.api.Test;
import org.openapitools.codegen.CodegenProperty;

import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;


public class BoatSwift5CodegenTests {
    BoatSwift5Generator boatSwift5Generator = new BoatSwift5Generator();

    @Test
    public void testGeneratorName() {
        assert Objects.equals(boatSwift5Generator.getName(), "boat-swift5");
    }
    @Test
    public void testTag() {
        assertEquals(boatSwift5Generator.getTag().toString(), "CLIENT");
    }
    @Test
    public void testEscapeReservedWord(){
        assertEquals(boatSwift5Generator.escapeReservedWord("String"), "_String");
    }
    @Test
    public void testModelFileFolder() {
        assertEquals(boatSwift5Generator.modelFileFolder(), "generated-code/swift/Classes/OpenAPIs/Models");
    }
    @Test
    public void testApiFileFolder(){
        assertEquals(boatSwift5Generator.apiFileFolder(), "generated-code/swift/Classes/OpenAPIs/APIs");
    }
    @Test
    public void testShouldNotBreakNonReservedWord(){
        assertEquals(boatSwift5Generator.toEnumVarName("Error",null),"error");
    }
    // Original Test https://github.com/OpenAPITools/openapi-generator/blob/master/modules/openapi-generator/src/test/java/org/openapitools/codegen/swift5/Swift5ClientCodegenTest.java#L77
    // return value varies
    @Test
    public void testCapitalizeWithDash() {
        assertEquals(boatSwift5Generator.toEnumVarName("ENTRY-NAME",null), "entryName");
    }
    // Original test https://github.com/OpenAPITools/openapi-generator/blob/master/modules/openapi-generator/src/test/java/org/openapitools/codegen/swift5/Swift5ClientCodegenTest.java#L82
    // return value varies
    @Test
    public void testCapitalizeWithSpace() {
        assertEquals(boatSwift5Generator.toEnumVarName("ENTRY NAME",null), "entryName");
    }
    // Original test https://github.com/OpenAPITools/openapi-generator/blob/master/modules/openapi-generator/src/test/java/org/openapitools/codegen/swift5/Swift5ClientCodegenTest.java#L87
    // return value varies
    @Test
    public void testCapitalizeWithUnderscore() {
        assertEquals(boatSwift5Generator.toEnumVarName("ENTRY_NAME",null), "entryName");
    }
    @Test
    public void testToParamNameReturnsCorrectString(){
        assertEquals(boatSwift5Generator.toParamName("created-at"),"createdAt");
    }
    @Test
    public void testToModelName() {
        assertEquals(boatSwift5Generator.toModelName("Response"), "ModelResponse");
    }
}
