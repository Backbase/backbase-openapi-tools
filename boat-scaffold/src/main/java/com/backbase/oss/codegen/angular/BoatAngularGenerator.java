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

import com.backbase.oss.codegen.doc.BoatCodegenParameter;
import com.backbase.oss.codegen.doc.BoatCodegenResponse;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.parameters.RequestBody;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.servers.Server;
import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import static org.apache.commons.lang3.StringUtils.capitalize;
import org.openapitools.codegen.CliOption;
import org.openapitools.codegen.CodegenModel;
import org.openapitools.codegen.CodegenOperation;
import org.openapitools.codegen.CodegenParameter;
import org.openapitools.codegen.CodegenResponse;
import org.openapitools.codegen.SupportingFile;
import org.openapitools.codegen.languages.AbstractTypeScriptClientCodegen;
import org.openapitools.codegen.meta.features.DocumentationFeature;
import org.openapitools.codegen.utils.ModelUtils;
import org.openapitools.codegen.utils.SemVer;
import static org.openapitools.codegen.utils.StringUtils.camelize;
import static org.openapitools.codegen.utils.StringUtils.underscore;

@Slf4j
public class BoatAngularGenerator extends AbstractTypeScriptClientCodegen {
    public static final String NAME = "boat-angular";

    public static final String NPM_REPOSITORY = "npmRepository";
    public static final String WITH_MOCKS = "withMocks";

    public static final String NG_VERSION = "ngVersion";
    public static final String FOUNDATION_VERSION = "foundationVersion";
    public static final String API_MODULE_PREFIX = "apiModulePrefix";
    public static final String SERVICE_SUFFIX = "serviceSuffix";
    public static final String BUILD_DIST = "buildDist";
    public static final String API_MODULE = "ApiModule";
    private static final String DEFAULT_IMPORT_PREFIX = "./";
    private static final String CLASS_NAME_PREFIX_PATTERN = "^[a-zA-Z0-9]*$";
    private static final String CLASS_NAME_SUFFIX_PATTERN = "^[a-zA-Z0-9]*$";
    public static final String CLASSNAME_KEY = "classname";
    public static final String PATH_NAME_KEY = "pathName";
    public static final String HAS_EXAMPLES = "hasExamples";
    public static final String PATTERN = "pattern";
    protected String foundationVersion = "6.6.7";
    protected String ngVersion = "10.0.0";
    protected String serviceSuffix = "Service";
    protected String serviceFileSuffix = ".service";
    protected String modelFileSuffix = "";

    public BoatAngularGenerator() {
        super();

        modifyFeatureSet(features -> features.includeDocumentationFeatures(DocumentationFeature.Readme));

        this.outputFolder = "generated-code/boat-angular";

        supportsMultipleInheritance = true;

        embeddedTemplateDir = templateDir = NAME;
        modelTemplateFiles.put("model.mustache", ".ts");
        apiTemplateFiles.put("api.service.mustache", ".ts");
        languageSpecificPrimitives.add("Blob");
        typeMapping.put("file", "Blob");
        apiPackage = "api";
        modelPackage = "model";

        this.cliOptions.add(new CliOption(NPM_REPOSITORY,
            "Use this property to set an url your private npmRepo in the package.json"));
        this.cliOptions.add(CliOption.newBoolean(WITH_MOCKS,
            "Setting this property to true will generate mocks out of the examples.",
            false));
        this.cliOptions.add(new CliOption(NG_VERSION, "The version of Angular. (At least 10.0.0)").defaultValue(this.ngVersion));
        this.cliOptions.add(new CliOption(FOUNDATION_VERSION, "The version of foundation-ang library.").defaultValue(this.foundationVersion));
        this.cliOptions.add(new CliOption(API_MODULE_PREFIX, "The prefix of the generated ApiModule."));
        this.cliOptions.add(new CliOption(SERVICE_SUFFIX, "The suffix of the generated service.").defaultValue(this.serviceSuffix));
        this.cliOptions.add(new CliOption(BUILD_DIST, "Path to build package to"));
    }

