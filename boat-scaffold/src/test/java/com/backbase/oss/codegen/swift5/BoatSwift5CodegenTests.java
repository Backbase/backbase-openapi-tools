package com.backbase.oss.codegen.swift5;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;


public class BoatSwift5CodegenTests {
    BoatSwift5Codegen boatSwift5CodeGen = new BoatSwift5Codegen();

    @Test
    public void testGeneratorName() {
//        assert Objects.equals(boatSwift5CodeGen.getName(), "boat-swift5");
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
    public void testToModelNameWithNumber(){
        assertEquals(boatSwift5CodeGen.toModelName("200Return"), "Model200Return");
    }
    @Test
    public void testToModelNameWithoutTransformation(){
        assertEquals(boatSwift5CodeGen.toModelName("Something"), "Something");
    }
    @Test
    public void testToModelFileName(){
        assertEquals(boatSwift5CodeGen.toModelFilename("Something"), "Something");
    }
    @Test
    public void testToApiNameWithEmptyString(){
        assertEquals(boatSwift5CodeGen.toApiName(""), "DefaultAPI");
    }
    @Test
    public void testToApiNameWithNonEmptyString(){
        assertEquals(boatSwift5CodeGen.toApiName("Pets"), "PetsAPI");
    }
    @Test
    public void testApiDocFileFolder() {
        assertEquals(boatSwift5CodeGen.apiDocFileFolder(),"generated-code/swift/docs/");
    }
    @Test
    public void testModelDocFileFolder(){
        assertEquals(boatSwift5CodeGen.modelDocFileFolder(),"generated-code/swift/docs/");
    }
    @Test
    public void testToModelDocFileName(){
        assertEquals(boatSwift5CodeGen.toModelDocFilename("something"), "Something");
    }
    @Test
    public void testToApiDocFilename(){
        assertEquals(boatSwift5CodeGen.toApiDocFilename("Something"), "SomethingAPI");
    }
    @Test
    public void testToOperationIDEmptyStringID(){
        Exception exp = assertThrows(RuntimeException.class, () -> {
            boatSwift5CodeGen.toOperationId(" ");
        });
        String expectedMessage = "Empty method name (operationId) not allowed";
        String actualMessage = exp.getMessage();

        assertTrue(actualMessage.contains(expectedMessage));
    }
    @Test
    public void testToOperationIdIsReservedWord(){
        assertEquals(boatSwift5CodeGen.toOperationId("return"), "callReturn");
    }
    @Test
    public void testToOperationIDStartsWithNumber(){
        assertEquals(boatSwift5CodeGen.toOperationId("45Return"),"call45return");
    }
    @Test
    public void testToVarNameWithNumbers(){
        assertEquals(boatSwift5CodeGen.toVarName("56Today"),"_56today");
    }
    @Test
    public void testToVarNameWithUppercase(){
        assertEquals(boatSwift5CodeGen.toVarName("TODAY"),"TODAY");
    }
    @Test
    public void testToParamNameWithNumbers(){
        assertEquals(boatSwift5CodeGen.toParamName("56Today"),"_56today");
    }
    @Test
    public void testToParamNameWithUppercase(){
        assertEquals(boatSwift5CodeGen.toParamName("TODAY"),"TODAY");
    }
    @Test
    public void testEscapeQuotationMark(){
        assertEquals(boatSwift5CodeGen.escapeQuotationMark("\"Today\""),"Today");
    }
    @Test
    public void testEscapeUnsafeCharacters(){
        assertEquals(boatSwift5CodeGen.escapeUnsafeCharacters("*/Today"),"*_/Today");
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
