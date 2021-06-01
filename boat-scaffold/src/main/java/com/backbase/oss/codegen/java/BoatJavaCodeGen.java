package com.backbase.oss.codegen.java;

import static java.lang.String.format;

import lombok.Getter;
import lombok.Setter;
import org.openapitools.codegen.CliOption;
import org.openapitools.codegen.CodegenModel;
import org.openapitools.codegen.CodegenParameter;
import org.openapitools.codegen.CodegenProperty;
import org.openapitools.codegen.languages.JavaClientCodegen;

public class BoatJavaCodeGen extends JavaClientCodegen {

    public static final String NAME = "boat-java";

    public static final String USE_WITH_MODIFIERS = "useWithModifiers";
    public static final String USE_SET_FOR_UNIQUE_ITEMS = "useSetForUniqueItems";

    public static final String USE_CLASS_LEVEL_BEAN_VALIDATION = "useClassLevelBeanValidation";
    public static final String USE_JACKSON_CONVERSION = "useJacksonConversion";

    public static final String USE_DEFAULT_API_CLIENT = "useDefaultApiClient";
    public static final String REST_TEMPLATE_BEAN_NAME = "restTemplateBeanName";
    public static final String CREATE_API_COMPONENT = "createApiComponent";
    public static final String USE_PROTECTED_FIELDS = "useProtectedFields";

    private static final String JAVA_UTIL_SET_NEW = "new " + "java.util.LinkedHashSet<>()";
    private static final String JAVA_UTIL_SET = "java.util.Set";
    private static final String JAVA_UTIL_SET_GEN = "java.util.Set<%s>";

    @Setter
    @Getter
    protected boolean useWithModifiers;
    @Setter
    @Getter
    protected boolean useSetForUniqueItems;
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
        this.embeddedTemplateDir = this.templateDir = NAME;

        this.cliOptions.add(CliOption.newBoolean(USE_CLASS_LEVEL_BEAN_VALIDATION,
            "Add @Validated to class-level Api interfaces", this.useClassLevelBeanValidation));
        this.cliOptions.add(CliOption.newBoolean(USE_WITH_MODIFIERS,
            "Whether to use \"with\" prefix for POJO modifiers", this.useWithModifiers));
        this.cliOptions.add(CliOption.newBoolean(USE_SET_FOR_UNIQUE_ITEMS,
            "Use java.util.Set for arrays that have uniqueItems set to true", this.useSetForUniqueItems));
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

        if (WEBCLIENT.equals(getLibrary())) {
            this.useSetForUniqueItems = false;
        }

        if (this.additionalProperties.containsKey(USE_WITH_MODIFIERS)) {
            this.useWithModifiers = convertPropertyToBoolean(USE_WITH_MODIFIERS);
        }
        writePropertyBack(USE_WITH_MODIFIERS, this.useWithModifiers);

        if (this.additionalProperties.containsKey(USE_SET_FOR_UNIQUE_ITEMS)) {
            this.useSetForUniqueItems = convertPropertyToBoolean(USE_SET_FOR_UNIQUE_ITEMS);
        }
        writePropertyBack(USE_SET_FOR_UNIQUE_ITEMS, this.useSetForUniqueItems);

        if (this.useSetForUniqueItems) {
            this.typeMapping.put("set", JAVA_UTIL_SET);

            this.importMapping.put("Set", JAVA_UTIL_SET);
            this.importMapping.put("LinkedHashSet", "java.util.LinkedHashSet");
        }

        if (RESTTEMPLATE.equals(getLibrary())) {
            if (this.additionalProperties.containsKey(USE_CLASS_LEVEL_BEAN_VALIDATION)) {
                this.useClassLevelBeanValidation = convertPropertyToBoolean(USE_CLASS_LEVEL_BEAN_VALIDATION);
            }
            writePropertyBack(USE_CLASS_LEVEL_BEAN_VALIDATION, this.useClassLevelBeanValidation);

            if (this.additionalProperties.containsKey(USE_JACKSON_CONVERSION)) {
                this.useJacksonConversion = convertPropertyToBoolean(USE_JACKSON_CONVERSION);
            }
            writePropertyBack(USE_JACKSON_CONVERSION, this.useJacksonConversion);
            if (this.useJacksonConversion) {
                this.supportingFiles.removeIf(f -> f.templateFile.equals("RFC3339DateFormat.mustache"));
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
        }
        if (this.additionalProperties.containsKey(USE_PROTECTED_FIELDS)) {
            this.additionalProperties.put("modelFieldsVisibility", "protected");
        } else {
            this.additionalProperties.put("modelFieldsVisibility", "private");
        }

        if (!getLibrary().startsWith("jersey")) {
            this.supportingFiles.removeIf(f -> f.templateFile.equals("ServerConfiguration.mustache"));
            this.supportingFiles.removeIf(f -> f.templateFile.equals("ServerVariable.mustache"));
        }
    }

    @Override
    public void postProcessModelProperty(CodegenModel model, CodegenProperty p) {
        super.postProcessModelProperty(model, p);

        if (p.isContainer && this.useSetForUniqueItems && p.getUniqueItems()) {
            p.containerType = "set";
            p.baseType = JAVA_UTIL_SET;
            p.dataType = format(JAVA_UTIL_SET_GEN, p.items.dataType);
            p.datatypeWithEnum = format(JAVA_UTIL_SET_GEN, p.items.datatypeWithEnum);
            p.defaultValue = JAVA_UTIL_SET_NEW;
        }

    }

    @Override
    public void postProcessParameter(CodegenParameter p) {
        super.postProcessParameter(p);

        if (p.isContainer && this.useSetForUniqueItems && p.getUniqueItems()) {
            // XXX the model set baseType to the container type, why is this different?

            p.baseType = p.dataType.replaceAll("^([^<]+)<.+>$", "$1");
            p.baseType = JAVA_UTIL_SET;
            p.dataType = format(JAVA_UTIL_SET_GEN, p.items.dataType);
            p.datatypeWithEnum = format(JAVA_UTIL_SET_GEN, p.items.datatypeWithEnum);
            p.defaultValue = JAVA_UTIL_SET_NEW;

        }
    }
}
