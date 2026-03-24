package com.backbase.oss.codegen.java;

import com.backbase.oss.codegen.java.BoatCodeGenUtils.CodegenValueType;
import io.swagger.v3.oas.models.media.Schema;
import lombok.Getter;
import lombok.Setter;
import org.openapitools.codegen.CliOption;
import org.openapitools.codegen.CodegenProperty;
import org.openapitools.codegen.SupportingFile;
import org.openapitools.codegen.languages.JavaClientCodegen;
import org.openapitools.codegen.utils.ModelUtils;

import java.io.File;

import static com.backbase.oss.codegen.java.BoatCodeGenUtils.getCollectionCodegenValue;

public class BoatJavaCodeGen extends JavaClientCodegen {

    public static final String NAME = "boat-java";

    public static final String USE_JACKSON_CONVERSION = "useJacksonConversion";
    public static final String REST_TEMPLATE_BEAN_NAME = "restTemplateBeanName";
    @Setter
    @Getter
    protected boolean useJacksonConversion;
    @Getter
    @Setter
    protected String restTemplateBeanName;

    public BoatJavaCodeGen() {
        this.useJakartaEe = true;
        this.openapiNormalizer.put("REF_AS_PARENT_IN_ALLOF", "true");

        this.embeddedTemplateDir = this.templateDir = NAME;

        this.cliOptions.add(CliOption.newBoolean(USE_JACKSON_CONVERSION,
            "Whether to use Jackson to convert query parameters to String", this.useJacksonConversion));
        this.cliOptions.add(CliOption.newString(REST_TEMPLATE_BEAN_NAME,
            "An optional RestTemplate bean name"));

        // change default to match creating @Component
        this.setGenerateClientAsBean(true);
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public void processOpts() {
        super.processOpts();

        if (RESTTEMPLATE.equals(getLibrary())) {
            processRestTemplateOpts();
        }

        if (!getLibrary().startsWith("jersey")) {
            this.supportingFiles.removeIf(f -> f.getTemplateFile().equals("ServerConfiguration.mustache"));
            this.supportingFiles.removeIf(f -> f.getTemplateFile().equals("ServerVariable.mustache"));
        }

    }

    private void processRestTemplateOpts() {
        if (this.additionalProperties.containsKey(USE_JACKSON_CONVERSION)) {
            this.useJacksonConversion = convertPropertyToBoolean(USE_JACKSON_CONVERSION);
        }
        writePropertyBack(USE_JACKSON_CONVERSION, this.useJacksonConversion);
        if (this.useJacksonConversion) {
            this.supportingFiles.removeIf(f -> f.getTemplateFile().equals("RFC3339DateFormat.mustache"));
        }

        this.restTemplateBeanName = (String) this.additionalProperties.get(REST_TEMPLATE_BEAN_NAME);

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

    @Override
    public String getTypeDeclaration(Schema p) {
        String s = super.getTypeDeclaration(p);
        if (useBeanValidation && isArrayTypeOfEnum(p)) {
            s = s.replace("<", "<@Valid ");
        }
        return s;
    }

    private boolean isArrayTypeOfEnum(Schema s) {
        Schema<?> target = ModelUtils.isGenerateAliasAsModel() ? s : unaliasSchema(s);
        if (!ModelUtils.isArraySchema(target)) {
            return false;
        }
        Schema items = ModelUtils.getSchemaItems(target);
        if (items.get$ref() != null) {
            items = openAPI.getComponents().getSchemas().get(ModelUtils.getSimpleRef(items.get$ref()));
        }
        return items.getEnum() != null;
    }
}
