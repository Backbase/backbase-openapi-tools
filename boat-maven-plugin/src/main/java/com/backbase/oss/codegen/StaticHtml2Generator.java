package com.backbase.oss.codegen;

import io.swagger.v3.oas.models.responses.ApiResponse;
import org.openapitools.codegen.*;

public class StaticHtml2Generator extends org.openapitools.codegen.languages.StaticHtml2Generator {
    protected Boolean generateAliasModel = true;


    public CodegenResponse fromResponse(String responseCode, ApiResponse response) {
        CodegenResponse r = super.fromResponse(responseCode,response);
        r.message = r.message.replace("`", "\\`");
        return r;
    }

    public StaticHtml2Generator() {
        super();
        cliOptions.add(new CliOption(CodegenConstants.GENERATE_ALIAS_AS_MODEL, CodegenConstants.GENERATE_ALIAS_AS_MODEL));
        additionalProperties.put(CodegenConstants.GENERATE_ALIAS_AS_MODEL, generateAliasModel);
    }
}