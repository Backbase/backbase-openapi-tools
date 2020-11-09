package com.backbase.oss.codegen;

import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.responses.ApiResponse;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.openapitools.codegen.CliOption;
import org.openapitools.codegen.CodegenConstants;
import org.openapitools.codegen.CodegenParameter;
import org.openapitools.codegen.CodegenResponse;

@Slf4j
public class BoatStaticHtml2Generator extends org.openapitools.codegen.languages.StaticHtml2Generator {
    protected Boolean generateAliasModel = true;


    public CodegenResponse fromResponse(String responseCode, ApiResponse response) {
        CodegenResponse r = super.fromResponse(responseCode, response);
        r.message = StringUtils.replace(r.message, "`", "\\`");
        return r;
    }

    public BoatStaticHtml2Generator() {
        super();
        cliOptions.add(new CliOption(CodegenConstants.GENERATE_ALIAS_AS_MODEL, CodegenConstants.GENERATE_ALIAS_AS_MODEL));
        additionalProperties.put(CodegenConstants.GENERATE_ALIAS_AS_MODEL, generateAliasModel);
    }

    @Override
    public CodegenParameter fromParameter(Parameter parameter, Set<String> imports) {

        CodegenParameter codegenParameter = super.fromParameter(parameter, imports);
        return BoatCodegenParameter.fromCodegenParameter(parameter, codegenParameter);
    }

}