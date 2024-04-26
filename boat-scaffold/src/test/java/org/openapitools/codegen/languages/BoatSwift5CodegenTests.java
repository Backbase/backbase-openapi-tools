package org.openapitools.codegen.languages;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Paths;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.*;

import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.servers.Server;

import io.swagger.v3.parser.util.SchemaTypeUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.openapitools.codegen.CodegenModel;
import org.openapitools.codegen.CodegenProperty;
import org.openapitools.codegen.model.ModelMap;
import org.openapitools.codegen.model.ModelsMap;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;


public class BoatSwift5CodegenTests {
    BoatSwift5Codegen boatSwift5CodeGen = new BoatSwift5Codegen();


    @Test
    void testGeneratorName() {
        assertEquals(boatSwift5CodeGen.getName(), "boat-swift5");
    }
    @Test
    void testGetHelpMessage() {
        assertEquals(boatSwift5CodeGen.getHelp(), "Generates a BOAT Swift 5.x client library.");
    }

    @Test
    void testProcessOptsSetDBSDataProvider() {
        boatSwift5CodeGen.additionalProperties().put("projectName","SomethingAPI");
        boatSwift5CodeGen.setLibrary(BoatSwift5Codegen.LIBRARY_DBS);
        boatSwift5CodeGen.processOpts();
        boatSwift5CodeGen.postProcess();
        assertEquals(BoatSwift5Codegen.LIBRARY_DBS, boatSwift5CodeGen.getLibrary());
    }

    @ParameterizedTest
    @ValueSource(strings = {BoatSwift5Codegen.DEPENDENCY_MANAGEMENT_PODFILE, "PODFILE", "podFile", "podfile", "podfilE"})
    void testSetDependenciesDoesNotConsiderCase(String arg) {
        boatSwift5CodeGen.additionalProperties().put("projectName","SomethingAPI");
        boatSwift5CodeGen.additionalProperties().put(BoatSwift5Codegen.DEPENDENCY_MANAGEMENT, arg);
        boatSwift5CodeGen.processOpts();

        final String[]  dependenciesAs = (String[]) boatSwift5CodeGen.additionalProperties().get(BoatSwift5Codegen.DEPENDENCY_MANAGEMENT);
        assertEquals(BoatSwift5Codegen.DEPENDENCY_MANAGEMENT_PODFILE, dependenciesAs[0]);
    }

    @Test
    void testWhenDependenciesAsIsNotSetShouldBeEmpty() {
        boatSwift5CodeGen.additionalProperties().put("projectName","SomethingAPI");
        boatSwift5CodeGen.processOpts();
        final String[] dependenciesAs = (String[]) boatSwift5CodeGen.additionalProperties().get(BoatSwift5Codegen.DEPENDENCY_MANAGEMENT);
        assertEquals(0,dependenciesAs.length);
    }

    @Test
    void testGetTypeDeclaration() {

        final ArraySchema childSchema = new ArraySchema().items(new StringSchema());

        assertEquals(boatSwift5CodeGen.getTypeDeclaration(childSchema),"[String]");
    }
    @Test
    void testCapitalizedReservedWord() throws Exception {
        assertEquals(boatSwift5CodeGen.toEnumVarName("AS", null), "_as");
    }

    @Test
    void testReservedWord() throws Exception {
        assertEquals(boatSwift5CodeGen.toEnumVarName("Public", null), "_public");
    }

    @Test
    void shouldNotBreakNonReservedWord() throws Exception {
        assertEquals(boatSwift5CodeGen.toEnumVarName("Error", null), "error");
    }

    @Test
    void shouldNotBreakCorrectName() throws Exception {
        assertEquals(boatSwift5CodeGen.toEnumVarName("EntryName", null), "entryName");
    }

    @Test
    void testSingleWordAllCaps() throws Exception {
        assertEquals(boatSwift5CodeGen.toEnumVarName("VALUE", null), "value");
    }

    @Test
    void testSingleWordLowercase() throws Exception {
       assertEquals(boatSwift5CodeGen.toEnumVarName("value", null), "value");
    }

