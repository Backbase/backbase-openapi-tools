package com.backbase.oss.codegen.swift5;

import org.junit.jupiter.api.Test;

import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;


public class BoatSwift5CodegenTests {
    BoatSwift5CodeGen boatSwift5CodeGen = new BoatSwift5CodeGen();

    @Test
    public void testGeneratorName() {
        assert Objects.equals(boatSwift5CodeGen.getName(), "boat-swift5");
    }
    @Test
    public void testTag() {
        assertEquals(boatSwift5CodeGen.getTag().toString(), "CLIENT");
    }
    @Test
    public void testEscapeReservedWord(){
        assertEquals(boatSwift5CodeGen.escapeReservedWord("String"), "_String");
    }
    @Test
    public void testModelFileFolder() {
        assertEquals(boatSwift5CodeGen.modelFileFolder(), "generated-code/swift/Classes/OpenAPIs/Models");
    }
    @Test
    public void testApiFileFolder(){
        assertEquals(boatSwift5CodeGen.apiFileFolder(), "generated-code/swift/Classes/OpenAPIs/APIs");
    }
    @Test
    public void testShouldNotBreakNonReservedWord(){
        assertEquals(boatSwift5CodeGen.toEnumVarName("Error",null),"error");
    }
    @Test
    public void testCapitalizeWithDash() {
        assertEquals(boatSwift5CodeGen.toEnumVarName("ENTRY-NAME",null), "entryName");
    }
    @Test
    public void testCapitalizeWithSpace() {
        assertEquals(boatSwift5CodeGen.toEnumVarName("ENTRY NAME",null), "entryName");
    }
    @Test
    public void testCapitalizeWithUnderscore() {
        assertEquals(boatSwift5CodeGen.toEnumVarName("ENTRY_NAME",null), "entryName");
    }
    @Test
    public void testToParamNameReturnsCorrectString(){
        assertEquals(boatSwift5CodeGen.toParamName("created-at"),"createdAt");
    }
    @Test
    public void testToModelName() {
        assertEquals(boatSwift5CodeGen.toModelName("Response"), "ModelResponse");
    }
    @Test
    public void testToVarName(){
        assertEquals(boatSwift5CodeGen.toVarName("something else"), "somethingElse");
    }
}
