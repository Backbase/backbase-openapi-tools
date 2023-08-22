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
    @Test
    public void testCapitalizeWithDash() {
        assertEquals(boatSwift5Generator.toEnumVarName("ENTRY-NAME",null), "entryName");
    }
    @Test
    public void testCapitalizeWithSpace() {
        assertEquals(boatSwift5Generator.toEnumVarName("ENTRY NAME",null), "entryName");
    }
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
    @Test
    public void testToVarName(){
        assertEquals(boatSwift5Generator.toVarName("something else"), "somethingElse");
    }
}
