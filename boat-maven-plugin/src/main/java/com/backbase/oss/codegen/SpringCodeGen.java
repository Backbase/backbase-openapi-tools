package com.backbase.oss.codegen;

import static org.openapitools.codegen.utils.StringUtils.camelize;

import com.samskivert.mustache.Mustache;
import com.samskivert.mustache.Template.Fragment;
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
    public static final String OPENAPI_NULLABLE = "openApiNullable";

    static private class NewLineIndent implements Mustache.Lambda {
        private final int level;
        private final int space;

        NewLineIndent(int level, String space) {
            this.level = level;
            this.space = Character.codePointAt(space, 0);
        }

        @Override
        public void execute(Fragment frag, Writer out) throws IOException {
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
    protected boolean useLombokAnnotations = false;

    /**
     * "Enable OpenAPI Jackson Nullable library"
     */
    @Setter
    @Getter
    protected boolean openApiNullable = true;

    public SpringCodeGen() {
        cliOptions.add(CliOption.newBoolean(USE_CLASS_LEVEL_BEAN_VALIDATION,
            "Add @Validated to class-level Api interfaces", useClassLevelBeanValidation));
        cliOptions.add(CliOption.newBoolean(ADD_SERVLET_REQUEST,
            "Adds a HttpServletRequest object to the API definition method.", addServletRequest));
        cliOptions.add(CliOption.newBoolean(USE_LOMBOK_ANNOTATIONS,
            "Add Lombok to class-level Api models. Defaults to false.", useLombokAnnotations));
        cliOptions.add(CliOption.newBoolean(OPENAPI_NULLABLE,
            "Enable OpenAPI Jackson Nullable library", openApiNullable));

        apiNameSuffix = "Api";
        instantiationTypes.put("set", "LinkedHashSet");
    }

    @Override
    public String toApiName(String name) {
        if (name.length() == 0) {
            name = "default";
        }

        name = sanitizeName(name);

        return camelize(apiNamePrefix + "_" + name + "_" + apiNameSuffix);
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

        typeMapping.put("set", "Set");
        instantiationTypes.put("set", "LinkedHashSet");
        importMapping.put("Set", "java.util.Set");
        importMapping.put("LinkedHashSet", "java.util.LinkedHashSet");

        if (fullJavaUtil) {
            typeMapping.put("set", "java.util.Set");
            instantiationTypes.put("set", "java.util.LinkedHashSet");
            importMapping.remove("Set");
            importMapping.remove("LinkedHashSet");
        }

        additionalProperties.put("indent4", new IndentedLambda(4, " "));
        additionalProperties.put("newLine4", new NewLineIndent(4, " "));
        additionalProperties.put("indent8", new IndentedLambda(8, " "));
        additionalProperties.put("newLine8", new NewLineIndent(8, " "));
    }

    @Override
    public void postProcessModelProperty(CodegenModel model, CodegenProperty p) {
        super.postProcessModelProperty(model, p);

        if (p.isContainer && p.getUniqueItems()) {
            model.imports.add("Set");
            model.imports.add("LinkedHashSet");

            p.containerType = "set";
            p.baseType = javaUtilPrefix + "Set";
            p.dataType = javaUtilPrefix + "Set<" + p.items.dataType + ">";
            p.datatypeWithEnum = javaUtilPrefix + "Set<" + p.items.datatypeWithEnum + ">";
            p.defaultValue = "new " + javaUtilPrefix + "LinkedHashSet<>()";
        }
    }

    @Override
    public void postProcessParameter(CodegenParameter p) {
        super.postProcessParameter(p);

        if (p.isContainer) {
            if (p.getUniqueItems()) {
                p.baseType = javaUtilPrefix + "Set";
                p.dataType = javaUtilPrefix + "Set<" + p.items.dataType + ">";
                p.datatypeWithEnum = javaUtilPrefix + "Set<" + p.items.datatypeWithEnum + ">";
                p.defaultValue = "new " + javaUtilPrefix + "LinkedHashSet<>()";
            } else {
                // XXX the model set this to the container type, why is this set to the element type for parameters?
                p.baseType = p.dataType.replaceAll("^(.+)<.+>$", "$1");
            }
        }
    }
}
