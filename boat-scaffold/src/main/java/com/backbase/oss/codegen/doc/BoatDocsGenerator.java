package com.backbase.oss.codegen.doc;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.node.ArrayNode;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.parameters.RequestBody;
import io.swagger.v3.oas.models.responses.ApiResponse;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.message.BasicNameValuePair;
import org.openapitools.codegen.CliOption;
import org.openapitools.codegen.CodegenConstants;
import org.openapitools.codegen.CodegenModel;
import org.openapitools.codegen.CodegenParameter;
import org.openapitools.codegen.CodegenResponse;

@Slf4j
public class BoatDocsGenerator extends org.openapitools.codegen.languages.StaticHtml2Generator {
    public static final String NAME = "boat-docs";

    protected Boolean generateAliasModel = true;

    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final ObjectReader paramReader = objectMapper.readerFor(new TypeReference<List<String>>() {
    });


    public BoatDocsGenerator() {
        super();
        embeddedTemplateDir = templateDir = NAME;
        cliOptions.add(new CliOption(CodegenConstants.GENERATE_ALIAS_AS_MODEL, CodegenConstants.GENERATE_ALIAS_AS_MODEL));
        additionalProperties.put(CodegenConstants.GENERATE_ALIAS_AS_MODEL, generateAliasModel);
        additionalProperties.put("appName", "OpenAPI Sample");
        additionalProperties.put("appDescription", "A sample OpenAPI server");
        additionalProperties.put("infoUrl", "https://backbase.github.io/backbase-openapi-tools/");
        additionalProperties.put("infoEmail", "oss@backbase.com");
        additionalProperties.put("licenseInfo", "All rights reserved");
        additionalProperties.put("licenseUrl", "http://apache.org/licenses/LICENSE-2.0.html");
        typeAliases = new HashMap<>();
    }


    @Override
    public void preprocessOpenAPI(OpenAPI openAPI) {
        super.preprocessOpenAPI(openAPI);

        // Add responses to additional properties
        if (openAPI.getComponents().getResponses() != null) {
            additionalProperties.put("responses", openAPI.getComponents().getResponses().entrySet().stream()
                .map(this::mapCodegenResponse)
                .collect(Collectors.toList()));
        }

        // Add parameters to additonal properties
        if (openAPI.getComponents().getParameters() != null) {
            Set<String> imports = new HashSet<>();
            additionalProperties.put("parameters", openAPI.getComponents().getParameters().entrySet().stream()
                .map(nameParameter -> mapComponentParameter(imports, nameParameter))
                .collect(Collectors.toList()));
        }

        // Add requests to addtional properties
        if (openAPI.getComponents().getRequestBodies() != null) {
            Set<String> imports = new HashSet<>();
            additionalProperties.put("requestBodies", openAPI.getComponents().getRequestBodies().entrySet().stream()
                .map(namedRequestBody -> mapComponentRequestBody(imports, namedRequestBody))
                .collect(Collectors.toList()));
        }

        if (openAPI.getPaths() != null)
            // Ensure single tags for operations
            openAPI.getPaths().forEach((path, pathItem) -> {
                pathItem.readOperations().forEach(operation -> {
                    if (operation.getTags() != null && operation.getTags().size() > 1) {
                        String tag = operation.getTags().get(operation.getTags().size() -1 );
                        log.warn("Operation: {} contains multiple tags {} which hinders rendering documentation. Rep" +
                            "lacing it with a single tag: {}", operation.getOperationId(), operation.getTags(), tag);
                        operation.tags(Collections.singletonList(tag));
                    }
                });
            });
    }

    private CodegenParameter mapComponentRequestBody(Set<String> imports, java.util.Map.Entry<String, RequestBody> namedRequestBody) {
        String name = namedRequestBody.getKey();
        RequestBody requestBody = namedRequestBody.getValue();
        return fromRequestBody(requestBody, imports, name);
    }

