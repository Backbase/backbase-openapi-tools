package org.openapitools.codegen.languages;

import io.swagger.parser.OpenAPIParser;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Paths;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.*;

import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.parser.core.models.ParseOptions;

import io.swagger.v3.parser.util.SchemaTypeUtil;
import org.junit.jupiter.api.Test;
import org.openapitools.codegen.CodegenModel;
import org.openapitools.codegen.CodegenProperty;
import org.openapitools.codegen.InlineModelResolver;
import org.openapitools.codegen.model.ModelMap;
import org.openapitools.codegen.model.ModelsMap;
import org.openapitools.codegen.utils.ModelUtils;

import java.lang.reflect.Array;
import java.util.*;

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
    public void testFromModel() {

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
        final CodegenModel cm = boatSwift5CodeGen.fromModel("sample", schema);

        assertEquals(cm.name, "sample");
        assertEquals(cm.classname, "Sample");
        assertEquals(cm.description, "a sample model");
        assertEquals(cm.vars.size(), 6);
        assertEquals(cm.getDiscriminatorName(),"test");

        final CodegenProperty property1 = cm.vars.get(0);
        assertEquals(property1.baseName, "id");
        assertEquals(property1.dataType, "Int64");
        assertEquals(property1.name, "id");
        assertNull(property1.defaultValue);
        assertEquals(property1.baseType, "Int64");
        assertTrue(property1.required);
        assertTrue(property1.isPrimitiveType);
        assertFalse(property1.isContainer);

        final CodegenProperty property2 = cm.vars.get(1);
        assertEquals(property2.baseName, "name");
        assertEquals(property2.dataType, "String");
        assertEquals(property2.name, "name");
        assertNull(property2.defaultValue);
        assertEquals(property2.baseType, "String");
        assertTrue(property2.required);
        assertTrue(property2.isPrimitiveType);
        assertFalse(property2.isContainer);

        final CodegenProperty property3 = cm.vars.get(2);
        assertEquals(property3.baseName, "content");
        assertEquals(property3.dataType, "Any");
        assertEquals(property3.name, "content");
        assertNull(property3.defaultValue);
        assertEquals(property3.baseType, "Any");
        assertFalse(property3.required);
        assertFalse(property3.isContainer);
        assertTrue(property3.isFreeFormObject);

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
    public  void testPostProcessAllModels() {
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
