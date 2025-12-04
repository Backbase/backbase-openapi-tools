package com.backbase.oss.codegen.java;

import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.servers.Server;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.openapitools.codegen.*;
import org.openapitools.codegen.config.GlobalSettings;
import org.openapitools.codegen.languages.SpringCodegen;
import org.openapitools.codegen.templating.mustache.IndentedLambda;
import org.openapitools.codegen.utils.ModelUtils;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static com.backbase.oss.codegen.java.BoatCodeGenUtils.getCollectionCodegenValue;
import static org.apache.commons.lang3.StringUtils.isEmpty;
import static org.openapitools.codegen.utils.StringUtils.camelize;

@Slf4j
public class BoatWebhooksCodeGen extends SpringCodegen {
    public static final String NAME = "boat-webhooks";

    public static final String USE_CLASS_LEVEL_BEAN_VALIDATION = "useClassLevelBeanValidation";
    public static final String ADD_SERVLET_REQUEST = "addServletRequest";
    public static final String ADD_BINDING_RESULT = "addBindingResult";
    public static final String USE_LOMBOK_ANNOTATIONS = "useLombokAnnotations";
    public static final String USE_WITH_MODIFIERS = "useWithModifiers";
    public static final String USE_PROTECTED_FIELDS = "useProtectedFields";
    public static final String  MUSTACHE_EXTENSION =".mustache";
    public static final String JAVA_EXTENSION =".java";


    /**
     * Add @Validated to class-level Api interfaces. Defaults to false
     */
    @Setter
    @Getter
    protected boolean useClassLevelBeanValidation;

    /**
     * Adds a HttpServletRequest object to the API definition method.
     */
    @Setter
    @Getter
    protected boolean addServletRequest;

    /**
     * Adds BindingResult to API interface method if @validate is used
     */
    @Setter
    @Getter
    protected boolean addBindingResult;

    /**
     * Add Lombok to class-level Api models. Defaults to false
     */
    @Setter
    @Getter
    protected boolean useLombokAnnotations;


    /**
     * Whether to use {@code with} prefix for pojos modifiers.
     */
    @Setter
    @Getter
    protected boolean useWithModifiers;

    @Setter
    @Getter
    protected boolean useProtectedFields;

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

        // Whether it's using ApiUtil or not.
        // cases:
        // <supportingFilesToGenerate>ApiUtil.java present or not</supportingFilesToGenerate>
        // <generateSupportingFiles>true or false</generateSupportingFiles>
        final String supFiles = GlobalSettings.getProperty(CodegenConstants.SUPPORTING_FILES);
        // cleared by <generateSuportingFiles>false</generateSuportingFiles>
        final boolean useApiUtil = supFiles != null && (supFiles.isEmpty()
                ? needApiUtil() // set to empty by <generateSuportingFiles>true</generateSuportingFiles>
                : supFiles.contains("ApiUtil.java")); // set by <supportingFilesToGenerate/>

        if (!useApiUtil) {
            this.supportingFiles
                    .removeIf(sf -> "apiUtil.mustache".equals(sf.getTemplateFile()));
        }

        writePropertyBack("useApiUtil", useApiUtil);

        final var serializerTemplate = "BigDecimalCustomSerializer";
        this.supportingFiles.add(new SupportingFile(
                serializerTemplate + MUSTACHE_EXTENSION,
                (sourceFolder + File.separator + modelPackage).replace(".", File.separator),
                serializerTemplate + JAVA_EXTENSION
        ));
        this.importMapping.put(serializerTemplate, modelPackage + "." + serializerTemplate);

        // Adding Webhook related models to supporting files
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
        String modelPath = (sourceFolder + File.separator + modelPackage).replace(".", File.separator);
        log.info("Supporting file output path: {}", modelPath);
        this.importMapping.put(webhookResponseTemplate, modelPackage + "." + webhookResponseTemplate);
        this.importMapping.put(servletContentTemplate, modelPackage + "." + servletContentTemplate);
        this.importMapping.put(posthookRequestTemplate, modelPackage + "." + posthookRequestTemplate);
        this.importMapping.put(prehookRequestTemplate, modelPackage + "." + prehookRequestTemplate);

        log.info("supportingFiles size: {}", this.supportingFiles.size());

        log.info("supportingFiles size: {}", this.supportingFiles);

