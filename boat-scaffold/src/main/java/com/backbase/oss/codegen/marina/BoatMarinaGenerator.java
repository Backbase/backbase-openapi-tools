package com.backbase.oss.codegen.marina;

import com.backbase.oss.codegen.BoatStaticDocsGenerator;
import com.backbase.oss.codegen.doc.BoatDocsGenerator;
import org.openapitools.codegen.CliOption;
import org.openapitools.codegen.CodegenConstants;
import org.openapitools.codegen.SupportingFile;
import org.openapitools.codegen.templating.HandlebarsEngineAdapter;

import java.io.File;
import java.util.HashMap;

public class BoatMarinaGenerator extends BoatStaticDocsGenerator   {

    public static final String NAME = "boat-marina";

    public BoatMarinaGenerator() {
        super();
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
    public void processOpts() {
        super.processOpts();
         this.supportingFiles.add(new SupportingFile("api.js.handlebars", "api.js"));


        // this.supportingFiles.add(new SupportingFile("css/jsontreeviewer.css", "css/jsontreeviewer.css"));
        // this.supportingFiles.add(new SupportingFile("css/jsonschemaview.css", "css/jsonschemaview.css"));
        // this.supportingFiles.add(new SupportingFile("css/prettify.css", "css/prettify.css"));
        // this.supportingFiles.add(new SupportingFile("css/response.css", "css/response.css"));
        // this.supportingFiles.add(new SupportingFile("css/styles.css", "css/styles.css"));

        // this.supportingFiles.add(new SupportingFile("js/json_stringify_safe.js", "js/json_stringify_safe.js"));
        // this.supportingFiles.add(new SupportingFile("js/js_bootstrap.js", "js/js_bootstrap.js"));
        // this.supportingFiles.add(new SupportingFile("js/jsonformatter.js", "js/jsonformatter.js"));
        // this.supportingFiles.add(new SupportingFile("js/jsonschema-ref-parser.js", "js/jsonschema-ref-parser.js"));
        // this.supportingFiles.add(new SupportingFile("js/jsonschemaview.js", "js/jsonschemaview.js"));
        // this.supportingFiles.add(new SupportingFile("js/jsontreeviewer.js", "js/jsontreeviewer.js"));
        // this.supportingFiles.add(new SupportingFile("js/jsonschemamergeallof.js", "js/jsonschemamergeallof.js"));
        // this.supportingFiles.add(new SupportingFile("js/marked.js", "js/marked.js"));
        // this.supportingFiles.add(new SupportingFile("js/prettify.js", "js/prettify.js"));
        // this.supportingFiles.add(new SupportingFile("js/webfontloader.js", "js/webfontloader.js"));

        // this.supportingFiles.add(new SupportingFile("backbase-logo.svg", "backbase-logo.svg"));

//        this.supportingFiles.add(new SupportingFile("index.js.handlebars", "spec/index.js"));
        // this.supportingFiles.add(new SupportingFile("models.js.handlebars", "spec/models.js"));

//         this.apiTemplateFiles.put("api.js.handlebars", ".js");
        // this.modelTemplateFiles.put("model.js.handlebars", ".js");

//        this.supportingFiles.add(new SupportingFile("api.js.handlebars", "api.js"));

    }

    @Override
    public String getName() {
        return NAME;
    }
}
