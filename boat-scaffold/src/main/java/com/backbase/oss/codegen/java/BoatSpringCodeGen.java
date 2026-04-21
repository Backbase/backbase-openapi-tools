package com.backbase.oss.codegen.java;

import static com.backbase.oss.codegen.java.BoatCodeGenUtils.getCollectionCodegenValue;
import static java.util.Arrays.stream;
import static java.util.stream.Collectors.joining;
import static org.apache.commons.lang3.StringUtils.contains;
import static org.apache.commons.lang3.StringUtils.isEmpty;
import static org.openapitools.codegen.utils.StringUtils.camelize;

import com.backbase.oss.codegen.java.BoatCodeGenUtils.CodegenValueType;
import com.samskivert.mustache.Mustache;
import com.samskivert.mustache.Template.Fragment;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.servers.Server;
import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.openapitools.codegen.CliOption;
import org.openapitools.codegen.CodegenConstants;
import org.openapitools.codegen.CodegenModel;
import org.openapitools.codegen.CodegenOperation;
import org.openapitools.codegen.CodegenParameter;
import org.openapitools.codegen.CodegenProperty;
import org.openapitools.codegen.SupportingFile;
import org.openapitools.codegen.config.GlobalSettings;
import org.openapitools.codegen.languages.SpringCodegen;
import org.openapitools.codegen.templating.mustache.IndentedLambda;
import org.openapitools.codegen.utils.ModelUtils;

@Slf4j
public class BoatSpringCodeGen extends SpringCodegen {

    public static final String NAME = "boat-spring";

    public static final String ADD_SERVLET_REQUEST = "addServletRequest";
    public static final String ADD_BINDING_RESULT = "addBindingResult";
    public static final String UNWRAP_ESCAPED_QUOTES = "unwrapEscapedQuotes";

