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
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.parameters.RequestBody;
import io.swagger.v3.oas.models.responses.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.openapitools.codegen.*;
import org.openapitools.codegen.languages.AbstractTypeScriptClientCodegen;
import org.openapitools.codegen.meta.FeatureSet;
import org.openapitools.codegen.meta.features.DocumentationFeature;
import org.openapitools.codegen.utils.ModelUtils;
import org.openapitools.codegen.utils.SemVer;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.apache.commons.lang3.StringUtils.capitalize;
import static org.openapitools.codegen.utils.StringUtils.*;

@Slf4j
public class BoatAngularMockGenerator extends AbstractTypeScriptClientCodegen {

    private static String CLASS_NAME_PREFIX_PATTERN = "^[a-zA-Z0-9]*$";
    private static String CLASS_NAME_SUFFIX_PATTERN = "^[a-zA-Z0-9]*$";
    private static String FILE_NAME_SUFFIX_PATTERN = "^[a-zA-Z0-9.-]*$";

    public static enum QUERY_PARAM_OBJECT_FORMAT_TYPE {dot, json, key};

    private static final String DEFAULT_IMPORT_PREFIX = "./";

    public static final String NPM_REPOSITORY = "npmRepository";
    public static final String NG_VERSION = "ngVersion";
    public static final String FOUNDATION_VERSION = "foundationVersion";
    public static final String FILE_NAMING = "fileNaming";
    public static final String BUILD_DIST = "buildDist";

    protected String foundationVersion = "6.0.0";
    public static final String QUERY_PARAM_OBJECT_FORMAT = "queryParamObjectFormat";

    protected String ngVersion = "10.0.0";
    protected String npmRepository = null;
    private boolean useSingleRequestParameter = true;
    protected String serviceSuffix = "Service";
    protected String serviceFileSuffix = ".service";
    protected String modelSuffix = "";
    protected String modelFileSuffix = "";
    protected String fileNaming = "camelCase";
    protected Boolean stringEnums = false;
    protected QUERY_PARAM_OBJECT_FORMAT_TYPE queryParamObjectFormat = QUERY_PARAM_OBJECT_FORMAT_TYPE.dot;

    private boolean taggedUnions = false;

	private FeatureSet featureSet;

    public BoatAngularMockGenerator() {
        super();

        modifyFeatureSet(features -> features.includeDocumentationFeatures(DocumentationFeature.Readme));

        this.outputFolder = "generated-code/boat-angular-mock";

        supportsMultipleInheritance = true;

        embeddedTemplateDir = templateDir = "boat-angular-mock";
        languageSpecificPrimitives.add("Blob");
        typeMapping.put("file", "Blob");
        apiPackage = "api";
        modelPackage = "model";

        this.cliOptions.add(new CliOption(NPM_REPOSITORY,
                "Use this property to set an url your private npmRepo in the package.json"));
        this.cliOptions.add(new CliOption(NG_VERSION, "The version of Angular. (At least 6.0.0)").defaultValue(this.ngVersion));
        this.cliOptions.add(new CliOption(FOUNDATION_VERSION, "The version of foundation-ang library.").defaultValue(this.foundationVersion));
        this.cliOptions.add(new CliOption(FILE_NAMING, "Naming convention for the output files: 'camelCase', 'kebab-case'.").defaultValue(this.fileNaming));
        this.cliOptions.add(new CliOption(BUILD_DIST, "Path to build package to"));
    }

    @Override
    protected void addAdditionPropertiesToCodeGenModel(CodegenModel codegenModel, Schema schema) {
        codegenModel.additionalPropertiesType = getTypeDeclaration(ModelUtils.getAdditionalProperties(schema));
        addImport(codegenModel, codegenModel.additionalPropertiesType);
    }

    @Override
    public String getName() {
        return "boat-angular-mock";
    }

    @Override
    public String getHelp() {
        return "Generates a TypeScript Angular (6.x - 10.x) mock library.";
    }