    @Override
    protected void addAdditionPropertiesToCodeGenModel(CodegenModel codegenModel, Schema schema) {
        codegenModel.additionalPropertiesType = getTypeDeclaration(ModelUtils.getAdditionalProperties(schema));
        addImport(codegenModel, codegenModel.additionalPropertiesType);
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public String getHelp() {
        return "Generates a TypeScript Angular (6.x - 10.x) client library.";
    }


    private void processOpt(String key, Consumer<String> whenProvided) {
        processOpt(key, whenProvided, null);
    }

    private void processOpt(String key, Consumer<String> whenProvided, Runnable notProvided) {
        if (additionalProperties.containsKey(key)) {
            whenProvided.accept(additionalProperties.get(key).toString());
        } else if (notProvided != null) {
            notProvided.run();
        }
    }

    private void processBooleanOpt(String key, Consumer<Boolean> whenProvided) {
        processBooleanOpt(key, whenProvided, null);
    }

    private void processBooleanOpt(String key, Consumer<Boolean> whenProvided, Runnable notProvided) {
        if (additionalProperties.containsKey(key)) {
            whenProvided.accept(convertPropertyToBoolean(key));
        } else if (notProvided != null) {
            notProvided.run();
        }
    }

    @Override
    public void processOpts() {
        super.processOpts();
        addSupportingFiles();

        processOpt(NG_VERSION,
            value -> applyAngularVersion(new SemVer(value)),
            () -> applyAngularVersion(new SemVer(this.ngVersion))
        );

        processOpt(FOUNDATION_VERSION,
            value -> additionalProperties.put(FOUNDATION_VERSION, new SemVer(value)),
            () -> {
                SemVer version = new SemVer(this.foundationVersion);
                additionalProperties.put(FOUNDATION_VERSION, version);
                log.info("generating code with foundation-ang {} ...", version);
                log.info("  (you can select the angular version by setting the additionalProperty foundationVersion)");
            });

        processBooleanOpt(WITH_MOCKS, withMocks -> {
            if (Boolean.TRUE.equals(withMocks)) {
                apiTemplateFiles.put("apiMocks.mustache", ".mocks.ts");
            }
        });

        processOpt(API_MODULE_PREFIX, value -> {
            validateClassPrefixArgument(value);

            additionalProperties.put("apiModuleClassName", value + API_MODULE);
            additionalProperties.put("configurationClassName", value + "Configuration");
            additionalProperties.put("configurationParametersInterfaceName", value + "ConfigurationParameters");
            additionalProperties.put("basePathVariableName", underscore(value).toUpperCase() + "_BASE_PATH");
        }, () -> {
            additionalProperties.put("apiModuleClassName", API_MODULE);
            additionalProperties.put("configurationClassName", "Configuration");
            additionalProperties.put("configurationParametersInterfaceName", "ConfigurationParameters");
            additionalProperties.put("basePathVariableName", "BASE_PATH");
        });
        processOpt(SERVICE_SUFFIX, value -> {
            serviceSuffix = value;
            validateClassSuffixArgument("Service", serviceSuffix);
        });
        processOpt(BUILD_DIST,
            value -> additionalProperties.put(BUILD_DIST, value),
            () -> additionalProperties.put(BUILD_DIST, "dist"));
    }

    private void applyAngularVersion(SemVer angularVersion) {
        if (!angularVersion.atLeast("10.0.0")) {
            throw new IllegalArgumentException("Only angular versions >= 10.0.0 are supported.");
        }

        additionalProperties.put(NG_VERSION, angularVersion);

        if (additionalProperties.containsKey(NPM_NAME)) {
            supportingFiles.add(new SupportingFile("package.mustache", getIndexDirectory(), "package.json"));
            supportingFiles.add(new SupportingFile("tsconfig.mustache", getIndexDirectory(), "tsconfig.json"));
            additionalProperties.put("zonejsVersion", "0.10.2");
            additionalProperties.put("ngPackagrVersion", "10.0.3");
            additionalProperties.put("tsickleVersion", "0.39.1");
            additionalProperties.put("rxjsVersion", "6.6.0");
            additionalProperties.put("tsVersion", ">=3.9.2 <4.0.0");
        }
    }

    private void addSupportingFiles() {
        supportingFiles.add(
            new SupportingFile("models.mustache", modelPackage().replace('.', File.separatorChar), "models.ts"));
        supportingFiles.add(new SupportingFile("public_api.mustache", getIndexDirectory(), "public_api.ts"));
        supportingFiles.add(new SupportingFile("api.module.mustache", getIndexDirectory(), "api.module.ts"));
        supportingFiles.add(new SupportingFile("configuration.mustache", getIndexDirectory(), "configuration.ts"));
        supportingFiles.add(new SupportingFile("variables.mustache", getIndexDirectory(), "variables.ts"));
        supportingFiles.add(new SupportingFile("encoder.mustache", getIndexDirectory(), "encoder.ts"));
        supportingFiles.add(new SupportingFile("gitignore", "", ".gitignore"));
        supportingFiles.add(new SupportingFile("git_push.sh.mustache", "", "git_push.sh"));
        supportingFiles.add(new SupportingFile("README.mustache", getIndexDirectory(), "README.md"));
    }

    private String getIndexDirectory() {
        String indexPackage = modelPackage.substring(0, Math.max(0, modelPackage.lastIndexOf('.')));
        return indexPackage.replace('.', File.separatorChar);
    }

    @Override
    public boolean isDataTypeFile(final String dataType) {
        return dataType != null && dataType.equals("Blob");
    }

    @Override
    public String getTypeDeclaration(Schema p) {
        if (ModelUtils.isFileSchema(p)) {
            return "Blob";
        } else {
            return super.getTypeDeclaration(p);
        }
    }

    private String applyLocalTypeMapping(String type) {
        if (typeMapping.containsKey(type)) {
            type = typeMapping.get(type);
        }
        return type;
    }

    @Override
    public BoatAngularCodegenOperation fromOperation(String path,
                                                     String httpMethod,
                                                     Operation operation,
                                                     List<Server> servers) {
        CodegenOperation codegenOperation = super.fromOperation(path, httpMethod, operation, servers);

        codegenOperation.responses.stream()
            .filter(codegenResponse -> codegenResponse.is2xx)
            .map(codegenResponse -> operation.getResponses().get(codegenResponse.code))
            .forEach(apiResponse -> addProducesReturnType(apiResponse, codegenOperation));

        return new BoatAngularCodegenOperation(codegenOperation);
    }

    private void addProducesReturnType(ApiResponse inputResponse, CodegenOperation codegenOperation) {
        ApiResponse response = ModelUtils.getReferencedApiResponse(this.openAPI, inputResponse);
        if (response == null || response.getContent() == null || response.getContent().isEmpty() || codegenOperation.produces == null) {
            return;
        }

        inputResponse.getContent().forEach((contentType, mediaType) -> {
            if (Objects.isNull(mediaType.getSchema())) return;
            String typeDeclaration = getTypeDeclaration(ModelUtils.unaliasSchema(this.openAPI, mediaType.getSchema()));
            codegenOperation.produces.stream()
                .filter(codegenMediaType -> codegenMediaType.get("mediaType").equals(contentType))
                .forEach(codegenMediaType -> codegenMediaType.put("returnType", typeDeclaration));
        });
    }

    @Override
    public void postProcessParameter(CodegenParameter parameter) {
        super.postProcessParameter(parameter);
        parameter.dataType = applyLocalTypeMapping(parameter.dataType);
    }

    @Override
    public Map<String, Object> postProcessOperationsWithModels(Map<String, Object> operations, List<Object> allModels) {
        String operationKey = "operations";
        Map<String, Object> objs = (Map<String, Object>) operations.get(operationKey);
        Map<String, Map<String, Object>> pathOperations = new HashMap<>();

        // Add filename information for api imports
        objs.put("apiFilename", getApiFilenameFromClassname(objs.get(CLASSNAME_KEY).toString()));

        List<CodegenOperation> ops = (List<CodegenOperation>) objs.get("operation");
        boolean hasSomeFormParams = false;
        for (CodegenOperation op : ops) {
            if (op.getHasFormParams()) {
                hasSomeFormParams = true;
            }
            processCodgenOperation(op, pathOperations, operationKey);
        }

        operations.put("pathOperations", pathOperations.values());
        operations.put("hasSomeFormParams", hasSomeFormParams);

        // Add additional filename information for model imports in the services
        List<Map<String, Object>> imports = (List<Map<String, Object>>) operations.get("imports");
        for (Map<String, Object> im : imports) {
            // This property is not used in the templates any more, subject for removal
            im.put("filename", im.get("import"));
            im.put(CLASSNAME_KEY, im.get(CLASSNAME_KEY));
        }

        return operations;
    }

    private void processCodgenOperation(CodegenOperation op, Map<String, Map<String, Object>> pathOperations, String operationkey) {

        op.httpMethod = op.httpMethod.toLowerCase(Locale.ENGLISH);


        // Prep a string buffer where we're going to set up our new version of the string.
        StringBuilder pathBuffer = new StringBuilder();
        StringBuilder parameterName = new StringBuilder();
        int insideCurly = 0;

        Map<String, Object> pathOp = new HashMap<>();
        pathOp.put(PATH_NAME_KEY, removeNonNameElementToCamelCase(op.path.replaceAll("[{}/]", "-")));
        pathOp.put(PATTERN, op.path);
        pathOp.put(HAS_EXAMPLES, op.examples != null && !op.examples.isEmpty());
        pathOp.put(operationkey, Collections.singletonList(op));
        pathOperations.merge(op.path, pathOp, (o1, o2) -> {
            o1.put(operationkey, Stream.of(
                (List<CodegenOperation>) o1.get(operationkey),
                (List<CodegenOperation>) o2.get(operationkey)
            ).flatMap(Collection::stream)
                .collect(Collectors.toList()));
            o1.put(HAS_EXAMPLES,
                (boolean) o1.get(HAS_EXAMPLES) ||
                    (boolean) o2.get(HAS_EXAMPLES)
            );

            return o1;

        });

        // Iterate through existing string, one character at a time.
        for (int i = 0; i < op.path.length(); i++) {
            switch (op.path.charAt(i)) {
                case '{':
                    // We entered curly braces, so track that.
                    insideCurly++;

                    // Add the more complicated component instead of just the brace.
                    pathBuffer.append("${encodeURIComponent(String(_");
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

    /**
     * Finds and returns a path parameter of an operation by its name
     *
     * @param operation     the operation
     * @param parameterName the name of the parameter
     * @return param
     */
    private CodegenParameter findPathParameterByName(CodegenOperation operation, String parameterName) {
        for (CodegenParameter param : operation.pathParams) {
            if (param.baseName.equals(parameterName)) {
                return param;
            }
        }
        return null;
    }

    @Override
    public Map<String, Object> postProcessModels(Map<String, Object> objs) {
        Map<String, Object> result = super.postProcessModels(objs);
        return postProcessModelsEnum(result);
    }

    @Override
    public Map<String, Object> postProcessAllModels(Map<String, Object> objs) {
        Map<String, Object> result = super.postProcessAllModels(objs);
        for (Map.Entry<String, Object> entry : result.entrySet()) {
            Map<String, Object> inner = (Map<String, Object>) entry.getValue();
            List<Map<String, Object>> models = (List<Map<String, Object>>) inner.get("models");
            for (Map<String, Object> mo : models) {
                CodegenModel cm = (CodegenModel) mo.get("model");
                // Add additional filename information for imports
                Set<String> parsedImports = parseImports(cm);
                mo.put("tsImports", toTsImports(cm, parsedImports));
            }
        }
        return result;
    }

    /**
     * Parse imports
     */
    private Set<String> parseImports(CodegenModel cm) {
        Set<String> newImports = new HashSet<>();
        if (!cm.imports.isEmpty()) {
            for (String name : cm.imports) {
                if (name.contains(" | ")) {
                    String[] parts = name.split(" \\| ");
                    Collections.addAll(newImports, parts);
                } else {
                    newImports.add(name);
                }
            }
        }
        return newImports;
    }

    private List<Map<String, String>> toTsImports(CodegenModel cm, Set<String> imports) {
        List<Map<String, String>> tsImports = new ArrayList<>();
        for (String im : imports) {
            if (!im.equals(cm.classname)) {
                HashMap<String, String> tsImport = new HashMap<>();
                // TVG: This is used as class name in the import statements of the model file
                tsImport.put(CLASSNAME_KEY, im);
                tsImport.put("filename", toModelFilename(removeModelPrefixSuffix(im)));
                tsImports.add(tsImport);
            }
        }
        return tsImports;
    }

    @Override
    public String toApiName(String name) {
        if (name.length() == 0) {
            return "DefaultService";
        }
        return camelize(name) + serviceSuffix;
    }

    @Override
    public String toApiFilename(String name) {
        if (name.length() == 0) {
            return "default.service";
        }
        return this.convertUsingFileNamingConvention(name) + serviceFileSuffix;
    }

    @Override
    public String toApiImport(String name) {
        if (importMapping.containsKey(name)) {
            return importMapping.get(name);
        }
        return apiPackage() + "/" + toApiFilename(name);
    }

    @Override
    public String toModelFilename(String name) {
        if (importMapping.containsKey(name)) {
            return importMapping.get(name);
        }
        return DEFAULT_IMPORT_PREFIX + this.convertUsingFileNamingConvention(this.sanitizeName(name)) + modelFileSuffix;
    }

    @Override
    public String toModelImport(String name) {
        if (importMapping.containsKey(name)) {
            return importMapping.get(name);
        }
        return modelPackage() + "/" + toModelFilename(name).substring(DEFAULT_IMPORT_PREFIX.length());
    }

    private String getApiFilenameFromClassname(String classname) {
        String name = classname.substring(0, classname.length() - serviceSuffix.length());
        return toApiFilename(name);
    }


    public String removeModelPrefixSuffix(String name) {
        String result = name;
        String prefix = capitalize(this.modelNamePrefix);
        String suffix = capitalize(this.modelNameSuffix);
        if (prefix.length() > 0 && result.startsWith(prefix)) {
            result = result.substring(prefix.length());
        }
        if (suffix.length() > 0 && result.endsWith(suffix)) {
            result = result.substring(0, result.length() - suffix.length());
        }

        return result;
    }

    /**
     * Validates that the given string value only contains alpha numeric characters.
     * Throws an IllegalArgumentException, if the string contains any other characters.
     *
     * @param value The value that is being validated.
     */
    private void validateClassPrefixArgument(String value) {
        if (!value.matches(CLASS_NAME_PREFIX_PATTERN)) {
            throw new IllegalArgumentException(
                String.format(Locale.ROOT, "%s class prefix only allows alphanumeric characters.", API_MODULE)
            );
        }
    }

    /**
     * Validates that the given string value only contains alpha numeric characters.
     * Throws an IllegalArgumentException, if the string contains any other characters.
     *
     * @param argument The name of the argument being validated. This is only used for displaying an error message.
     * @param value    The value that is being validated.
     */
    private void validateClassSuffixArgument(String argument, String value) {
        if (!value.matches(CLASS_NAME_SUFFIX_PATTERN)) {
            throw new IllegalArgumentException(
                String.format(Locale.ROOT, "%s class suffix only allows alphanumeric characters.", argument)
            );
        }
    }

    /**
     * Converts the original name to camelCase
     *
     * @param originalName the original name to transform
     * @return the transformed name
     */
    private String convertUsingFileNamingConvention(String originalName) {
        String name = this.removeModelPrefixSuffix(originalName);
        return camelize(name, true);
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

}