        if (this.additionalProperties.containsKey(USE_CLASS_LEVEL_BEAN_VALIDATION)) {
            this.useClassLevelBeanValidation = convertPropertyToBoolean(USE_CLASS_LEVEL_BEAN_VALIDATION);
        }
        if (this.additionalProperties.containsKey(ADD_SERVLET_REQUEST)) {
            this.addServletRequest = convertPropertyToBoolean(ADD_SERVLET_REQUEST);
        }
        if (this.additionalProperties.containsKey(ADD_BINDING_RESULT)) {
            this.addBindingResult = convertPropertyToBoolean(ADD_BINDING_RESULT);
        }
        if (this.additionalProperties.containsKey(USE_LOMBOK_ANNOTATIONS)) {
            this.useLombokAnnotations = convertPropertyToBoolean(USE_LOMBOK_ANNOTATIONS);
        }
        if (this.additionalProperties.containsKey(USE_WITH_MODIFIERS)) {
            this.useWithModifiers = convertPropertyToBoolean(USE_WITH_MODIFIERS);
        }
        if (this.additionalProperties.containsKey(USE_PROTECTED_FIELDS)) {
            this.additionalProperties.put("modelFieldsVisibility", "protected");
        } else {
            this.additionalProperties.put("modelFieldsVisibility", "private");
        }

        writePropertyBack(USE_CLASS_LEVEL_BEAN_VALIDATION, this.useClassLevelBeanValidation);
        writePropertyBack(ADD_SERVLET_REQUEST, this.addServletRequest);
        writePropertyBack(ADD_BINDING_RESULT, this.addBindingResult);
        writePropertyBack(USE_LOMBOK_ANNOTATIONS, this.useLombokAnnotations);
        writePropertyBack(USE_WITH_MODIFIERS, this.useWithModifiers);
        writePropertyBack(USE_PROTECTED_FIELDS, this.useProtectedFields);