    private static final String VENDOR_EXTENSION_NOT_NULL = "x-not-null";
    private static final String JSON_SERIALIZE = "JsonSerialize";

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
                String processedLine = StringUtils.substring(line, indent);
                out.write(postProcessLine(processedLine));
                out.write(System.lineSeparator());
            }
        }

        protected String postProcessLine(String line) {
            return line;
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
     * This lambda reformats multiline generated code into single line.
     */
    static class FormatToOneLine implements Mustache.Lambda {

        private static final String SINGLE_SPACE = " ";

        private static final String WHITESPACE_REGEX = "\\s+";

        @Override
        public void execute(Fragment frag, Writer out) throws IOException {
            String text = frag.execute();
            if (text == null || text.isEmpty()) {
                return;
            }
            String formatted = text
                .replaceAll(WHITESPACE_REGEX, SINGLE_SPACE)
                .replaceAll("\\< ", "<")
                .replaceAll(" >", ">")
                .trim();

            if (log.isTraceEnabled()) {
                log.trace("Fragment [{}] reformatted into [{}]", text, formatted);
            }

            out.write(formatted);
        }
    }

    static class TrimAndIndent extends NewLineIndent {

        TrimAndIndent(int level, String space) {
            super(level, space);
        }

        @Override
        protected String postProcessLine(String line) {
            return line.trim();
        }
    }

    static class UnwrapEscapedQuotes implements Mustache.Lambda {

        @Override
        public void execute(Fragment frag, Writer out) throws IOException {
            String text = frag.execute();
            if (text == null) {
                return;
            }
            String normalized = text.replace("\\\\\"", "\\\"");
            if (normalized.length() >= 4 && normalized.startsWith("\\\"") && normalized.endsWith("\\\"")) {
                out.write(normalized.substring(2, normalized.length() - 2));
                return;
            }
            out.write(normalized);
        }
    }

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

    public BoatSpringCodeGen() {
        super();
        this.embeddedTemplateDir = this.templateDir = NAME;
        this.openapiNormalizer.put("REF_AS_PARENT_IN_ALLOF", "true");

        this.cliOptions.add(CliOption.newBoolean(ADD_SERVLET_REQUEST,
            "Adds a HttpServletRequest object to the API definition method.", this.addServletRequest));
        this.cliOptions.add(CliOption.newBoolean(ADD_BINDING_RESULT,
            "Adds a Binding result as method perimeter. Only implemented if @validate is being used.",
            this.addBindingResult));

        this.apiNameSuffix = "Api";
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
     * "overridden" to fix invalid code when the data type is a collection of a fully qualified classname. eg. <code>Set<@Valid
     * com.backbase.dbs.arrangement.commons.model.TranslationItemDto></code>
     *
     * @param itemsProperty
     * @param dataType
     * @return
     */
    String replaceBeanValidationCollectionType(CodegenProperty itemsProperty, String dataType) {
        if (!useBeanValidation || isEmpty(dataType) || isResponseType(itemsProperty)) {
            return dataType;
        }

        String result = dataType;
        if (itemsProperty.isModel) {
            if (!contains(dataType, "@Valid")) {
                result = dataType.replace("<", "<@Valid ");
            }
            Matcher m = Pattern.compile("^(.+\\<)(@Valid) ([a-z\\.]+)([A-Z].*)(\\>)$").matcher(dataType);
            if (m.matches()) {
                // Set<@Valid com.backbase.dbs.arrangement.commons.model.TranslationItemDto>
                result = m.group(1) + m.group(3) + m.group(2) + " " + m.group(4) + m.group(5);
            }
        } else if (applyNotNullVendorExtension(itemsProperty, dataType)) {
            result = dataType.replace("<", "<@NotNull ");
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

    private boolean applyNotNullVendorExtension(CodegenProperty itemsProperty, String dataType) {
        if (contains(dataType, "@NotNull")) {
            return false;
        }

        return booleanExtension(itemsProperty, VENDOR_EXTENSION_NOT_NULL);
    }

    private boolean booleanExtension(CodegenProperty itemsProperty, String name) {
        if (itemsProperty == null || itemsProperty.getVendorExtensions() == null) {
            return false;
        }

        try {
            return Boolean.parseBoolean(String.valueOf(itemsProperty.getVendorExtensions().getOrDefault(name, "false")));
        } catch (Exception e) {
            return false;
        }
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

        final var serializerTemplate = "BigDecimalCustomSerializer";
        this.supportingFiles.add(new SupportingFile(
            serializerTemplate + ".mustache",
            (sourceFolder + File.separator + modelPackage).replace(".", java.io.File.separator),
            serializerTemplate + ".java"
        ));
        this.importMapping.put(serializerTemplate, modelPackage + "." + serializerTemplate);

        final boolean useJackson3 = convertPropertyToBoolean(USE_JACKSON_3);
        if (useJackson3) {
            this.importMapping.put(JSON_SERIALIZE, "tools.jackson.databind.annotation.JsonSerialize");
        } else {
            this.importMapping.put(JSON_SERIALIZE, "com.fasterxml.jackson.databind.annotation.JsonSerialize");
        }

        if (this.additionalProperties.containsKey(ADD_SERVLET_REQUEST)) {
            this.addServletRequest = convertPropertyToBoolean(ADD_SERVLET_REQUEST);
        }
        if (this.additionalProperties.containsKey(ADD_BINDING_RESULT)) {
            this.addBindingResult = convertPropertyToBoolean(ADD_BINDING_RESULT);
        }

        writePropertyBack(ADD_SERVLET_REQUEST, this.addServletRequest);
        writePropertyBack(ADD_BINDING_RESULT, this.addBindingResult);

        this.additionalProperties.put("indent4", new IndentedLambda(4, " ", true, true));
        this.additionalProperties.put("newLine4", new NewLineIndent(4, " "));
        this.additionalProperties.put("indent8", new IndentedLambda(8, " ", true, true));
        this.additionalProperties.put("newLine8", new NewLineIndent(8, " "));
        this.additionalProperties.put("toOneLine", new FormatToOneLine());
        this.additionalProperties.put("trimAndIndent4", new TrimAndIndent(4, " "));
        this.additionalProperties.put(UNWRAP_ESCAPED_QUOTES, new UnwrapEscapedQuotes());
    }

    @Override
    public void postProcessParameter(CodegenParameter p) {
        super.postProcessParameter(p);
        if (p.isContainer && !this.reactive) {
            p.baseType = p.dataType.replaceAll("^([^<]+)<.+>$", "$1");
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
        if (operation.getExtensions() == null) {
            operation.setExtensions(new LinkedHashMap<>());
        }
        final CodegenOperation codegenOperation = super.fromOperation(path, httpMethod, operation, servers);
        if (this.addServletRequest) {
            final CodegenParameter codegenParameter = new CodegenParameter() {
                public boolean isHttpServletRequest = true;
            };
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
            .map(CodegenValueType::getValue)
            .orElseGet(() -> super.toDefaultValue(cp, referencedSchema));
    }

    @Override
    public void postProcessModelProperty(CodegenModel model, CodegenProperty property) {

        super.postProcessModelProperty(model, property);

        if (shouldSerializeBigDecimalAsString(property)) {
            property.vendorExtensions.put("x-extra-annotation", "@JsonSerialize(using = BigDecimalCustomSerializer.class)");
            model.imports.add("BigDecimalCustomSerializer");
            model.imports.add(JSON_SERIALIZE);
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
}
