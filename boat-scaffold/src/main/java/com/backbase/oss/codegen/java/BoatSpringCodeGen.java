package com.backbase.oss.codegen.java;

import com.samskivert.mustache.Mustache;
import com.samskivert.mustache.Template.Fragment;
import java.io.IOException;
import java.io.Writer;
import static java.util.Arrays.stream;
import static java.util.stream.Collectors.joining;
import java.util.stream.IntStream;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import org.openapitools.codegen.CliOption;
import org.openapitools.codegen.CodegenConstants;
import org.openapitools.codegen.CodegenModel;
import org.openapitools.codegen.CodegenParameter;
import org.openapitools.codegen.CodegenProperty;
import org.openapitools.codegen.config.GlobalSettings;
import org.openapitools.codegen.languages.SpringCodegen;
import org.openapitools.codegen.templating.mustache.IndentedLambda;
import static org.openapitools.codegen.utils.StringUtils.camelize;

public class BoatSpringCodeGen extends SpringCodegen {
    public static final String NAME = "boat-spring";

    public static final String USE_CLASS_LEVEL_BEAN_VALIDATION = "useClassLevelBeanValidation";
    public static final String ADD_SERVLET_REQUEST = "addServletRequest";
    public static final String ADD_BINDING_RESULT = "addBindingResult";
    public static final String USE_LOMBOK_ANNOTATIONS = "useLombokAnnotations";
    public static final String USE_SET_FOR_UNIQUE_ITEMS = "useSetForUniqueItems";
    public static final String OPENAPI_NULLABLE = "openApiNullable";
    public static final String USE_WITH_MODIFIERS = "useWithModifiers";
    public static final String BASE_TYPE = "java.util.Set";

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

    @Setter
    @Getter
    protected boolean useSetForUniqueItems;

    /**
     * Enable OpenAPI Jackson Nullable library
     */
    @Setter
    @Getter
    protected boolean openApiNullable = true;

    /**
     * Whether to use {@code with} prefix for pojos modifiers.
     */
    @Setter
    @Getter
    protected boolean useWithModifiers;

