package org.openapitools.codegen.languages;

import io.swagger.v3.oas.models.media.ArraySchema;
import io.swagger.v3.oas.models.media.Schema;
import org.openapitools.codegen.CodegenConfig;
import org.openapitools.codegen.CodegenModel;
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
}