    private CodegenParameter mapComponentParameter(Set<String> imports, java.util.Map.Entry<String, Parameter> nameParameter) {
        Parameter parameter = nameParameter.getValue();
        return fromParameter(parameter, imports);
    }

    private CodegenResponse mapCodegenResponse(java.util.Map.Entry<String, ApiResponse> codeResponse) {
        String responseCode = codeResponse.getKey();
        // try to resolve response code from key. otherwise use default
        responseCode = responseCode.replaceAll("\\D+", "");
        if (responseCode.length() != 3) {
            responseCode = "default";
        }
        ApiResponse response = codeResponse.getValue();
        return fromResponse(responseCode, response);
    }


    @Override
    public String toModelName(String name) {
        String modelName = super.toModelName(name);
        if (!name.equals(modelName)) {
            log.debug("NOT converting toModelName: {} to: {}", name, modelName);
        }
        return name;
    }

    @Override
    public String toVarName(String name) {
        String varName = super.toVarName(name);
        if (!name.equals(varName)) {
            log.debug("NOT converting varName: {} to: {}", name, varName);
        }
        return name;
    }

    @Override
    public String toApiVarName(String name) {
        String apiVarName = super.toApiVarName(name);
        if (!name.equals(apiVarName)) {
            log.debug("NOT converting apiVarName: {} to: {}", name, apiVarName);
        }
        return name;
    }

    @Override
    public String toParamName(String name) {
        String paramName = super.toParamName(name);
        if (!name.equals(paramName)) {
            log.debug("NOT converting apiVarName: {} to: {}", name, paramName);
        }
        return name;
    }

    @Override
    public CodegenParameter fromParameter(Parameter parameter, Set<String> imports) {
        CodegenParameter codegenParameter = super.fromParameter(parameter, imports);
        log.debug("Created CodegenParameter model for parameter: {}", parameter.getName());
        return BoatCodegenParameter.fromCodegenParameter(parameter, codegenParameter, openAPI);
    }

    @Override
    public CodegenParameter fromRequestBody(RequestBody body, Set<String> imports, String bodyParameterName) {
        CodegenParameter codegenParameter = super.fromRequestBody(body, imports, bodyParameterName);
        log.debug("Created CodegenParameter model for request body: {} with bodyParameterName: {}", codegenParameter.baseName, bodyParameterName);
        return BoatCodegenParameter.fromCodegenParameter(codegenParameter, body, openAPI);
    }

    @Override
    public CodegenResponse fromResponse(String responseCode, ApiResponse response) {
        CodegenResponse r = super.fromResponse(responseCode, response);
        r.message = StringUtils.replace(r.message, "`", "\\`");

        return new BoatCodegenResponse(r, responseCode, response, openAPI);
    }

    @Override
    public CodegenModel fromModel(String name, Schema schema) {
        CodegenModel codegenModel = super.fromModel(name, schema);
        log.debug("Created CodegenModel for name: {}, schema: {} resulting in: {}", name, schema.getName(), codegenModel.getName());
        codegenModel.isAlias = false;
        return codegenModel;
    }

    //    @Override
    public void setParameterExampleValue(CodegenParameter codegenParameter, Parameter parameter) {
        super.setParameterExampleValue(codegenParameter, parameter);

        Object example = parameter.getExample();

        if (parameter.getStyle() != null) {
            switch (parameter.getStyle()) {
                case FORM:
                    if (example instanceof ArrayNode && codegenParameter.isQueryParam) {
                        try {
                            List<String> values = paramReader.readValue((ArrayNode) example);
                            List<BasicNameValuePair> params = values.stream()
                                .map(value -> new BasicNameValuePair(codegenParameter.paramName, value))
                                .collect(Collectors.toList());
                            codegenParameter.example = URLEncodedUtils.format(params, Charset.defaultCharset());
                        } catch (IOException e) {
                            log.warn("Failed to format query string parameter: {}", codegenParameter.example);
                        }
                    }
                    break;
                default:
                    break;
            }
        }
    }

    @Override
    public String getName() {
        return NAME;
    }
}