        this.additionalProperties.put("indent4", new IndentedLambda(4, " ", true, true));
        this.additionalProperties.put("newLine4", new BoatSpringCodeGen.NewLineIndent(4, " "));
        this.additionalProperties.put("indent8", new IndentedLambda(8, " ", true, true));
        this.additionalProperties.put("newLine8", new BoatSpringCodeGen.NewLineIndent(8, " "));
        this.additionalProperties.put("toOneLine", new BoatSpringCodeGen.FormatToOneLine());
        this.additionalProperties.put("trimAndIndent4", new BoatSpringCodeGen.TrimAndIndent(4, " "));
    }

    private boolean needApiUtil() {
        return this.apiTemplateFiles.containsKey("api.mustache")
                && this.apiTemplateFiles.containsKey("apiDelegate.mustache");
    }

    /*
     * Overridden to be able to override the private <code>replaceBeanValidationCollectionType</code> method.
     */
    @Override
    public CodegenParameter fromParameter(Parameter parameter, Set<String> imports) {
        CodegenParameter codegenParameter = super.fromParameter(parameter, imports);
        if (!isListOrSet(codegenParameter)) {
            return new BoatSpringCodegenParameter(codegenParameter);
        } else {
            codegenParameter.datatypeWithEnum = replaceBeanValidationCollectionType(codegenParameter.items, codegenParameter.datatypeWithEnum);
            codegenParameter.dataType = replaceBeanValidationCollectionType(codegenParameter.items, codegenParameter.dataType);
            return new BoatSpringCodegenParameter(codegenParameter);
        }
    }

    /*
     * Overridden to be able to override the private <code>replaceBeanValidationCollectionType</code> method.
     */
    @Override
    public CodegenProperty fromProperty(String name, Schema p, boolean required, boolean schemaIsFromAdditionalProperties) {
        CodegenProperty codegenProperty = super.fromProperty(name, p, required, schemaIsFromAdditionalProperties);
        if (!isListOrSet(codegenProperty)) {
            return new BoatSpringCodegenProperty(codegenProperty);
        } else {
            codegenProperty.datatypeWithEnum = replaceBeanValidationCollectionType(codegenProperty.items, codegenProperty.datatypeWithEnum);
            codegenProperty.dataType = replaceBeanValidationCollectionType(codegenProperty.items, codegenProperty.dataType);
            return new BoatSpringCodegenProperty(codegenProperty);
        }
    }

    /**
     * "overridden" to fix invalid code when the data type is a collection of a fully qualified classname.
     * eg. <code>Set<@Valid com.backbase.dbs.arrangement.commons.model.TranslationItemDto></code>
     *
     * @param codegenProperty
     * @param dataType
     * @return
     */
    String replaceBeanValidationCollectionType(CodegenProperty codegenProperty, String dataType) {
        if (!useBeanValidation || isEmpty(dataType) || !codegenProperty.isModel || isResponseType(codegenProperty)) {
            return dataType;
        }
        String result = dataType;
        if (!dataType.contains("@Valid")) {
            result = dataType.replace("<", "<@Valid ");
        }
        Matcher m = Pattern.compile("^(.+\\<)(@Valid) ([a-z\\.]+)([A-Z].*)(\\>)$").matcher(dataType);
        if (m.matches()) {
            // Set<@Valid com.backbase.dbs.arrangement.commons.model.TranslationItemDto>
            result = m.group(1) + m.group(3) + m.group(2) + " " + m.group(4) + m.group(5);
        }
        return result;
    }

    // Copied, but not modified
    private static boolean isListOrSet(CodegenProperty codegenProperty) {
        return codegenProperty.isContainer && !codegenProperty.isMap;
    }

    // Copied, but not modified
    private static boolean isListOrSet(CodegenParameter codegenParameter) {
        return codegenParameter.isContainer && !codegenParameter.isMap;
    }

    // Copied, but not modified
    private static boolean isResponseType(CodegenProperty codegenProperty) {
        return codegenProperty.baseName.toLowerCase(Locale.ROOT).contains("response");
    }

    /**
     This method has been overridden in order to add a parameter to codegen operation for adding HttpServletRequest to
     the service interface. There is a relevant httpServletParam.mustache file.
     */
    @Override
    public CodegenOperation fromOperation(String path, String httpMethod, Operation operation, List<Server> servers) {
        if (operation.getExtensions() == null) {
            operation.setExtensions(new LinkedHashMap<>());
        }
        final CodegenOperation codegenOperation = super.fromOperation(path, httpMethod, operation, servers);
        // Remove the standard body parameter (if it exists) ---
        // This prevents the generator's default logic from inserting its own request body.
        codegenOperation.allParams.removeIf(p -> p.isBodyParam);
        if (this.addServletRequest) {
            final CodegenParameter codegenParameter = new CodegenParameter();
            codegenParameter.paramName = "httpServletRequest";
            codegenOperation.allParams.add(codegenParameter);
        }
        if (codegenOperation.returnType != null) {
            codegenOperation.returnType = codegenOperation.returnType.replace("@Valid", "");
        }
        return codegenOperation;
    }

    @Override
    public String toDefaultValue(CodegenProperty cp, Schema schema) {
        final Schema referencedSchema = ModelUtils.getReferencedSchema(this.openAPI, schema);
        return getCollectionCodegenValue(cp, referencedSchema, containerDefaultToNull, instantiationTypes())
                .map(BoatCodeGenUtils.CodegenValueType::getValue)
                .orElseGet(() -> super.toDefaultValue(cp, referencedSchema));
    }

    @Override
    public void postProcessModelProperty(CodegenModel model, CodegenProperty property) {

        super.postProcessModelProperty(model, property);

        if (shouldSerializeBigDecimalAsString(property)) {
            property.vendorExtensions.put("x-extra-annotation", "@JsonSerialize(using = BigDecimalCustomSerializer.class)");
            model.imports.add("BigDecimalCustomSerializer");
            model.imports.add("JsonSerialize");
        }
    }

    private boolean shouldSerializeBigDecimalAsString(CodegenProperty property) {
        return (serializeBigDecimalAsString && ("decimal".equalsIgnoreCase(property.baseType) || "bigdecimal".equalsIgnoreCase(property.baseType)))
                || (isApiStringFormattedAsNumber(property) && !isDataTypeString(property));
    }

    private boolean isApiStringFormattedAsNumber(CodegenProperty property) {
        return "string".equalsIgnoreCase(property.openApiType) && "number".equalsIgnoreCase(property.dataFormat);
    }

    private boolean isDataTypeString(CodegenProperty property) {
        return Stream.of(property.baseType, property.dataType, property.datatypeWithEnum)
                .anyMatch("string"::equalsIgnoreCase);
    }

    @Override
    public void postProcessParameter(CodegenParameter p) {
        super.postProcessParameter(p);
        if (p.isContainer && !this.reactive) {
            p.baseType = p.dataType.replaceAll("^([^<]+)<.+>$", "$1");
        }
    }

}
