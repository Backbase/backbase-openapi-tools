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
import org.openapitools.codegen.SupportingFile;
import org.openapitools.codegen.meta.features.DocumentationFeature;
import org.openapitools.codegen.utils.ModelUtils;
import org.openapitools.codegen.utils.SemVer;

import java.io.File;

@Slf4j
public class BoatAngularMockGenerator extends BoatAngularAbstractGenerator {

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

        supportingFiles.add(new SupportingFile("apis.mustache", apiPackage().replace('.', File.separatorChar), "api.ts"));
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

    public static enum QUERY_PARAM_OBJECT_FORMAT_TYPE {dot, json, key}

}