    @Test
    void testCapitalsWithUnderscore() throws Exception {
        assertEquals(boatSwift5CodeGen.toEnumVarName("ENTRY_NAME", null), "entryName");
    }

    @Test
    void testCapitalsWithDash() throws Exception {
        assertEquals(boatSwift5CodeGen.toEnumVarName("ENTRY-NAME", null), "entryName");
    }

    @Test
    void testCapitalsWithSpace() throws Exception {
        assertEquals(boatSwift5CodeGen.toEnumVarName("ENTRY NAME", null), "entryName");
    }

    @Test
    void testLowercaseWithUnderscore() throws Exception {
        assertEquals(boatSwift5CodeGen.toEnumVarName("entry_name", null), "entryName");
    }

    @Test
    void testStartingWithNumber() throws Exception {
        assertEquals(boatSwift5CodeGen.toEnumVarName("123EntryName", null), "_123entryName");
        assertEquals(boatSwift5CodeGen.toEnumVarName("123Entry_name", null), "_123entryName");
        assertEquals(boatSwift5CodeGen.toEnumVarName("123EntryName123", null), "_123entryName123");
    }

    @Test
    void testSpecialCharacters() throws Exception {
        assertEquals(boatSwift5CodeGen.toEnumVarName("1:1", null), "_1Colon1");
        assertEquals(boatSwift5CodeGen.toEnumVarName("1:One", null), "_1ColonOne");
        assertEquals(boatSwift5CodeGen.toEnumVarName("Apple&Swift", null), "appleAmpersandSwift");
        assertEquals(boatSwift5CodeGen.toEnumVarName("$", null), "dollar");
        assertEquals(boatSwift5CodeGen.toEnumVarName("+1", null), "plus1");
        assertEquals(boatSwift5CodeGen.toEnumVarName(">=", null), "greaterThanOrEqualTo");
    }

    @Test
    void prefixExceptionTest() {

        boatSwift5CodeGen.setModelNamePrefix("API");

        final String result = boatSwift5CodeGen.toModelName("AnyCodable");
        assertEquals(result, "AnyCodable");
    }

    @Test
    void suffixExceptionTest() {

        boatSwift5CodeGen.setModelNameSuffix("API");

        final String result = boatSwift5CodeGen.toModelName("AnyCodable");
        assertEquals(result, "AnyCodable");
    }

    @Test
    void prefixTest() {
        boatSwift5CodeGen.setModelNamePrefix("API");
        final String result = boatSwift5CodeGen.toModelName("MyType");
        assertEquals(result, "APIMyType");
    }

    @Test
    void suffixTest() {
        boatSwift5CodeGen.setModelNameSuffix("API");
        final String result = boatSwift5CodeGen.toModelName("MyType");
        assertEquals(result, "MyTypeAPI");
    }

    @Test
    void testDefaultPodAuthors() throws Exception {
        boatSwift5CodeGen.additionalProperties().put("projectName","SomethingAPI");
        // Given
        // When
        boatSwift5CodeGen.processOpts();
        // Then
        final String podAuthors = (String) boatSwift5CodeGen.additionalProperties().get(Swift5ClientCodegen.POD_AUTHORS);
        assertEquals(podAuthors, Swift5ClientCodegen.DEFAULT_POD_AUTHORS);
    }