    @Override
    public void processOpts() {
        super.processOpts();

        supportingFiles
                .add(new SupportingFile("apis.mustache", apiPackage().replace('.', File.separatorChar), "api.ts"));
        supportingFiles.add(new SupportingFile("index.mustache", getIndexDirectory(), "index.ts"));
        supportingFiles.add(new SupportingFile("gitignore", "", ".gitignore"));
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

        SemVer foundationVersion;
        if (additionalProperties.containsKey(FOUNDATION_VERSION)) {
            foundationVersion = new SemVer(additionalProperties.get(FOUNDATION_VERSION).toString());
        } else {
            foundationVersion = new SemVer(this.foundationVersion);
            log.info("generating code with foundation-ang {} ...", foundationVersion);
            log.info("  (you can select the angular version by setting the additionalProperty foundationVersion)");
        }

        if (additionalProperties.containsKey(NPM_NAME)) {
            addNpmPackageGeneration(ngVersion);
        }

        apiTemplateFiles.put("apiMocks.mustache", ".mocks.ts");

        additionalProperties.put(NG_VERSION, ngVersion);
        additionalProperties.put(FOUNDATION_VERSION, foundationVersion);

        if (!additionalProperties.containsKey(BUILD_DIST)) {
            additionalProperties.put(BUILD_DIST, "dist");
        }
    }

