package com.backbase.oss.codegen.java;

import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.openapitools.codegen.ClientOptInput;
import org.openapitools.codegen.CodegenConstants;
import org.openapitools.codegen.DefaultGenerator;
import org.openapitools.codegen.config.CodegenConfigurator;
import org.openapitools.codegen.config.GlobalSettings;
import org.openapitools.codegen.languages.SpringCodegen;
import org.openapitools.codegen.languages.features.BeanValidationFeatures;
import org.openapitools.codegen.languages.features.OptionalFeatures;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;


public class BoatSpringTemplatesAllEnabled {
    static final String PROP_BASE = BoatSpringTemplatesAllEnabled.class.getSimpleName() + ".";
    static final String TEST_OUTPUT = System.getProperty(PROP_BASE + "output", "target/boat-spring-templates-tests");

    @BeforeAll
    static public void setUpClass() throws IOException {
        Files.createDirectories(Paths.get(TEST_OUTPUT));
        FileUtils.deleteDirectory(new File(TEST_OUTPUT, "src"));
    }

    @Test
    void test() {
        final File input = new File("src/test/resources/boat-spring/openapi.yaml");
        final CodegenConfigurator gcf = new CodegenConfigurator();

        gcf.setGeneratorName(BoatSpringCodeGen.NAME);
        gcf.setInputSpec(input.getAbsolutePath());
        gcf.setOutputDir(TEST_OUTPUT + "/Specific");

        GlobalSettings.setProperty(CodegenConstants.APIS, "");
        GlobalSettings.setProperty(CodegenConstants.API_DOCS, "true");
        GlobalSettings.setProperty(CodegenConstants.API_TESTS, "true");
        GlobalSettings.setProperty(CodegenConstants.MODELS, "");
        GlobalSettings.setProperty(CodegenConstants.MODEL_TESTS, "true");
        GlobalSettings.setProperty(CodegenConstants.MODEL_DOCS, "true");

        GlobalSettings.setProperty(CodegenConstants.SUPPORTING_FILES, "pom.xml");
        gcf.setApiNameSuffix("-api");

        gcf.addAdditionalProperty(OptionalFeatures.USE_OPTIONAL, true);

        gcf.addAdditionalProperty(BoatSpringCodeGen.USE_CLASS_LEVEL_BEAN_VALIDATION, true);
        gcf.addAdditionalProperty(BoatSpringCodeGen.ADD_SERVLET_REQUEST, true);
        gcf.addAdditionalProperty(BoatSpringCodeGen.ADD_BINDING_RESULT,true);
            gcf.addAdditionalProperty(BeanValidationFeatures.USE_BEANVALIDATION, true);
        gcf.addAdditionalProperty(BoatSpringCodeGen.USE_LOMBOK_ANNOTATIONS, true);
        gcf.addAdditionalProperty(BoatSpringCodeGen.OPENAPI_NULLABLE, true);
        gcf.addAdditionalProperty(BoatSpringCodeGen.USE_WITH_MODIFIERS, true);
        gcf.addAdditionalProperty(SpringCodegen.REACTIVE, false);

        final String destPackage = "com.backbase.oss.test";

        gcf.setApiPackage(destPackage + "api");
        gcf.setModelPackage(destPackage + "model");
        gcf.setInvokerPackage(destPackage + "invoker");

        gcf.addAdditionalProperty(SpringCodegen.BASE_PACKAGE, destPackage + "base");
        gcf.addAdditionalProperty(SpringCodegen.CONFIG_PACKAGE, destPackage + "config");

        gcf.addAdditionalProperty(CodegenConstants.HIDE_GENERATION_TIMESTAMP, true);
        gcf.addAdditionalProperty(SpringCodegen.INTERFACE_ONLY, false);
        gcf.addAdditionalProperty(SpringCodegen.USE_TAGS, true);
        gcf.addAdditionalProperty(SpringCodegen.SKIP_DEFAULT_INTERFACE, false);
        gcf.addAdditionalProperty(CodegenConstants.ARTIFACT_ID, "boat-templates-tests");
        gcf.addAdditionalProperty("additionalDependencies", ""
            + "        <dependency>\n"
            + "            <groupId>jakarta.persistence</groupId>\n"
            + "            <artifactId>jakarta.persistence-api</artifactId>\n"
            + "            <version>2.2.3</version>\n"
            + "        </dependency>\n"
            + "        <dependency>\n"
            + "            <groupId>jakarta.servlet</groupId>\n"
            + "            <artifactId>jakarta.servlet-api</artifactId>\n"
            + "            <version>4.0.4</version>\n"
            + "        </dependency>\n"
            + "        <dependency>\n"
            + "            <groupId>org.springframework.boot</groupId>\n"
            + "            <artifactId>spring-boot-starter-webflux</artifactId>\n"
            + "        </dependency>\n"
            + "        <dependency>\n"
            + "            <groupId>org.openapitools</groupId>\n"
            + "            <artifactId>jackson-databind-nullable</artifactId>\n"
            + "            <version>0.2.1</version>\n"
            + "        </dependency>\n"
            + "");

        gcf.setTemplateDir("boat-spring");

        final ClientOptInput coi = gcf.toClientOptInput();

        new DefaultGenerator().opts(coi).generate();
    }
}
