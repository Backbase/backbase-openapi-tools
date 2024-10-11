package com.backbase.oss.codegen.marina;

import com.backbase.oss.codegen.BoatStaticDocsGenerator;
import org.openapitools.codegen.CliOption;
import org.openapitools.codegen.CodegenConstants;
import org.openapitools.codegen.SupportingFile;
import org.openapitools.codegen.templating.HandlebarsEngineAdapter;

import java.util.HashMap;

public class BoatMarinaGenerator extends BoatStaticDocsGenerator {

    public static final String NAME = "boat-marina";

    public BoatMarinaGenerator() {
        super();
        library = NAME;

        this.supportingFiles.clear();
        this.supportingFiles.add(new SupportingFile("api.js.handlebars", "api.js"));

        embeddedTemplateDir = templateDir = NAME;
        cliOptions.add(new CliOption(CodegenConstants.GENERATE_ALIAS_AS_MODEL, CodegenConstants.GENERATE_ALIAS_AS_MODEL));
        additionalProperties.put(CodegenConstants.GENERATE_ALIAS_AS_MODEL, true);
        additionalProperties.put("appName", "BOAT Marina Documentation");
        additionalProperties.put("appDescription", "For a collection of doc(k)s");
        additionalProperties.put("infoUrl", "https://backbase.github.io/backbase-openapi-tools/");
        additionalProperties.put("infoEmail", "oss@backbase.com");
        additionalProperties.put("licenseInfo", "All rights reserved");
        additionalProperties.put("licenseUrl", "http://apache.org/licenses/LICENSE-2.0.html");
        typeAliases = new HashMap<>();
        HandlebarsEngineAdapter templatingEngine = new BoatHandlebarsEngineAdapter();
        setTemplatingEngine(templatingEngine);

    }



    @Override
    public String getName() {
        return NAME;
    }
}
