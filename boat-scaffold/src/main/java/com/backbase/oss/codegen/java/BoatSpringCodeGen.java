package com.backbase.oss.codegen.java;

import com.backbase.oss.codegen.java.BoatCodeGenUtils.CodegenValueType;
import com.samskivert.mustache.Mustache;
import com.samskivert.mustache.Template.Fragment;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.servers.Server;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import org.openapitools.codegen.*;
import org.openapitools.codegen.config.GlobalSettings;
import org.openapitools.codegen.languages.SpringCodegen;
import org.openapitools.codegen.templating.mustache.IndentedLambda;
import org.openapitools.codegen.utils.ModelUtils;

import java.io.IOException;
import java.io.Writer;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.IntStream;

import static com.backbase.oss.codegen.java.BoatCodeGenUtils.getCollectionCodegenValue;
import static java.util.Arrays.stream;
import static java.util.stream.Collectors.joining;
import static org.apache.commons.lang3.StringUtils.contains;
import static org.apache.commons.lang3.StringUtils.isEmpty;
import static org.openapitools.codegen.utils.StringUtils.camelize;

public class BoatSpringCodeGen extends SpringCodegen {

    public static final String NAME = "boat-spring";

    public static final String USE_CLASS_LEVEL_BEAN_VALIDATION = "useClassLevelBeanValidation";
    public static final String ADD_SERVLET_REQUEST = "addServletRequest";
    public static final String ADD_BINDING_RESULT = "addBindingResult";
    public static final String USE_LOMBOK_ANNOTATIONS = "useLombokAnnotations";
    public static final String USE_WITH_MODIFIERS = "useWithModifiers";
    public static final String USE_PROTECTED_FIELDS = "useProtectedFields";

    static class NewLineIndent implements Mustache.Lambda {

        private final String prefix;
        private static final String REGEX = "\\s+$";

        NewLineIndent(int level, String space) {
            this.prefix = IntStream.range(0, level).mapToObj(n -> space).collect(joining());
        }

        @Override
        public void execute(Fragment frag, Writer out) throws IOException {
            final String text = frag.execute();

            if (text == null || text.isEmpty()) {
                return;
            }

            final String[] lines = splitLines(text);
            final int indent = minIndent(lines);

            for (final String line : lines) {
                out.write(this.prefix);
                out.write(StringUtils.substring(line, indent));
                out.write(System.lineSeparator());
            }
        }

        private String[] splitLines(final String text) {
            return stream(text.split("\\r\\n|\\n"))
                .map(s -> s.replaceFirst(REGEX, ""))
                .toArray(String[]::new);
        }

        private int minIndent(String[] lines) {
            return stream(lines)
                .filter(StringUtils::isNotBlank)
                .map(s -> s.replaceFirst(REGEX, ""))
                .map(NewLineIndent::indentLevel)
                .min(Integer::compareTo)
                .orElse(0);
        }

        static int indentLevel(String text) {
            return IntStream
                .range(0, text.replaceFirst(REGEX, text).length())
                .filter(n -> !Character.isWhitespace(text.charAt(n)))
                .findFirst().orElse(0);
        }
    }

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

    public BoatSpringCodeGen() {
        super();
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

        this.apiNameSuffix = "Api";
    }

    /*
     * Overridden to be able to override the private <code>replaceBeanValidationCollectionType</code> method.
     */
    @Override
    public CodegenParameter fromParameter(Parameter parameter, Set<String> imports) {
        CodegenParameter codegenParameter = super.fromParameter(parameter, imports);
        if (!isListOrSet(codegenParameter)) {
            return codegenParameter;
        } else {
            codegenParameter.datatypeWithEnum = replaceBeanValidationCollectionType(codegenParameter.items, codegenParameter.datatypeWithEnum);
            codegenParameter.dataType = replaceBeanValidationCollectionType(codegenParameter.items, codegenParameter.dataType);
            return codegenParameter;
        }
    }

    /*
     * Overridden to be able to override the private <code>replaceBeanValidationCollectionType</code> method.
     */
    @Override
    public CodegenProperty fromProperty(String name, Schema p, boolean required, boolean schemaIsFromAdditionalProperties) {
        CodegenProperty codegenProperty = super.fromProperty(name, p, required, schemaIsFromAdditionalProperties);
        if (!isListOrSet(codegenProperty)) {
            return codegenProperty;
        } else {
            codegenProperty.datatypeWithEnum = replaceBeanValidationCollectionType(codegenProperty.items, codegenProperty.datatypeWithEnum);
            codegenProperty.dataType = replaceBeanValidationCollectionType(codegenProperty.items, codegenProperty.dataType);
            return codegenProperty;
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
        if (!contains(dataType, "@Valid")) {
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

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public String toApiName(String name) {
        if (name.length() == 0) {
            name = "default";
        }

        name = sanitizeName(name);

        return camelize(this.apiNamePrefix + "_" + name + "_" + this.apiNameSuffix);
    }

    @Override
    public void processOpts() {
        super.processOpts();


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


        this.additionalProperties.put("indent4", new IndentedLambda(4, " "));
        this.additionalProperties.put("newLine4", new NewLineIndent(4, " "));
        this.additionalProperties.put("indent8", new IndentedLambda(8, " "));
        this.additionalProperties.put("newLine8", new NewLineIndent(8, " "));
    }


    @Override
    public void postProcessParameter(CodegenParameter p) {
        super.postProcessParameter(p);

        if (p.isContainer) {
            if (!this.reactive) {
                p.baseType = p.dataType.replaceAll("^([^<]+)<.+>$", "$1");
            }
        }
    }

    private boolean needApiUtil() {
        return this.apiTemplateFiles.containsKey("api.mustache")
            && this.apiTemplateFiles.containsKey("apiDelegate.mustache");
    }

    /**
        This method has been overridden in order to add a parameter to codegen operation for adding HttpServletRequest to
        the service interface. There is a relevant httpServletParam.mustache file.
     */
    @Override
    public CodegenOperation fromOperation(String path, String httpMethod, Operation operation, List<Server> servers) {
        final CodegenOperation codegenOperation = super.fromOperation(path, httpMethod, operation, servers);
        if (this.addServletRequest) {
            final CodegenParameter codegenParameter = new CodegenParameter() {
                public boolean isHttpServletRequest = true;
            };
            codegenParameter.paramName = "httpServletRequest";
            codegenOperation.allParams.add(codegenParameter);
        }
        return codegenOperation;
    }

    @Override
    public String toDefaultValue(CodegenProperty cp, Schema schema) {
        final Schema referencedSchema = ModelUtils.getReferencedSchema(this.openAPI, schema);
        return getCollectionCodegenValue(cp, referencedSchema, containerDefaultToNull, instantiationTypes())
            .map(CodegenValueType::getValue)
            .orElseGet(() -> super.toDefaultValue(cp, referencedSchema));
    }
}