    @Test
    void testPodAuthors() throws Exception {
        boatSwift5CodeGen.additionalProperties().put("projectName","SomethingAPI");
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
    void testFromModelCodegenModel() {

        final CodegenModel cm = prepareForModelTests();

        assertEquals(cm.name, "sample");
        assertEquals(cm.classname, "Sample");
        assertEquals(cm.description, "a sample model");
        assertEquals(cm.vars.size(), 6);
        assertEquals(cm.getDiscriminatorName(), "test");
    }

    @Test
    void testFromModelProperty1() {
        final CodegenModel cm = prepareForModelTests();

        final CodegenProperty property1 = cm.vars.get(0);
        assertEquals(property1.baseName, "id");
        assertEquals(property1.dataType, "Int64");
        assertEquals(property1.name, "id");
        assertNull(property1.defaultValue);
        assertEquals(property1.baseType, "Int64");
        assertTrue(property1.required);
        assertTrue(property1.isPrimitiveType);
        assertFalse(property1.isContainer);
    }
    @Test
    void testFromModelProperty2() {
        final CodegenModel cm = prepareForModelTests();

        final CodegenProperty property2 = cm.vars.get(1);
        assertEquals(property2.baseName, "name");
        assertEquals(property2.dataType, "String");
        assertEquals(property2.name, "name");
        assertNull(property2.defaultValue);
        assertEquals(property2.baseType, "String");
        assertTrue(property2.required);
        assertTrue(property2.isPrimitiveType);
        assertFalse(property2.isContainer);
    }
    @Test
    void testFromModelProperty3() {
        final CodegenModel cm = prepareForModelTests();

        final CodegenProperty property3 = cm.vars.get(2);
        assertEquals(property3.baseName, "content");
        assertEquals(property3.dataType, "Any");
        assertEquals(property3.name, "content");
        assertNull(property3.defaultValue);
        assertEquals(property3.baseType, "Any");
        assertFalse(property3.required);
        assertFalse(property3.isContainer);
        assertTrue(property3.isFreeFormObject);
    }
    @Test
    void testFromModelProperty4() {
        final CodegenModel cm = prepareForModelTests();

        final CodegenProperty property4 = cm.vars.get(3);
        assertEquals(property4.baseName, "nested");
        assertEquals(property4.dataType, "[Any]");
        assertEquals(property4.name, "nested");
        assertNull(property4.defaultValue);
        assertEquals(property4.baseType, "Array");
        assertFalse(property4.required);
        assertTrue(property4.isContainer);
        assertTrue(property4.isFreeFormObject);
        assertTrue(property4.items.isFreeFormObject);
    }
    @Test
    void testFromModelProperty5() {
        final CodegenModel cm = prepareForModelTests();

        final CodegenProperty property5 = cm.vars.get(4);
        assertEquals(property5.baseName, "map");
        assertEquals(property5.dataType, "[String: Any]");
        assertEquals(property5.name, "map");
        assertNull(property5.defaultValue);
        assertEquals(property5.baseType, "Dictionary");
        assertFalse(property5.required);
        assertTrue(property5.isContainer);
        assertTrue(property5.isFreeFormObject);
        assertTrue(property5.isMap);
        assertTrue(property5.items.isFreeFormObject);

    }
    @Test
    void testFromModelProperty6() {
        final CodegenModel cm = prepareForModelTests();

        final CodegenProperty property6 = cm.vars.get(5);
        assertEquals(property6.baseName, "secondMap");
        assertEquals(property6.dataType, "[String: String]");
        assertEquals(property6.name, "secondMap");
        assertNull(property6.defaultValue);
        assertEquals(property6.baseType, "Dictionary");
        assertFalse(property6.required);
        assertTrue(property6.isContainer);
        assertTrue(property6.isMap);
        assertFalse(property6.isFreeFormObject);
        assertFalse(property6.items.isFreeFormObject);

    }

    @Test
    void testPostProcessAllModels() {
        final CodegenModel parent = new CodegenModel();
        Map<String, ModelsMap> models = new HashMap<>();

        final CodegenModel parentModel = new CodegenModel();
        parentModel.setName("ActionParent");
        parentModel.classname = "ActionParent";

        final List<CodegenProperty> codegenProperties = new ArrayList<>();

        final CodegenModel actionRecipeItemParentModel = new CodegenModel();
        actionRecipeItemParentModel.setName("ActionRecipeItemParent");

        final Set<String> child = new HashSet<>();
        child.add("ActionRecipeItemParent");

        parent.allOf = child;
        parent.setAllVars(codegenProperties);
        parent.parentModel = parentModel;
        parent.setIsModel(true);
        parent.modelJson = "{'required': ['true'], 'type': 'object', 'properties': {'type':{'maxLength':64, 'minLength':1, type: 'string'}}}";


        models.put("ActionParent", createCodegenModelWrapper(parent));
        models.put("ActionRecipeItemParent", createCodegenModelWrapper(actionRecipeItemParentModel));

        assertEquals(boatSwift5CodeGen.postProcessAllModels(models), models);
    }

    @Test
    void test_processOptsThrowsWhenProjectNameIsInvalid() {
        boatSwift5CodeGen.additionalProperties().put("projectName","SomethingClient");
        RuntimeException exception = assertThrows(RuntimeException.class, () -> boatSwift5CodeGen.processOpts());
        assertEquals("SomethingClient is not valid. projectName should end with `API or `Api`", exception.getMessage());
    }

    @Test
    void test_processOptsSetsStrippedModuleNameIsAProjectNameEndingWithAPIIsProvided() {
        boatSwift5CodeGen.additionalProperties().put("projectName","SomethingAPI");
        String expectedModuleName = "Something";
        boatSwift5CodeGen.processOpts();
        assertEquals(boatSwift5CodeGen.additionalProperties().get("moduleName"), expectedModuleName);
    }

    @Test
    void test_processOptsSetsOriginalProjectNameAsModuleNameIfNameDoesNotEndWithAPIOrClient() {
        String expectedModuleName = "Something";
        boatSwift5CodeGen.additionalProperties().put("projectName",expectedModuleName);
        boatSwift5CodeGen.processOpts();
        assertEquals(boatSwift5CodeGen.additionalProperties().get("moduleName"), expectedModuleName);
    }

    private CodegenModel prepareForModelTests() {

        final ObjectSchema objectSchema = new ObjectSchema();
        objectSchema.setDescription("Sample ObjectSchema");

        final ArraySchema nestedArraySchema = new ArraySchema().items(new ObjectSchema());

        final MapSchema mapSchema = new MapSchema();
        mapSchema.additionalProperties(objectSchema);

        final MapSchema mapSchema1 = new MapSchema();
        mapSchema1.setDescription("Sample ObjectSchema");
        mapSchema1.additionalProperties(new StringSchema());

        final Schema schema = new Schema()
                .description("a sample model")
                .addProperty("id", new IntegerSchema().format(SchemaTypeUtil.INTEGER64_FORMAT))
                .addProperty("name", new StringSchema())
                .addProperty("content", objectSchema)
                .addProperty("nested", nestedArraySchema)
                .addProperty("map", mapSchema)
                .addProperty("secondMap", mapSchema1)
                .addRequiredItem("id")
                .addRequiredItem("name")
                .name("sample")
                .discriminator(new Discriminator().propertyName("test"));

        OpenAPI openAPI = createOpenAPIWithOneSchema("sample", schema);
        boatSwift5CodeGen.setOpenAPI(openAPI);
        return boatSwift5CodeGen.fromModel("sample", schema);
    }

    static ModelsMap createCodegenModelWrapper(CodegenModel cm) {
        ModelsMap objs = new ModelsMap();
        List<ModelMap> modelMaps = new ArrayList<>();
        ModelMap modelMap = new ModelMap();
        modelMap.setModel(cm);
        modelMaps.add(modelMap);
        objs.setModels(modelMaps);
        return objs;
    }

    static OpenAPI createOpenAPIWithOneSchema(String name, Schema schema) {
        OpenAPI openAPI = createOpenAPI();
        openAPI.setComponents(new Components());
        openAPI.getComponents().addSchemas(name, schema);
        return openAPI;
    }
    static OpenAPI createOpenAPI() {
        OpenAPI openAPI = new OpenAPI();
        openAPI.setComponents(new Components());
        openAPI.setPaths(new Paths());

        final Info info = new Info();
        info.setDescription("API under test");
        info.setVersion("1.0.7");
        info.setTitle("My title");
        openAPI.setInfo(info);

        final Server server = new Server();
        server.setUrl("https://localhost:9999/root");
        openAPI.setServers(Collections.singletonList(server));
        return openAPI;
    }
}
