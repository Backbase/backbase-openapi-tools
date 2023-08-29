package com.backbase.oss.codegen.swift5;

import org.openapitools.codegen.languages.Swift5ClientCodegen;
import org.openapitools.codegen.meta.GeneratorMetadata;
import org.openapitools.codegen.meta.Stability;

public class BoatSwift5Codegen extends Swift5ClientCodegen {

    private static final String LIBRARY_DBS = "dbsDataProvider";

    /**
     * Constructor for the swift5 language codegen module.
     */
    public BoatSwift5Codegen() {
        super();
        this.useOneOfInterfaces = true;

        generatorMetadata = GeneratorMetadata.newBuilder(generatorMetadata)
                .stability(Stability.STABLE)
                .build();

        apiTemplateFiles.put("api.mustache", ".swift");
        apiTemplateFiles.put("api_parameters.mustache", "RequestParams.swift");

        modelTemplateFiles.put("model.mustache", ".swift");
        embeddedTemplateDir = templateDir = "boat-swift5";
        modelDocTemplateFiles.put("model_doc.mustache", ".md");
        apiDocTemplateFiles.put("api_doc.mustache", ".md");

        supportedLibraries.put(LIBRARY_DBS, "HTTP client: DBSDataProvider");
        setLibrary(LIBRARY_DBS);
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
    }

    @Override
    public void postProcess() {
        System.out.println("################################################################################");
        System.out.println("#              Thanks for using BOAT Swift 5 Generator.                        #");
        System.out.println("################################################################################");
    }

}