    public BoatSpringCodeGen() {
        this.embeddedTemplateDir = this.templateDir = NAME;

        this.cliOptions.add(CliOption.newBoolean(USE_CLASS_LEVEL_BEAN_VALIDATION,
            "Add @Validated to class-level Api interfaces.", this.useClassLevelBeanValidation));
        this.cliOptions.add(CliOption.newBoolean(ADD_SERVLET_REQUEST,
            "Adds a HttpServletRequest object to the API definition method.", this.addServletRequest));
        this.cliOptions.add(CliOption.newBoolean(ADD_BINDING_RESULT,
                "Adds a Binding result as method perimeter. Only implemented if @validate is being used.", this.addBindingResult));
        this.cliOptions.add(CliOption.newBoolean(USE_LOMBOK_ANNOTATIONS,
            "Add Lombok to class-level Api models. Defaults to false.", this.useLombokAnnotations));
        this.cliOptions.add(CliOption.newBoolean(USE_SET_FOR_UNIQUE_ITEMS,
            "Use java.util.Set for arrays that have uniqueItems set to true.", this.useSetForUniqueItems));
        this.cliOptions.add(CliOption.newBoolean(OPENAPI_NULLABLE,
            "Enable OpenAPI Jackson Nullable library.", this.openApiNullable));
        this.cliOptions.add(CliOption.newBoolean(USE_WITH_MODIFIERS,
            "Whether to use \"with\" prefix for POJO modifiers.", this.useWithModifiers));

        this.apiNameSuffix = "Api";
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

        if (this.reactive) {
            this.useSetForUniqueItems = false;
        }

        // Whether it's using ApiUtil or not.
        // cases:
        // <supportingFilesToGenerate>ApiUtil.java present or not</supportingFilesToGenerate>
        // <generateSupportingFiles>true or false</generateSupportingFiles>
        final String supFiles = GlobalSettings.getProperty(CodegenConstants.SUPPORTING_FILES);
        final boolean useApiUtil =
            supFiles == null
                ? false // cleared by <generateSuportingFiles>false</generateSuportingFiles>
                : supFiles.isEmpty()
                    ? needApiUtil() // set to empty by <generateSuportingFiles>true</generateSuportingFiles>
                    : supFiles.contains("ApiUtil.java"); // set by <supportingFilesToGenerate/>

        if (!useApiUtil) {
            this.supportingFiles
                .removeIf(sf -> "apiUtil.mustache".equals(sf.templateFile));
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
        if (this.additionalProperties.containsKey(USE_SET_FOR_UNIQUE_ITEMS)) {
            this.useSetForUniqueItems = convertPropertyToBoolean(USE_SET_FOR_UNIQUE_ITEMS);
        }
        if (this.additionalProperties.containsKey(OPENAPI_NULLABLE)) {
            this.openApiNullable = convertPropertyToBoolean(OPENAPI_NULLABLE);
        }
        if (this.additionalProperties.containsKey(USE_WITH_MODIFIERS)) {
            this.useWithModifiers = convertPropertyToBoolean(USE_WITH_MODIFIERS);
        }

        writePropertyBack(USE_CLASS_LEVEL_BEAN_VALIDATION, this.useClassLevelBeanValidation);
        writePropertyBack(ADD_SERVLET_REQUEST, this.addServletRequest);
        writePropertyBack(ADD_BINDING_RESULT, this.addBindingResult);
        writePropertyBack(USE_LOMBOK_ANNOTATIONS, this.useLombokAnnotations);
        writePropertyBack(OPENAPI_NULLABLE, this.openApiNullable);
        writePropertyBack(USE_SET_FOR_UNIQUE_ITEMS, this.useSetForUniqueItems);
        writePropertyBack(USE_WITH_MODIFIERS, this.useWithModifiers);

        if (this.useSetForUniqueItems) {
            this.typeMapping.put("set", BASE_TYPE);

            this.importMapping.put("Set", BASE_TYPE);
            this.importMapping.put("LinkedHashSet", "java.util.LinkedHashSet");
        }

        this.additionalProperties.put("indent4", new IndentedLambda(4, " "));
        this.additionalProperties.put("newLine4", new NewLineIndent(4, " "));
        this.additionalProperties.put("indent8", new IndentedLambda(8, " "));
        this.additionalProperties.put("newLine8", new NewLineIndent(8, " "));
    }

    @Override
    public void postProcessModelProperty(CodegenModel model, CodegenProperty p) {
        super.postProcessModelProperty(model, p);

        if (p.isContainer && this.useSetForUniqueItems && p.getUniqueItems()) {
            p.containerType = "set";
            p.baseType = BASE_TYPE;
            p.dataType = BASE_TYPE + "<" + p.items.dataType + ">";
            p.datatypeWithEnum = BASE_TYPE + "<" + p.items.datatypeWithEnum + ">";
            p.defaultValue = "new " + "java.util.LinkedHashSet<>()";
        }
    }

    @Override
    public void postProcessParameter(CodegenParameter p) {
        super.postProcessParameter(p);

        if (p.isContainer) {
            if (!this.reactive) {
                p.baseType = p.dataType.replaceAll("^([^<]+)<.+>$", "$1");
            }

            if (this.useSetForUniqueItems && p.getUniqueItems()) {
                p.baseType = BASE_TYPE;
                p.dataType = BASE_TYPE + "<" + p.items.dataType + ">";
                p.datatypeWithEnum = BASE_TYPE + "<" + p.items.datatypeWithEnum + ">";
                p.defaultValue = "new " + "java.util.LinkedHashSet<>()";
            }
        }
    }

    private boolean needApiUtil() {
        return this.apiTemplateFiles.containsKey("api.mustache")
            && this.apiTemplateFiles.containsKey("apiDelegate.mustache");
    }
}
