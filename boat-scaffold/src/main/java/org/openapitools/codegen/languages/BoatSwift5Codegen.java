package org.openapitools.codegen.languages;

import io.swagger.v3.oas.models.media.ArraySchema;
import io.swagger.v3.oas.models.media.Schema;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.WordUtils;
import org.openapitools.codegen.*;
import org.openapitools.codegen.meta.GeneratorMetadata;
import org.openapitools.codegen.meta.Stability;
import org.openapitools.codegen.model.ModelsMap;
import org.openapitools.codegen.utils.ModelUtils;

import java.util.*;
import java.util.regex.Pattern;

public class BoatSwift5Codegen extends Swift5ClientCodegen implements CodegenConfig {
    public static final String LIBRARY_DBS = "dbsDataProvider";
    public static final String DEPENDENCY_MANAGEMENT = "dependenciesAs";
    public static final String DEPENDENCY_MANAGEMENT_PODFILE = "Podfile";
    protected static final String DEPENDENCY_MANAGEMENT_CARTFILE = "Cartfile";
    protected static final String[] DEPENDENCY_MANAGEMENT_OPTIONS = {DEPENDENCY_MANAGEMENT_CARTFILE, DEPENDENCY_MANAGEMENT_PODFILE};
    protected static final String MODULE_NAME = "moduleName";
    protected String[] dependenciesAs = new String[0];

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

        cliOptions.add(new CliOption(DEPENDENCY_MANAGEMENT,
                "Available dependency managers "
                        + StringUtils.join(DEPENDENCY_MANAGEMENT_OPTIONS, ", ")
                        + " are available."));
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

        if (additionalProperties.containsKey(DEPENDENCY_MANAGEMENT)) {
            Object dependenciesAsObject = additionalProperties.get(DEPENDENCY_MANAGEMENT);
            if (dependenciesAsObject instanceof String) {
                setDependenciesAs((WordUtils.capitalizeFully((String) dependenciesAsObject).split(",")));
            }
        }

        additionalProperties.put(DEPENDENCY_MANAGEMENT, dependenciesAs);
        if (ArrayUtils.contains(dependenciesAs, DEPENDENCY_MANAGEMENT_PODFILE)) {
            supportingFiles.add(new SupportingFile("Podfile.mustache",
                    "",
                    DEPENDENCY_MANAGEMENT_PODFILE));
        }
        if (!additionalProperties.containsKey(MODULE_NAME)) {
            additionalProperties.put(MODULE_NAME, sanitize((String) additionalProperties.get(PROJECT_NAME)));
        }
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
        CodegenModel codegenModel = super.fromModel(name, model);
        if (codegenModel.description != null) {
            codegenModel.imports.add("ApiModel");
        }

        fixAllFreeFormObject(codegenModel);

        return codegenModel;
    }

    public void setDependenciesAs(String[] dependenciesAs) {
        this.dependenciesAs = dependenciesAs;
    }


    /*
    This is added as a compatibility requirement for API specs containing free form objects
    missing `additionalProperties` property
     */
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

    /*
    Get the projectName,
    Check if it's ending with API, if yes, strip the API / Api and set moduleName to the stripped string
    Check if it's ending with Client, if yes, stop, fail the whole generation.
     */
    String sanitize(String projectName) {
        String projName = "";
        if (Pattern.matches("\\w.*(API|Api)$", projectName)) {
            projName = Pattern.compile("(API|Api)$").matcher(projectName).replaceAll("");
        } else if (Pattern.matches("\\w.*(CLIENT|client|Client)$", projectName)) {
            throw new RuntimeException(projectName + " is not valid. projectName should end with `API or `Api`");
        } else {
            projName = projectName;
        }
        return projName;
    }

    @Override
    public void postProcess() {
        System.out.println("################################################################################");
        System.out.println("#            Thanks for using BOAT Swift 5 OpenAPI Generator.                   #");
        System.out.println("################################################################################");
    }

}

