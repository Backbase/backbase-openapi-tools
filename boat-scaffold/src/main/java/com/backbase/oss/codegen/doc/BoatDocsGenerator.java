package com.backbase.oss.codegen.doc;

import com.backbase.oss.codegen.utils.DeprecationExtensions;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.servers.Server;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.openapitools.codegen.CliOption;
import org.openapitools.codegen.CodegenConstants;

import java.util.HashMap;
import org.openapitools.codegen.CodegenOperation;

@Slf4j
public class BoatDocsGenerator extends com.backbase.oss.codegen.BoatStaticDocsGenerator {
    public static final String NAME = "boat-docs";


    public BoatDocsGenerator() {
        super();
        embeddedTemplateDir = templateDir = NAME;
        cliOptions.add(new CliOption(CodegenConstants.GENERATE_ALIAS_AS_MODEL, CodegenConstants.GENERATE_ALIAS_AS_MODEL));
        additionalProperties.put(CodegenConstants.GENERATE_ALIAS_AS_MODEL, true);
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
        boolean specDeprecated = DeprecationExtensions.isSpecDeprecated(openAPI.getInfo());
        Optional<LocalDate> sunsetDate = DeprecationExtensions.getSunsetDate(openAPI.getInfo());
        additionalProperties.put("boatApiDeprecated", specDeprecated);
        additionalProperties.put("boatApiDeprecationMessage",
            DeprecationExtensions.buildDeprecationMessage(sunsetDate));
    }

    @Override
    public CodegenOperation fromOperation(String path, String httpMethod, Operation operation, List<Server> servers) {
        CodegenOperation codegenOperation = super.fromOperation(path, httpMethod, operation, servers);
        boolean isMultipleAccessControlPermission = codegenOperation.vendorExtensions.containsKey("x-BbAccessControls");
        codegenOperation.vendorExtensions.put("hasMultipleAccessControlPermissions", isMultipleAccessControlPermission);
        if (isMultipleAccessControlPermission) {
            try {
                Map<String, Object> accessControlInfo = (Map<String, Object>)codegenOperation.vendorExtensions.get("x-BbAccessControls");

                if (accessControlInfo.containsKey("permissions")) {
                    List<Map<String, Object>> permissions = (List<Map<String, Object>>) accessControlInfo.get("permissions");

                    for (int p = 0; p < permissions.size(); p++) {
                        permissions.get(p).put("letter", (char)('a' + p));
                    }
                }
            } catch (Exception e) {
                log.warn("Unable to add index to access control BOAT docs", e);
            }
        }

        applyDeprecationIfNeeded(codegenOperation);
        return codegenOperation;
    }

    private void applyDeprecationIfNeeded(CodegenOperation codegenOperation) {
        Boolean specDeprecated = (Boolean) additionalProperties.get("boatApiDeprecated");
        if (specDeprecated != null && specDeprecated) {
            codegenOperation.isDeprecated = true;
            String message = (String) additionalProperties.get("boatApiDeprecationMessage");
            if (message != null && !codegenOperation.vendorExtensions.containsKey(DeprecationExtensions.X_BOAT_DEPRECATION_MESSAGE)) {
                codegenOperation.vendorExtensions.put(DeprecationExtensions.X_BOAT_DEPRECATION_MESSAGE, message);
            }
        }
    }

    @Override
    public String getName() {
        return NAME;
    }
}