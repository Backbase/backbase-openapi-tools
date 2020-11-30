/*
 * Copyright 2018 OpenAPI-Generator Contributors (https://openapi-generator.tech)
 * Copyright 2018 SmartBear Software
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.backbase.oss.codegen.angular;

import io.swagger.v3.oas.models.media.Schema;
import lombok.extern.slf4j.Slf4j;
import org.openapitools.codegen.CodegenModel;
import org.openapitools.codegen.CodegenOperation;
import org.openapitools.codegen.CodegenParameter;
import org.openapitools.codegen.SupportingFile;
import org.openapitools.codegen.utils.ModelUtils;
import org.openapitools.codegen.utils.SemVer;

import java.io.File;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Slf4j
public class BoatAngularGenerator extends BoatAngularAbstractGenerator {

    public BoatAngularGenerator() {
        super();

        this.outputFolder = "generated-code/boat-angular";

        supportsMultipleInheritance = true;

        embeddedTemplateDir = templateDir = "boat-angular";
        modelTemplateFiles.put("model.mustache", ".ts");
        apiTemplateFiles.put("api.service.mustache", ".ts");

    }

    @Override
    protected void addAdditionPropertiesToCodeGenModel(CodegenModel codegenModel, Schema schema) {
        codegenModel.additionalPropertiesType = getTypeDeclaration(ModelUtils.getAdditionalProperties(schema));
        addImport(codegenModel, codegenModel.additionalPropertiesType);
    }

    @Override
    public String getName() {
        return "boat-angular";
    }

    @Override
    public String getHelp() {
        return "Generates a TypeScript Angular (6.x - 10.x) client library compatible with Backbase.";
    }

    @Override
    public void processOpts() {
        super.processOpts();
        supportingFiles.add(new SupportingFile("models.mustache", modelPackage().replace('.', File.separatorChar), "models.ts"));
        supportingFiles.add(new SupportingFile("apis.mustache", apiPackage().replace('.', File.separatorChar), "api.ts"));
        supportingFiles.add(new SupportingFile("index.mustache", getIndexDirectory(), "index.ts"));
        supportingFiles.add(new SupportingFile("api.module.mustache", getIndexDirectory(), "api.module.ts"));
        supportingFiles.add(new SupportingFile("configuration.mustache", getIndexDirectory(), "configuration.ts"));
        supportingFiles.add(new SupportingFile("variables.mustache", getIndexDirectory(), "variables.ts"));
        supportingFiles.add(new SupportingFile("encoder.mustache", getIndexDirectory(), "encoder.ts"));
        supportingFiles.add(new SupportingFile("gitignore", "", ".gitignore"));
        supportingFiles.add(new SupportingFile("git_push.sh.mustache", "", "git_push.sh"));
        supportingFiles.add(new SupportingFile("README.mustache", getIndexDirectory(), "README.md"));

        // determine NG version
        SemVer ngVersion;
        if (additionalProperties.containsKey(NG_VERSION)) {
            ngVersion = new SemVer(additionalProperties.get(NG_VERSION).toString());
        } else {
            ngVersion = new SemVer(this.ngVersion);
            log.info("generating code for Angular {} ...", ngVersion);
            log.info("  (you can select the angular version by setting the additionalProperty ngVersion)");
        }

        if (additionalProperties.containsKey(NPM_NAME)) {
            addNpmPackageGeneration(ngVersion);
        }

        if (additionalProperties.containsKey(STRING_ENUMS)) {
            setStringEnums(Boolean.valueOf(additionalProperties.get(STRING_ENUMS).toString()));
            additionalProperties.put("stringEnums", getStringEnums());
            if (getStringEnums()) {
                classEnumSeparator = "";
            }
        }

        if (additionalProperties.containsKey(WITH_INTERFACES)) {
            boolean withInterfaces = Boolean.parseBoolean(additionalProperties.get(WITH_INTERFACES).toString());
            if (withInterfaces) {
                apiTemplateFiles.put("apiInterface.mustache", "Interface.ts");
            }
        }

        if (additionalProperties.containsKey(USE_SINGLE_REQUEST_PARAMETER)) {
            this.setUseSingleRequestParameter(convertPropertyToBoolean(USE_SINGLE_REQUEST_PARAMETER));
        }
        writePropertyBack(USE_SINGLE_REQUEST_PARAMETER, getUseSingleRequestParameter());

        if (additionalProperties.containsKey(TAGGED_UNIONS)) {
            taggedUnions = Boolean.parseBoolean(additionalProperties.get(TAGGED_UNIONS).toString());
        }

        if (!additionalProperties.containsKey(PROVIDED_IN_ROOT)) {
            additionalProperties.put(PROVIDED_IN_ROOT, true);
        } else {
            additionalProperties.put(PROVIDED_IN_ROOT, Boolean.parseBoolean(
                    additionalProperties.get(PROVIDED_IN_ROOT).toString()
            ));
        }

        if (ngVersion.atLeast("9.0.0")) {
            additionalProperties.put(ENFORCE_GENERIC_MODULE_WITH_PROVIDERS, true);
        } else {
            additionalProperties.put(ENFORCE_GENERIC_MODULE_WITH_PROVIDERS, false);
        }

        additionalProperties.put(NG_VERSION, ngVersion);

        if (additionalProperties.containsKey(API_MODULE_PREFIX)) {
            String apiModulePrefix = additionalProperties.get(API_MODULE_PREFIX).toString();
            validateClassPrefixArgument("ApiModule", apiModulePrefix);

            additionalProperties.put("apiModuleClassName", apiModulePrefix + "ApiModule");
            additionalProperties.put("configurationClassName", apiModulePrefix + "Configuration");
            additionalProperties.put("configurationParametersInterfaceName", apiModulePrefix + "ConfigurationParameters");
            additionalProperties.put("apiModulePrefix", true);
        } else {
            additionalProperties.put("apiModuleClassName", "ApiModule");
            additionalProperties.put("configurationClassName", "Configuration");
            additionalProperties.put("configurationParametersInterfaceName", "ConfigurationParameters");
            additionalProperties.put("apiModulePrefix", false);
        }
        if (additionalProperties.containsKey(SERVICE_SUFFIX)) {
            serviceSuffix = additionalProperties.get(SERVICE_SUFFIX).toString();
            validateClassSuffixArgument("Service", serviceSuffix);
        }
        if (additionalProperties.containsKey(SERVICE_FILE_SUFFIX)) {
            serviceFileSuffix = additionalProperties.get(SERVICE_FILE_SUFFIX).toString();
            validateFileSuffixArgument("Service", serviceFileSuffix);
        }
        if (additionalProperties.containsKey(MODEL_SUFFIX)) {
            modelSuffix = additionalProperties.get(MODEL_SUFFIX).toString();
            validateClassSuffixArgument("Model", modelSuffix);
        }
        if (additionalProperties.containsKey(MODEL_FILE_SUFFIX)) {
            modelFileSuffix = additionalProperties.get(MODEL_FILE_SUFFIX).toString();
            validateFileSuffixArgument("Model", modelFileSuffix);
        }
        if (additionalProperties.containsKey(FILE_NAMING)) {
            this.setFileNaming(additionalProperties.get(FILE_NAMING).toString());
        }

        if (additionalProperties.containsKey(QUERY_PARAM_OBJECT_FORMAT)) {
            setQueryParamObjectFormat((String) additionalProperties.get(QUERY_PARAM_OBJECT_FORMAT));
        }
        additionalProperties.put("isQueryParamObjectFormatDot", getQueryParamObjectFormatDot());
        additionalProperties.put("isQueryParamObjectFormatJson", getQueryParamObjectFormatJson());
        additionalProperties.put("isQueryParamObjectFormatKey", getQueryParamObjectFormatKey());
    }

    @Override
    public void postProcessParameter(CodegenParameter parameter) {
        super.postProcessParameter(parameter);
        parameter.dataType = applyLocalTypeMapping(parameter.dataType);
    }

    @Override
    public Map<String, Object> postProcessOperationsWithModels(Map<String, Object> operations, List<Object> allModels) {
        Map<String, Object> objs = (Map<String, Object>) operations.get("operations");

        // Add filename information for api imports
        objs.put("apiFilename", getApiFilenameFromClassname(objs.get("classname").toString()));

        List<CodegenOperation> ops = (List<CodegenOperation>) objs.get("operation");
        boolean hasSomeFormParams = false;
        for (CodegenOperation op : ops) {
            if (op.getHasFormParams()) {
                hasSomeFormParams = true;
            }
            op.httpMethod = op.httpMethod.toLowerCase(Locale.ENGLISH);


            // Prep a string buffer where we're going to set up our new version of the string.
            StringBuilder pathBuffer = new StringBuilder();
            StringBuilder parameterName = new StringBuilder();
            int insideCurly = 0;

            // Iterate through existing string, one character at a time.
            for (int i = 0; i < op.path.length(); i++) {
                switch (op.path.charAt(i)) {
                    case '{':
                        // We entered curly braces, so track that.
                        insideCurly++;

                        // Add the more complicated component instead of just the brace.
                        pathBuffer.append("${encodeURIComponent(String(");
                        break;
                    case '}':
                        // We exited curly braces, so track that.
                        insideCurly--;

                        // Add the more complicated component instead of just the brace.
                        CodegenParameter parameter = findPathParameterByName(op, parameterName.toString());
                        pathBuffer.append(toParamName(parameterName.toString()));
                        if (parameter != null && parameter.isDateTime) {
                            pathBuffer.append(".toISOString()");
                        }
                        pathBuffer.append("))}");
                        parameterName.setLength(0);
                        break;
                    default:
                        char nextChar = op.path.charAt(i);
                        if (insideCurly > 0) {
                            parameterName.append(nextChar);
                        } else {
                            pathBuffer.append(nextChar);
                        }
                        break;
                }
            }

            // Overwrite path to TypeScript template string, after applying everything we just did.
            op.path = pathBuffer.toString();
        }

        operations.put("hasSomeFormParams", hasSomeFormParams);

        // Add additional filename information for model imports in the services
        List<Map<String, Object>> imports = (List<Map<String, Object>>) operations.get("imports");
        for (Map<String, Object> im : imports) {
            // This property is not used in the templates any more, subject for removal
            im.put("filename", im.get("import"));
            im.put("classname", im.get("classname"));
        }

        return operations;
    }

    public static enum QUERY_PARAM_OBJECT_FORMAT_TYPE {dot, json, key}
}
