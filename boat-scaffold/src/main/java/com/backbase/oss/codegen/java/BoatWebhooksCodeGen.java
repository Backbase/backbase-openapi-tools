package com.backbase.oss.codegen.java;

import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.servers.Server;
import lombok.extern.slf4j.Slf4j;
import org.openapitools.codegen.CliOption;
import org.openapitools.codegen.CodegenOperation;
import org.openapitools.codegen.SupportingFile;

import java.io.File;
import java.util.List;

import static org.openapitools.codegen.utils.StringUtils.camelize;

@Slf4j
public class BoatWebhooksCodeGen extends BoatSpringCodeGen {
    public static final String NAME = "boat-webhooks";

    public static final String USE_CLASS_LEVEL_BEAN_VALIDATION = "useClassLevelBeanValidation";
    public static final String ADD_SERVLET_REQUEST = "addServletRequest";
    public static final String ADD_BINDING_RESULT = "addBindingResult";
    public static final String USE_LOMBOK_ANNOTATIONS = "useLombokAnnotations";
    public static final String USE_WITH_MODIFIERS = "useWithModifiers";
    public static final String USE_PROTECTED_FIELDS = "useProtectedFields";
    public static final String  MUSTACHE_EXTENSION =".mustache";
    public static final String JAVA_EXTENSION =".java";


    public BoatWebhooksCodeGen() {
        super();
        log.info("BoatWebhooksCodeGen constructor called. NAME: {}", NAME);
        this.embeddedTemplateDir = this.templateDir = NAME;
        this.openapiNormalizer.put("REF_AS_PARENT_IN_ALLOF", "true");

        this.cliOptions.add(CliOption.newBoolean(USE_CLASS_LEVEL_BEAN_VALIDATION,
                "Add @Validated to class-level Api interfaces.", this.useClassLevelBeanValidation));
        this.cliOptions.add(CliOption.newBoolean(ADD_SERVLET_REQUEST,
                "Adds a HttpServletRequest object to the API definition method.", this.addServletRequest));
        this.cliOptions.add(CliOption.newBoolean(ADD_BINDING_RESULT,
                "Adds a Binding result as method perimeter. Only implemented if @validate is being used.",
                this.addBindingResult));
        this.cliOptions.add(CliOption.newBoolean(USE_LOMBOK_ANNOTATIONS,
                "Add Lombok to class-level Api models. Defaults to false.", this.useLombokAnnotations));
        this.cliOptions.add(CliOption.newBoolean(USE_WITH_MODIFIERS,
                "Whether to use \"with\" prefix for POJO modifiers.", this.useWithModifiers));
        this.cliOptions.add(CliOption.newString(USE_PROTECTED_FIELDS,
                "Whether to use protected visibility for model fields"));
        supportedLibraries.put(NAME, "Boat Webhooks codegen");
        this.apiNameSuffix = "Api";
        this.apiNamePrefix = "Webhook";
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public String toApiName(String name) {
        if (name.isEmpty()) {
            name = "default";
        }

        name = sanitizeName(name);

        return camelize(this.apiNamePrefix + "_" + name + "_" + this.apiNameSuffix);
    }

    @Override
    public void processOpts() {
        super.processOpts();
        log.info("BoatWebhooksCodeGen processOpts called. Adding supporting files and properties.");
        final var webhookResponseTemplate = "WebhookResponse";
        this.supportingFiles.add(new SupportingFile(webhookResponseTemplate + MUSTACHE_EXTENSION,
                (sourceFolder + File.separator + modelPackage).replace(".", File.separator),
                webhookResponseTemplate + JAVA_EXTENSION));
        final var servletContentTemplate = "ServletContent";
        this.supportingFiles.add(new SupportingFile(servletContentTemplate + MUSTACHE_EXTENSION,
                (sourceFolder + File.separator + modelPackage).replace(".", File.separator),
                servletContentTemplate + JAVA_EXTENSION));
        final var posthookRequestTemplate = "PosthookRequest";
        this.supportingFiles.add(new SupportingFile(posthookRequestTemplate + MUSTACHE_EXTENSION,
                (sourceFolder + File.separator + modelPackage).replace(".", File.separator),
                posthookRequestTemplate + JAVA_EXTENSION));
        final var prehookRequestTemplate = "PrehookRequest";
        this.supportingFiles.add(new SupportingFile(prehookRequestTemplate + MUSTACHE_EXTENSION,
                (sourceFolder + File.separator + modelPackage).replace(".", File.separator),
                prehookRequestTemplate + JAVA_EXTENSION));
    }


    @Override
    public CodegenOperation fromOperation(String path, String httpMethod, Operation operation, List<Server> servers) {
        CodegenOperation codegenOperation = super.fromOperation(path, httpMethod, operation, servers);
        // Remove the standard body parameter (if it exists) ---
        // This prevents the generator's default logic from inserting its own request body.
        codegenOperation.allParams.removeIf(p -> p.isBodyParam);
        return codegenOperation;
    }

}
