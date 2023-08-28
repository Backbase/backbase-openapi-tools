package com.backbase.oss.codegen.swift5;

import io.swagger.v3.oas.models.media.ArraySchema;
import io.swagger.v3.oas.models.media.Schema;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.text.WordUtils;
import org.openapitools.codegen.*;
import org.openapitools.codegen.languages.Swift5ClientCodegen;
import org.openapitools.codegen.meta.GeneratorMetadata;
import org.openapitools.codegen.meta.Stability;
import org.openapitools.codegen.model.ModelMap;
import org.openapitools.codegen.model.ModelsMap;
import org.openapitools.codegen.model.OperationMap;
import org.openapitools.codegen.model.OperationsMap;
import org.openapitools.codegen.utils.CamelizeOption;
import org.openapitools.codegen.utils.ModelUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class BoatSwift5Codegen extends Swift5ClientCodegen {

    protected static final String LIBRARY_DBS = "dbsDataProvider";

    protected boolean nonPublicApi = false;

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
