package org.openapitools.codegen.languages;

import io.swagger.v3.oas.models.media.ArraySchema;
import io.swagger.v3.oas.models.media.Schema;
import org.openapitools.codegen.CodegenConfig;
import org.openapitools.codegen.CodegenModel;
import org.openapitools.codegen.CodegenProperty;
import org.openapitools.codegen.SupportingFile;
import org.openapitools.codegen.meta.GeneratorMetadata;
import org.openapitools.codegen.meta.Stability;
import org.openapitools.codegen.model.ModelsMap;
import org.openapitools.codegen.utils.ModelUtils;

import java.util.*;


public class BoatSwift5Codegen extends Swift5ClientCodegen implements CodegenConfig {
    private static final String LIBRARY_DBS = "dbsDataProvider";

    /**
     * Constructor for the BoatSwift5Codegen codegen module.
     */
    public BoatSwift5Codegen() {
        super();
        this.useOneOfInterfaces = true;

        generatorMetadata = GeneratorMetadata.newBuilder(generatorMetadata)
                .stability(Stability.STABLE)
                .build();
        supportedLibraries.put(LIBRARY_DBS, "HTTP client: DBSDataProvider");
        setLibrary(LIBRARY_DBS);

        // Set the default template directory
        embeddedTemplateDir = templateDir = getName();

        // Type mappings //
        this.typeMapping.remove("object");
        this.typeMapping.remove("AnyType");
        this.typeMapping.put("object", "Any");
        this.typeMapping.put("AnyType", "Any");
    }

    @Override
    public String getName() {
        return "boat-swift5";
    }

    @Override
    public String getHelp() {
        return "Generates a BOAT Swift 5.x client library.";
    }

    @Override
    public void processOpts() {
        super.processOpts();
        additionalProperties.put("useDBSDataProvider", getLibrary().equals(LIBRARY_DBS));
        supportingFiles.add(new SupportingFile("AnyCodable.swift.mustache", sourceFolder, "AnyCodable.swift"));
        this.supportingFiles.add(new SupportingFile("AnyCodable.swift.mustache", this.sourceFolder, "AnyCodable.swift"));
    }

    // Fix issues with generating arrays with Set.
    @Override
    public String getTypeDeclaration(Schema p) {
        if (ModelUtils.isArraySchema(p)) {
            ArraySchema ap = (ArraySchema) p;
            Schema inner = ap.getItems();
            return "[" + getTypeDeclaration(inner) + "]";
        }
        return super.getTypeDeclaration(p);
    }

    @Override
    public CodegenModel fromModel(String name, Schema model) {
        Map<String, Schema> allDefinitions = ModelUtils.getSchemas(this.openAPI);
        CodegenModel codegenModel = super.fromModel(name, model);
        if (codegenModel.description != null) {
            codegenModel.imports.add("ApiModel");
        }

        fixAllFreeFormObject(codegenModel);

        return codegenModel;
    }

    private void fixAllFreeFormObject(CodegenModel codegenModel) {
        this.fixFreeFormObject(codegenModel.vars);
        this.fixFreeFormObject(codegenModel.optionalVars);
        this.fixFreeFormObject(codegenModel.requiredVars);
        this.fixFreeFormObject(codegenModel.parentVars);
        this.fixFreeFormObject(codegenModel.allVars);
        this.fixFreeFormObject(codegenModel.readOnlyVars);
        this.fixFreeFormObject(codegenModel.readWriteVars);
    }

    /*
     If a property has both isFreeFormObject and isMapContainer true make isFreeFormObject false
     This way when we have a free form object in the spec that has a typed value it will be
     treated as a Dictionary
    */
    private void fixFreeFormObject(List<CodegenProperty> codegenProperties) {
        for (CodegenProperty codegenProperty : codegenProperties) {
            if (codegenProperty.isFreeFormObject && codegenProperty.isMap && !codegenProperty.items.isFreeFormObject) {
                codegenProperty.isFreeFormObject = false;
            }

            if (codegenProperty.isArray && codegenProperty.items.isFreeFormObject) {
                codegenProperty.isFreeFormObject = true;
                if (codegenProperty.additionalProperties == null) {
                    codegenProperty.isMap = false;
                }
            }
        }
    }

    // Fix for inheritance bug
    @Override
    public Map<String, ModelsMap> postProcessAllModels(Map<String, ModelsMap> objs) {
        Map<String, ModelsMap> postProcessedModels = super.postProcessAllModels(objs);
        Iterator it = postProcessedModels.entrySet().iterator();

        while (it.hasNext()) {
            Map.Entry<String, ModelsMap> entry = (Map.Entry)it.next();
            CodegenModel model = ModelUtils.getModelByName(entry.getKey(), postProcessedModels);
            addParentProperties(model, postProcessedModels);
        }
        return postProcessedModels;
    }

    // Fix for inheritance bug
    private void addParentProperties(CodegenModel model, Map<String, ModelsMap> objs) {
        Set<String> parents = model.allOf;
        if (parents == null || parents.isEmpty()) {
            return;
        }

        for (String parent : parents) {
            CodegenModel parentModel = ModelUtils.getModelByName(parent, objs);
            fixInheritance(model, parentModel);
            Set<String> parentsOfParent = parentModel.allOf;

            if (parentsOfParent != null && !parentsOfParent.isEmpty()) {
                // then recursively add all the parent properties of the parents.
                addParentProperties(parentModel, objs);
            }
        }
    }

    /*
     Fix for inheritance bug
     There is no inheritance for Swift structs, so we're adding all parent vars
     recursively to the models allVars list while making sure we don't have duplicates.
    */
    private void fixInheritance(CodegenModel codegenModel, CodegenModel parentModel) {
        if (parentModel != null) {
            if (!parentModel.allVars.isEmpty()) {
                codegenModel.allVars.addAll(parentModel.allVars);
                codegenModel.requiredVars.addAll(parentModel.requiredVars);
            }
        }
        codegenModel.removeAllDuplicatedProperty();
    }

    @Override
    public void postProcess() {
        System.out.println("################################################################################");
        System.out.println("# Thanks for using BOAT Swift 5 OpenAPI Generator.                                          #");
        System.out.println("################################################################################");
    }

}

