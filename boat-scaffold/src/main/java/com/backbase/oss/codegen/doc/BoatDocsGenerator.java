package com.backbase.oss.codegen.doc;

import lombok.extern.slf4j.Slf4j;
import org.openapitools.codegen.CliOption;
import org.openapitools.codegen.CodegenConstants;

import java.util.HashMap;

@Slf4j
public class BoatDocsGenerator extends com.backbase.oss.codegen.BoatStaticDocsGenerator {
    public static final String NAME = "boat-docs";


    public BoatDocsGenerator() {
        super();
        embeddedTemplateDir = templateDir = NAME;
        cliOptions.add(new CliOption(CodegenConstants.GENERATE_ALIAS_AS_MODEL, CodegenConstants.GENERATE_ALIAS_AS_MODEL));
        additionalProperties.put(CodegenConstants.GENERATE_ALIAS_AS_MODEL, false);
        additionalProperties.put("appName", "OpenAPI Sample");
        additionalProperties.put("appDescription", "A sample OpenAPI server");
        additionalProperties.put("infoUrl", "https://backbase.github.io/backbase-openapi-tools/");
        additionalProperties.put("infoEmail", "oss@backbase.com");
        additionalProperties.put("licenseInfo", "All rights reserved");
        additionalProperties.put("licenseUrl", "http://apache.org/licenses/LICENSE-2.0.html");
        typeAliases = new HashMap<>();
    }


    @Override
    public String getName() {
        return NAME;
    }
}