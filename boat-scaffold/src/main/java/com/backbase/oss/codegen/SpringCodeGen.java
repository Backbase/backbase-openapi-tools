package com.backbase.oss.codegen;


import com.samskivert.mustache.Mustache;
import com.samskivert.mustache.Template;
import java.io.IOException;
import java.io.Writer;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import org.openapitools.codegen.CliOption;
import org.openapitools.codegen.CodegenModel;
import org.openapitools.codegen.CodegenParameter;
import org.openapitools.codegen.CodegenProperty;
import org.openapitools.codegen.templating.mustache.IndentedLambda;

public class SpringCodeGen extends org.openapitools.codegen.languages.SpringCodegen {
    public static final String USE_CLASS_LEVEL_BEAN_VALIDATION = "useClassLevelBeanValidation";
    public static final String ADD_SERVLET_REQUEST = "addServletRequest";
    public static final String USE_LOMBOK_ANNOTATIONS = "useLombokAnnotations";
    public static final String USE_SET_FOR_UNIQUE_ITEMS = "useSetForUniqueItems";
    public static final String OPENAPI_NULLABLE = "openApiNullable";

    static private class NewLineIndent implements Mustache.Lambda {
        private final int level;
        private final int space;

        NewLineIndent(int level, String space) {
            this.level = level;
            this.space = Character.codePointAt(space, 0);
        }

        @Override
        public void execute(Template.Fragment frag, Writer out) throws IOException {
            String text = frag.execute();

            if (text == null || text.isEmpty()) {
                return;
            }

            out.write(System.lineSeparator());
            out.write(StringUtils.repeat(new String(Character.toChars(space)), level));
            out.write(text);
        }
    }

    /**
     * Add @Validated to class-level Api interfaces. Defaults to false
     */
    @Setter
    @Getter
    protected boolean useClassLevelBeanValidation = false;

    /**
     * Adds a HttpServletRequest object to the API definition method.
     */
    @Setter
    @Getter
    protected boolean addServletRequest = false;

    /**
     * Add Lombok to class-level Api models. Defaults to false
     */
    @Setter
    @Getter
    protected boolean useLombokAnnotations = true;

    /**
     * "Enable OpenAPI Jackson Nullable library"
     */
    @Setter
    @Getter
    protected boolean openApiNullable = false;

    @Setter
    @Getter
    protected boolean useSetForUniqueItems = true;

    public SpringCodeGen() {
        cliOptions.add(CliOption.newBoolean(USE_CLASS_LEVEL_BEAN_VALIDATION,
            "Add @Validated to class-level Api interfaces", useClassLevelBeanValidation));
        cliOptions.add(CliOption.newBoolean(ADD_SERVLET_REQUEST,
            "Adds a HttpServletRequest object to the API definition method.", addServletRequest));
        cliOptions.add(CliOption.newBoolean(USE_LOMBOK_ANNOTATIONS,
            "Add Lombok to class-level Api models. Defaults to false.", useLombokAnnotations));
        cliOptions.add(CliOption.newBoolean(OPENAPI_NULLABLE,
            "Enable OpenAPI Jackson Nullable library", openApiNullable));
        cliOptions.add(CliOption.newBoolean(USE_SET_FOR_UNIQUE_ITEMS,
            "Use java.util.Set for arrays that have uniqueItems set to true", useSetForUniqueItems));

        apiNameSuffix = "Api";
    }

    @Override
    public String toApiName(String name) {
        if (name.length() == 0) {
            name = "default";
        }

        name = sanitizeName(name);

        return org.openapitools.codegen.utils.StringUtils.camelize(apiNamePrefix + "_" + name + "_" + apiNameSuffix);
    }

    @Override
    public void processOpts() {
        super.processOpts();

        supportingFiles.stream()
            .filter(sf -> "apiUtil.mustache".equals(sf.templateFile))
            .findAny()
            .ifPresent(supportingFiles::remove);

        if (additionalProperties.containsKey(USE_CLASS_LEVEL_BEAN_VALIDATION)) {
            useClassLevelBeanValidation = convertPropertyToBoolean(USE_CLASS_LEVEL_BEAN_VALIDATION);
        }
        if (additionalProperties.containsKey(ADD_SERVLET_REQUEST)) {
            addServletRequest = convertPropertyToBoolean(ADD_SERVLET_REQUEST);
        }
        if (additionalProperties.containsKey(USE_LOMBOK_ANNOTATIONS)) {
            useLombokAnnotations = convertPropertyToBoolean(USE_LOMBOK_ANNOTATIONS);
        }
        if (additionalProperties.containsKey(OPENAPI_NULLABLE)) {
            openApiNullable = convertPropertyToBoolean(OPENAPI_NULLABLE);
        }
        if (additionalProperties.containsKey(USE_SET_FOR_UNIQUE_ITEMS)) {
            useSetForUniqueItems = convertPropertyToBoolean(USE_SET_FOR_UNIQUE_ITEMS);
        }

        if (useSetForUniqueItems) {
            typeMapping.put("set", "java.util.Set");

            importMapping.put("Set", "java.util.Set");
            importMapping.put("LinkedHashSet", "java.util.LinkedHashSet");
        }

        additionalProperties.put("indent4", new IndentedLambda(4, " "));
        additionalProperties.put("newLine4", new NewLineIndent(4, " "));
        additionalProperties.put("indent8", new IndentedLambda(8, " "));
        additionalProperties.put("newLine8", new NewLineIndent(8, " "));
    }

    @Override
    public void postProcessModelProperty(CodegenModel model, CodegenProperty p) {
        super.postProcessModelProperty(model, p);

        if (p.isContainer) {
            if (useSetForUniqueItems && p.getUniqueItems()) {
                p.containerType = "set";
                p.baseType = "java.util.Set";
                p.dataType = "java.util.Set<" + p.items.dataType + ">";
                p.datatypeWithEnum = "java.util.Set<" + p.items.datatypeWithEnum + ">";
                p.defaultValue = "new " + "java.util.LinkedHashSet<>()";
            }
        }
    }

    @Override
    public void postProcessParameter(CodegenParameter p) {
        super.postProcessParameter(p);

        if (p.isContainer) {
            // XXX the model set this to the container type, why is this different?
            p.baseType = p.dataType.replaceAll("^([^<]+)<.+>$", "$1");

            if (useSetForUniqueItems && p.getUniqueItems()) {
                p.baseType = "java.util.Set";
                p.dataType = "java.util.Set<" + p.items.dataType + ">";
                p.datatypeWithEnum = "java.util.Set<" + p.items.datatypeWithEnum + ">";
                p.defaultValue = "new " + "java.util.LinkedHashSet<>()";
            }
        }
    }
}
