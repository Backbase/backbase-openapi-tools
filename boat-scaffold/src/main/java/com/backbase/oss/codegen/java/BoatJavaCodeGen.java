package com.backbase.oss.codegen.java;

import static com.backbase.oss.codegen.java.BoatCodeGenUtils.getCollectionCodegenValue;

import com.backbase.oss.codegen.java.BoatCodeGenUtils.CodegenValueType;
import io.swagger.v3.oas.models.media.Schema;
import java.io.File;
import lombok.Getter;
import lombok.Setter;
import org.openapitools.codegen.CliOption;
import org.openapitools.codegen.CodegenProperty;
import org.openapitools.codegen.SupportingFile;
import org.openapitools.codegen.languages.JavaClientCodegen;
import org.openapitools.codegen.utils.ModelUtils;

public class BoatJavaCodeGen extends JavaClientCodegen {

    public static final String NAME = "boat-java";

    public static final String USE_WITH_MODIFIERS = "useWithModifiers";
    public static final String USE_CLASS_LEVEL_BEAN_VALIDATION = "useClassLevelBeanValidation";
    public static final String USE_JACKSON_CONVERSION = "useJacksonConversion";
    public static final String USE_DEFAULT_API_CLIENT = "useDefaultApiClient";
    public static final String REST_TEMPLATE_BEAN_NAME = "restTemplateBeanName";
    public static final String CREATE_API_COMPONENT = "createApiComponent";
    public static final String USE_PROTECTED_FIELDS = "useProtectedFields";
    @Setter
    @Getter
    protected boolean useWithModifiers;
    @Setter
    @Getter
    protected boolean useClassLevelBeanValidation;
    @Setter
    @Getter
    protected boolean useJacksonConversion;
    @Getter
    @Setter
    protected boolean useDefaultApiClient = true;
    @Getter
    @Setter
    protected String restTemplateBeanName;
    @Getter
    @Setter
    protected boolean createApiComponent = true;

    public BoatJavaCodeGen() {
        this.useJakartaEe = true;
        this.openapiNormalizer.put("REF_AS_PARENT_IN_ALLOF", "true");

        this.embeddedTemplateDir = this.templateDir = NAME;

        this.cliOptions.add(CliOption.newBoolean(USE_CLASS_LEVEL_BEAN_VALIDATION,
            "Add @Validated to class-level Api interfaces", this.useClassLevelBeanValidation));
        this.cliOptions.add(CliOption.newBoolean(USE_WITH_MODIFIERS,
            "Whether to use \"with\" prefix for POJO modifiers", this.useWithModifiers));
        this.cliOptions.add(CliOption.newBoolean(USE_JACKSON_CONVERSION,
            "Whether to use Jackson to convert query parameters to String", this.useJacksonConversion));
        this.cliOptions.add(CliOption.newBoolean(USE_DEFAULT_API_CLIENT,
            "Whether to use a default ApiClient with a builtin template", this.useDefaultApiClient));
        this.cliOptions.add(CliOption.newString(REST_TEMPLATE_BEAN_NAME,
            "An optional RestTemplate bean name"));
        this.cliOptions.add(CliOption.newString(CREATE_API_COMPONENT,
            "Whether to generate the client as a Spring component"));
        this.cliOptions.add(CliOption.newString(USE_PROTECTED_FIELDS,
            "Whether to use protected visibility for model fields"));
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public void processOpts() {
        super.processOpts();

        if (this.additionalProperties.containsKey(USE_WITH_MODIFIERS)) {
            this.useWithModifiers = convertPropertyToBoolean(USE_WITH_MODIFIERS);
        }
        writePropertyBack(USE_WITH_MODIFIERS, this.useWithModifiers);

        if (RESTTEMPLATE.equals(getLibrary())) {
            processRestTemplateOpts();
        }
        if (this.additionalProperties.containsKey(USE_PROTECTED_FIELDS)) {
            this.additionalProperties.put("modelFieldsVisibility", "protected");
        } else {
            this.additionalProperties.put("modelFieldsVisibility", "private");
        }

        if (!getLibrary().startsWith("jersey")) {
            this.supportingFiles.removeIf(f -> f.getTemplateFile().equals("ServerConfiguration.mustache"));
            this.supportingFiles.removeIf(f -> f.getTemplateFile().equals("ServerVariable.mustache"));
        }

    }

    private void processRestTemplateOpts() {
        if (this.additionalProperties.containsKey(USE_CLASS_LEVEL_BEAN_VALIDATION)) {
            this.useClassLevelBeanValidation = convertPropertyToBoolean(USE_CLASS_LEVEL_BEAN_VALIDATION);
        }
        writePropertyBack(USE_CLASS_LEVEL_BEAN_VALIDATION, this.useClassLevelBeanValidation);

        if (this.additionalProperties.containsKey(USE_JACKSON_CONVERSION)) {
            this.useJacksonConversion = convertPropertyToBoolean(USE_JACKSON_CONVERSION);
        }
        writePropertyBack(USE_JACKSON_CONVERSION, this.useJacksonConversion);
        if (this.useJacksonConversion) {
            this.supportingFiles.removeIf(f -> f.getTemplateFile().equals("RFC3339DateFormat.mustache"));
        }

        if (this.additionalProperties.containsKey(USE_DEFAULT_API_CLIENT)) {
            this.useDefaultApiClient = convertPropertyToBoolean(USE_DEFAULT_API_CLIENT);
        }
        writePropertyBack(USE_DEFAULT_API_CLIENT, this.useDefaultApiClient);

        this.restTemplateBeanName = (String) this.additionalProperties.get(REST_TEMPLATE_BEAN_NAME);

        if (this.additionalProperties.containsKey(CREATE_API_COMPONENT)) {
            this.createApiComponent = convertPropertyToBoolean(CREATE_API_COMPONENT);
        }
        writePropertyBack(CREATE_API_COMPONENT, this.createApiComponent);

        if (useJacksonConversion) {
            final var serializerTemplate = "BigDecimalCustomSerializer";
            String targetDir = (sourceFolder + File.separator + modelPackage).replace(".", java.io.File.separator);
            String importPackage = targetDir.replace("\\", ".").replace("/", ".");
            if (importPackage.length() > 0 && !importPackage.endsWith(".")) {
                importPackage += ".";
            }
            this.supportingFiles.add(new SupportingFile(serializerTemplate + ".mustache", targetDir, serializerTemplate + ".java"));
            this.importMapping.put(serializerTemplate, importPackage + serializerTemplate);
        }
    }

    @Override
    public String toDefaultValue(CodegenProperty cp, Schema schema) {
        final Schema referencedSchema = ModelUtils.getReferencedSchema(this.openAPI, schema);
        return getCollectionCodegenValue(cp, referencedSchema, containerDefaultToNull, instantiationTypes())
            .map(CodegenValueType::getValue)
            .orElseGet(() -> super.toDefaultValue(cp, referencedSchema));
    }
}
