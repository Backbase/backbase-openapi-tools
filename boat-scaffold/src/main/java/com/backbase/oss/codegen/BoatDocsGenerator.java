package com.backbase.oss.codegen;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.node.ArrayNode;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.parameters.RequestBody;
import io.swagger.v3.oas.models.responses.ApiResponse;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.message.BasicNameValuePair;
import org.openapitools.codegen.CliOption;
import org.openapitools.codegen.CodegenConstants;
import org.openapitools.codegen.CodegenParameter;
import org.openapitools.codegen.CodegenResponse;

@Slf4j
public class BoatDocsGenerator extends org.openapitools.codegen.languages.StaticHtml2Generator {
    protected Boolean generateAliasModel = true;

    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final ObjectReader paramReader = objectMapper.readerFor(new TypeReference<List<String>>() {
    });


    public BoatDocsGenerator() {
        super();
        embeddedTemplateDir = templateDir = "boat-docs";
        cliOptions.add(new CliOption(CodegenConstants.GENERATE_ALIAS_AS_MODEL, CodegenConstants.GENERATE_ALIAS_AS_MODEL));
        additionalProperties.put(CodegenConstants.GENERATE_ALIAS_AS_MODEL, generateAliasModel);
    }

    @Override
    public CodegenParameter fromParameter(Parameter parameter, Set<String> imports) {
        CodegenParameter codegenParameter = super.fromParameter(parameter, imports);
        return BoatCodegenParameter.fromCodegenParameter(parameter, codegenParameter);
    }

    @Override
    public CodegenParameter fromRequestBody(RequestBody body, Set<String> imports, String bodyParameterName) {
        CodegenParameter codegenParameter = super.fromRequestBody(body, imports, bodyParameterName);
        return BoatCodegenParameter.fromCodegenParameter(codegenParameter, body, imports, bodyParameterName, openAPI);
    }

    @Override
    public CodegenResponse fromResponse(String responseCode, ApiResponse response) {
        CodegenResponse r = super.fromResponse(responseCode, response);
        r.message = StringUtils.replace(r.message, "`", "\\`");
        return r;
    }




//    @Override
    public void setParameterExampleValue(CodegenParameter codegenParameter, Parameter parameter) {
        super.setParameterExampleValue(codegenParameter, parameter);

        Object example = parameter.getExample();

        switch (parameter.getStyle()) {
            case FORM:
                if (example instanceof ArrayNode && codegenParameter.isQueryParam) {
                    try {
                        List<String> values = paramReader.readValue((ArrayNode) example);
                        List<BasicNameValuePair> params = values.stream()
                            .map(value -> new BasicNameValuePair(codegenParameter.paramName, value))
                            .collect(Collectors.toList());
                        parameter.setExample(URLEncodedUtils.format(params, Charset.defaultCharset()));
                    } catch (IOException e) {
                        log.warn("Failed to format query string parameter: {}", codegenParameter.example);
                    }
                }
                break;
            default:
                break;
        }
    }
//
//    @Override
//    public void setParameterExampleValue(CodegenParameter codegenParameter, RequestBody requestBody) {
//        super.setParameterExampleValue(codegenParameter, requestBody);
//    }

    @Override
    public String getName() {
        return "boat-docs";
    }
}