package org.openapitools.codegen.languages;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasEntry;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.*;

import io.swagger.v3.oas.models.media.ArraySchema;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import org.checkerframework.checker.units.qual.A;
import org.openapitools.codegen.CodegenModel;
import org.openapitools.codegen.languages.BoatSwift5Codegen;
import org.junit.jupiter.api.Test;
import org.openapitools.codegen.model.ModelMap;
import org.openapitools.codegen.model.ModelsMap;


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

    @Test
    public void testPostProcessAllModels() {
        final BoatSwift5Codegen codegen = new BoatSwift5Codegen();
        Map<String, ModelsMap> models = new HashMap<>();
        final CodegenModel parent = new CodegenModel();
        parent.setImports(new HashSet<>(Arrays.asList("bike", "car")));

        parent.setClassname("parent");
        models.put("parent", createCodegenModelWrapper(parent));

        assertEquals(codegen.postProcessAllModels(models), models);
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
}
