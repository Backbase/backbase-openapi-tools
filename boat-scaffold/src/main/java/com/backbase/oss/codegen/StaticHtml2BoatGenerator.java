package com.backbase.oss.codegen;

import io.swagger.v3.oas.models.responses.ApiResponse;
import org.openapitools.codegen.*;

public class StaticHtml2BoatGenerator extends org.openapitools.codegen.languages.StaticHtml2Generator {
    public static final String NAME = "html2-boat";

    protected Boolean generateAliasModel = true;

    public StaticHtml2BoatGenerator() {
        this.embeddedTemplateDir = this.templateDir = NAME;

        this.cliOptions
            .add(new CliOption(CodegenConstants.GENERATE_ALIAS_AS_MODEL, CodegenConstants.GENERATE_ALIAS_AS_MODEL));
        this.additionalProperties.put(CodegenConstants.GENERATE_ALIAS_AS_MODEL, this.generateAliasModel);
    }

    @Override
    public CodegenResponse fromResponse(String responseCode, ApiResponse response) {
        final CodegenResponse r = super.fromResponse(responseCode, response);
        r.message = r.message.replace("`", "\\`");
        return r;
    }

    @Override
    public String getName() {
        return NAME;
    }
}
