package com.backbase.oss.codegen.doc;

import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.servers.Server;
import java.util.List;
import java.util.Map;
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

        return codegenOperation;
    }

    @Override
    public String getName() {
        return NAME;
    }
}