    private void addNpmPackageGeneration(SemVer ngVersion) {

        if (additionalProperties.containsKey(NPM_REPOSITORY)) {
            this.setNpmRepository(additionalProperties.get(NPM_REPOSITORY).toString());
        }

        // Set the typescript version compatible to the Angular version
        if (ngVersion.atLeast("10.0.0")) {
            additionalProperties.put("tsVersion", ">=3.9.2 <4.0.0");
        } else if (ngVersion.atLeast("9.0.0")) {
            additionalProperties.put("tsVersion", ">=3.6.0 <3.8.0");
        } else if (ngVersion.atLeast("8.0.0")) {
            additionalProperties.put("tsVersion", ">=3.4.0 <3.6.0");
        } else if (ngVersion.atLeast("7.0.0")) {
            additionalProperties.put("tsVersion", ">=3.1.1 <3.2.0");
        } else {
            // Angular v6 requires typescript ">=2.7.2 and <2.10.0"
            additionalProperties.put("tsVersion", ">=2.7.2 and <2.10.0");
        }

        // Set the rxJS version compatible to the Angular version
        if (ngVersion.atLeast("10.0.0")) {
            additionalProperties.put("rxjsVersion", "6.6.0");
        } else if (ngVersion.atLeast("9.0.0")) {
            additionalProperties.put("rxjsVersion", "6.5.3");
        } else if (ngVersion.atLeast("8.0.0")) {
            additionalProperties.put("rxjsVersion", "6.5.0");
        } else if (ngVersion.atLeast("7.0.0")) {
            additionalProperties.put("rxjsVersion", "6.3.0");
        } else {
            // Angular v6
            additionalProperties.put("rxjsVersion", "6.1.0");
        }

        supportingFiles.add(new SupportingFile("ng-package.mustache", getIndexDirectory(), "ng-package.json"));

        // Specific ng-packagr configuration
        if (ngVersion.atLeast("10.0.0")) {
            additionalProperties.put("ngPackagrVersion", "10.0.3");
            additionalProperties.put("tsickleVersion", "0.39.1");
        } else if (ngVersion.atLeast("9.0.0")) {
            additionalProperties.put("ngPackagrVersion", "9.0.1");
            additionalProperties.put("tsickleVersion", "0.38.0");
        } else if (ngVersion.atLeast("8.0.0")) {
            additionalProperties.put("ngPackagrVersion", "5.4.0");
            additionalProperties.put("tsickleVersion", "0.35.0");
        } else if (ngVersion.atLeast("7.0.0")) {
            // compatible versions with typescript version
            additionalProperties.put("ngPackagrVersion", "5.1.0");
            additionalProperties.put("tsickleVersion", "0.34.0");
        } else {
            // angular v6
            // compatible versions with typescript version
            additionalProperties.put("ngPackagrVersion", "3.0.6");
            additionalProperties.put("tsickleVersion", "0.32.1");
        }

        // set zone.js version
        if (ngVersion.atLeast("9.0.0")) {
            additionalProperties.put("zonejsVersion", "0.10.2");
        } else if (ngVersion.atLeast("8.0.0")) {
            additionalProperties.put("zonejsVersion", "0.9.1");
        } else {
            // compatible versions to Angular 6+
            additionalProperties.put("zonejsVersion", "0.8.26");
        }

        //Files for building our lib
        supportingFiles.add(new SupportingFile("package.mustache", getIndexDirectory(), "package.json"));
        supportingFiles.add(new SupportingFile("tsconfig.mustache", getIndexDirectory(), "tsconfig.json"));
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
    public void postProcessParameter(CodegenParameter parameter) {
        super.postProcessParameter(parameter);
        parameter.dataType = applyLocalTypeMapping(parameter.dataType);
    }

//    @Override
//    public Map<String, Object> postProcessOperationsWithModels(Map<String, Object> operations, List<Object> allModels) {
//        Map<String, Object> objs = (Map<String, Object>) operations.get("operations");
//        Map<String, Map<String, Object>> pathOperations = new HashMap<String, Map<String, Object>>();
//
//        // Add filename information for api imports
//        objs.put("apiFilename", getApiFilenameFromClassname(objs.get("classname").toString()));
//
//        List<CodegenOperation> ops = (List<CodegenOperation>) objs.get("operation");
//        boolean hasSomeFormParams = false;
//        for (CodegenOperation op : ops) {
//            if (op.getHasFormParams()) {
//                hasSomeFormParams = true;
//            }
//            op.httpMethod = op.httpMethod.toLowerCase(Locale.ENGLISH);
//
//
//            // Prep a string buffer where we're going to set up our new version of the string.
//            StringBuilder pathBuffer = new StringBuilder();
//            StringBuilder parameterName = new StringBuilder();
//            int insideCurly = 0;
//
//            Map<String, Object> pathOp = new HashMap<>();
//            pathOp.put("pathName", removeNonNameElementToCamelCase(op.path.replaceAll("[\\{\\}\\/]", "-")));
//            pathOp.put("pattern", op.path);
//            pathOp.put("hasExamples", op.examples != null && !op.examples.isEmpty());
//            pathOp.put("operations", Arrays.asList(op));
//            pathOperations.merge(op.path, pathOp, (o1, o2) -> {
//                o1.put("operations", Stream.of(
//                    (List<CodegenOperation>) o1.get("operations"),
//                    (List<CodegenOperation>) o2.get("operations")
//                ).flatMap(oper -> oper.stream())
//                .collect(Collectors.toList()));
//                o1.put("hasExamples",
//                    (boolean) o1.get("hasExamples") ||
//                    (boolean) o2.get("hasExamples")
//                );
//
//                return o1;
//            });
//
//
//            // Overwrite path to TypeScript template string, after applying everything we just did.
//            op.path = pathBuffer.toString();
//        }
//
//        operations.put("pathOperations", pathOperations.values());
//        operations.put("hasSomeFormParams", hasSomeFormParams);
//
//        // Add additional filename information for model imports in the services
//        List<Map<String, Object>> imports = (List<Map<String, Object>>) operations.get("imports");
//        for (Map<String, Object> im : imports) {
//            // This property is not used in the templates any more, subject for removal
//            im.put("filename", im.get("import"));
//            im.put("classname", im.get("classname"));
//        }
//
//        return operations;
//    }

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

    public String getNpmRepository() {
        return npmRepository;
    }

    public void setNpmRepository(String npmRepository) {
        this.npmRepository = npmRepository;
    }

    private String getApiFilenameFromClassname(String classname) {
        String name = classname.substring(0, classname.length() - serviceSuffix.length());
        return toApiFilename(name);
    }

    public String removeModelPrefixSuffix(String name) {
        String result = name;
        if (modelSuffix.length() > 0 && result.endsWith(modelSuffix)) {
            result = result.substring(0, result.length() - modelSuffix.length());
        }
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
     * Converts the original name according to the current <code>fileNaming</code> strategy.
     *
     * @param originalName the original name to transform
     * @return the transformed name
     */
    private String convertUsingFileNamingConvention(String originalName) {
        String name = this.removeModelPrefixSuffix(originalName);
        if ("kebab-case".equals(fileNaming)) {
            name = dashize(underscore(name));
        } else {
            name = camelize(name, true);
        }
        return name;
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
