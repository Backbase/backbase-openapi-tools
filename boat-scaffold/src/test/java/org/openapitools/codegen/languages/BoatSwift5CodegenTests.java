package org.openapitools.codegen.languages;

//import static org.hamcrest.MatcherassertThat;
//import static org.hamcrest.Matchers.hasEntry;

import com.google.common.collect.Sets;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.media.ArraySchema;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import org.openapitools.codegen.*;

import org.junit.jupiter.api.Test;
import org.openapitools.codegen.model.ModelMap;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;


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
        
        boatSwift5CodeGen.setLibrary("dbsDataProvider");

        boatSwift5CodeGen.processOpts();
        boatSwift5CodeGen.postProcess();

//        assertThat(boatSwift5CodeGen.additionalProperties(), hasEntry("useDBSDataProvider", true));

    }
    @Test
    public void testGetTypeDeclaration() {

        final ArraySchema childSchema = new ArraySchema().items(new StringSchema());

        assertEquals(boatSwift5CodeGen.getTypeDeclaration(childSchema),"[String]");
    }
    @Test
    public void testCapitalizedReservedWord() throws Exception {
        assertEquals(boatSwift5CodeGen.toEnumVarName("AS", null), "_as");
    }

    @Test
    public void testReservedWord() throws Exception {
        assertEquals(boatSwift5CodeGen.toEnumVarName("Public", null), "_public");
    }

    @Test
    public void shouldNotBreakNonReservedWord() throws Exception {
        assertEquals(boatSwift5CodeGen.toEnumVarName("Error", null), "error");
    }

    @Test
    public void shouldNotBreakCorrectName() throws Exception {
        assertEquals(boatSwift5CodeGen.toEnumVarName("EntryName", null), "entryName");
    }

    @Test
    public void testSingleWordAllCaps() throws Exception {
        assertEquals(boatSwift5CodeGen.toEnumVarName("VALUE", null), "value");
    }

    @Test
    public void testSingleWordLowercase() throws Exception {
       assertEquals(boatSwift5CodeGen.toEnumVarName("value", null), "value");
    }

    @Test
    public void testCapitalsWithUnderscore() throws Exception {
        assertEquals(boatSwift5CodeGen.toEnumVarName("ENTRY_NAME", null), "entryName");
    }

    @Test
    public void testCapitalsWithDash() throws Exception {
        assertEquals(boatSwift5CodeGen.toEnumVarName("ENTRY-NAME", null), "entryName");
    }

    @Test
    public void testCapitalsWithSpace() throws Exception {
        assertEquals(boatSwift5CodeGen.toEnumVarName("ENTRY NAME", null), "entryName");
    }

    @Test
    public void testLowercaseWithUnderscore() throws Exception {
        assertEquals(boatSwift5CodeGen.toEnumVarName("entry_name", null), "entryName");
    }

    @Test
    public void testStartingWithNumber() throws Exception {
        assertEquals(boatSwift5CodeGen.toEnumVarName("123EntryName", null), "_123entryName");
        assertEquals(boatSwift5CodeGen.toEnumVarName("123Entry_name", null), "_123entryName");
        assertEquals(boatSwift5CodeGen.toEnumVarName("123EntryName123", null), "_123entryName123");
    }

    @Test
    public void testSpecialCharacters() throws Exception {
        assertEquals(boatSwift5CodeGen.toEnumVarName("1:1", null), "_1Colon1");
        assertEquals(boatSwift5CodeGen.toEnumVarName("1:One", null), "_1ColonOne");
        assertEquals(boatSwift5CodeGen.toEnumVarName("Apple&Swift", null), "appleAmpersandSwift");
        assertEquals(boatSwift5CodeGen.toEnumVarName("$", null), "dollar");
        assertEquals(boatSwift5CodeGen.toEnumVarName("+1", null), "plus1");
        assertEquals(boatSwift5CodeGen.toEnumVarName(">=", null), "greaterThanOrEqualTo");
    }

    @Test
    public void prefixExceptionTest() {

        boatSwift5CodeGen.setModelNamePrefix("API");

        final String result = boatSwift5CodeGen.toModelName("AnyCodable");
        assertEquals(result, "AnyCodable");
    }

    @Test
    public void suffixExceptionTest() {

        boatSwift5CodeGen.setModelNameSuffix("API");

        final String result = boatSwift5CodeGen.toModelName("AnyCodable");
        assertEquals(result, "AnyCodable");
    }

    @Test
    public void prefixTest() {

        boatSwift5CodeGen.setModelNamePrefix("API");

        final String result = boatSwift5CodeGen.toModelName("MyType");
        assertEquals(result, "APIMyType");
    }

    @Test
    public void suffixTest() {

        boatSwift5CodeGen.setModelNameSuffix("API");

        final String result = boatSwift5CodeGen.toModelName("MyType");
        assertEquals(result, "MyTypeAPI");
    }

    @Test
    public void testDefaultPodAuthors() throws Exception {
        // Given

        // When
        boatSwift5CodeGen.processOpts();

        // Then
        final String podAuthors = (String) boatSwift5CodeGen.additionalProperties().get(Swift5ClientCodegen.POD_AUTHORS);
        assertEquals(podAuthors, Swift5ClientCodegen.DEFAULT_POD_AUTHORS);
    }

    @Test
    public void testPodAuthors() throws Exception {
        // Given
        final String openAPIDevs = "OpenAPI Devs";
        boatSwift5CodeGen.additionalProperties().put(Swift5ClientCodegen.POD_AUTHORS, openAPIDevs);

        // When
        boatSwift5CodeGen.processOpts();

        // Then
        final String podAuthors = (String) boatSwift5CodeGen.additionalProperties().get(Swift5ClientCodegen.POD_AUTHORS);
        assertEquals(podAuthors, openAPIDevs);
    }
    @Test
    public void binaryDataTest() {
        // TODO update json file

        final OpenAPI openAPI = TestUtils.parseFlattenSpec("src/test/resources/boat-swift5/2_0/binaryDataTest.json");
//        final DefaultCodegen codegen = new Swift5ClientCodegen();
        boatSwift5CodeGen.setOpenAPI(openAPI);
        final String path = "/tests/binaryResponse";
        final Operation p = openAPI.getPaths().get(path).getPost();
        final CodegenOperation op = boatSwift5CodeGen.fromOperation(path, "post", p, null);

        assertEquals(op.returnType, "URL");
        assertEquals(op.bodyParam.dataType, "URL");
        assertTrue(op.bodyParam.isBinary);
        assertTrue(op.responses.get(0).isBinary);
    }

    @Test
    public void dateDefaultTest() {
        final OpenAPI openAPI = TestUtils.parseFlattenSpec("src/test/resources/boat-swift5/2_0/datePropertyTest.json");

        boatSwift5CodeGen.setOpenAPI(openAPI);
        final String path = "/tests/dateResponse";
        final Operation p = openAPI.getPaths().get(path).getPost();
        final CodegenOperation op = boatSwift5CodeGen.fromOperation(path, "post", p, null);

        assertEquals(op.returnType, "Date");
        assertEquals(op.bodyParam.dataType, "Date");
    }
    @Test
    public void oneOfFormParameterTest() {
        final OpenAPI openAPI = TestUtils.parseFlattenSpec("src/test/resources/boat-swift5/3_0/issue_15511.yaml");

        boatSwift5CodeGen.setOpenAPI(openAPI);
        boatSwift5CodeGen.processOpts();
        final String path = "/as/token.oauth2";
        final Operation p = openAPI.getPaths().get(path).getPost();
        final CodegenOperation op = boatSwift5CodeGen.fromOperation(path, "post", p, null);

        assertEquals(op.formParams.size(), 6);

        assertEquals(op.formParams.get(0).baseName, "client_id");
        assertEquals(op.formParams.get(1).baseName, "grant_type");
        assertEquals(op.formParams.get(2).baseName, "password");
        assertEquals(op.formParams.get(3).baseName, "scope");
        assertEquals(op.formParams.get(4).baseName, "username");
        assertEquals(op.formParams.get(5).baseName, "refresh_token");

        assertEquals(op.formParams.get(0).required, false);
        assertEquals(op.formParams.get(1).required, false);
        assertEquals(op.formParams.get(2).required, true);
        assertEquals(op.formParams.get(3).required, true);
        assertEquals(op.formParams.get(4).required, true);
        assertEquals(op.formParams.get(5).required, false);

    }

    @Test
    public void testNestedReadonlySchemas() {
        final OpenAPI openAPI = TestUtils.parseFlattenSpec("src/test/resources/boat-swift5/3_0/allOf-readonly.yaml");

        boatSwift5CodeGen.processOpts();
        boatSwift5CodeGen.setOpenAPI(openAPI);
        final Map<String, Schema> schemaBefore = openAPI.getComponents().getSchemas();
        assertEquals(schemaBefore.keySet(), Sets.newHashSet("club", "owner"));
    }

    @Test
    public void testNestedNullableSchemas() {
        final OpenAPI openAPI = TestUtils.parseFlattenSpec("src/test/resources/boat-swift5/3_0/allOf-nullable.yaml");

        boatSwift5CodeGen.processOpts();
        boatSwift5CodeGen.setOpenAPI(openAPI);
        final Map<String, Schema> schemaBefore = openAPI.getComponents().getSchemas();
        assertEquals(schemaBefore.keySet(), Sets.newHashSet("club", "owner"));
    }
    @Test
    public void allOfDuplicatedPropertiesTest() {
        final OpenAPI openAPI = TestUtils.parseFlattenSpec("src/test/resources/boat-swift5/3_0/allOfDuplicatedProperties.yaml");

        final Schema schema = openAPI.getComponents().getSchemas().get("ModelC");
        boatSwift5CodeGen.setOpenAPI(openAPI);
        CodegenModel modelC = boatSwift5CodeGen.fromModel("ModelC", schema);
        assertNotNull(modelC);
        assertEquals(modelC.getVars().size(), 5);

        CodegenProperty cp0 = modelC.getVars().get(0);
        assertEquals(cp0.name, "foo");

        CodegenProperty cp1 = modelC.getVars().get(1);
        assertEquals(cp1.name, "duplicatedOptional");

        CodegenProperty cp2 = modelC.getVars().get(2);
        assertEquals(cp2.name, "duplicatedRequired");

        CodegenProperty cp3 = modelC.getVars().get(3);
        assertEquals(cp3.name, "bar");

        CodegenProperty cp4 = modelC.getVars().get(4);
        assertEquals(cp4.name, "baz");
    }


}
