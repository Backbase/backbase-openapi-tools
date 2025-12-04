package com.backbase.oss.codegen.java;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.samskivert.mustache.Template;
import io.swagger.parser.OpenAPIParser;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.parser.core.models.ParseOptions;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.openapitools.codegen.ClientOptInput;
import org.openapitools.codegen.CodegenModel;
import org.openapitools.codegen.CodegenOperation;
import org.openapitools.codegen.CodegenProperty;
import org.openapitools.codegen.DefaultGenerator;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

import static com.backbase.oss.codegen.java.BoatWebhooksCodeGen.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class BoatWebhooksCodeGenTests {

    static final String PROP_BASE = BoatWebhooksCodeGenTests.class.getSimpleName() + ".";
    static final String TEST_OUTPUT = System.getProperty(PROP_BASE + "output", "target/boat-webhooks-codegen-tests");

    @BeforeAll
    static void before() throws IOException {
        Files.createDirectories(Paths.get(TEST_OUTPUT));
        FileUtils.deleteDirectory(new File(TEST_OUTPUT));
    }

    @Test
    void processOptsUseProtectedFields() {
        final BoatWebhooksCodeGen gen = new BoatWebhooksCodeGen();
        final Map<String, Object> options = gen.additionalProperties();

        options.put(USE_PROTECTED_FIELDS, "true");
        options.put(USE_CLASS_LEVEL_BEAN_VALIDATION, "true");
        options.put(ADD_SERVLET_REQUEST, "true");
        options.put(ADD_BINDING_RESULT, "true");
        options.put(USE_LOMBOK_ANNOTATIONS, "true");
        options.put(USE_WITH_MODIFIERS, "true");

        gen.processOpts();

        assertThat(gen.additionalProperties(), hasEntry("modelFieldsVisibility", "protected"));
    }

    @Test
    void newLineIndent() throws IOException {
        final BoatSpringCodeGen.NewLineIndent indent = new BoatSpringCodeGen.NewLineIndent(2, "_");
        final StringWriter output = new StringWriter();
        final Template.Fragment frag = mock(Template.Fragment.class);

        when(frag.execute()).thenReturn("\n Good \r\n   morning,  \r\n  Dave ");

        indent.execute(frag, output);

        assertThat(output.toString(), equalTo(String.format("__%n__Good%n__  morning,%n__ Dave%n")));
    }

    @Test
    void addServletRequestTestFromOperation(){
        final BoatWebhooksCodeGen gen = new BoatWebhooksCodeGen();
        gen.addServletRequest = true;
        CodegenOperation co = gen.fromOperation("/test", "POST", new Operation(), null);
        assertEquals(1, co.allParams.size());
        assertEquals("httpServletRequest", co.allParams.get(0).paramName);
    }

    @Test
    void webhookWithCardsApi() throws IOException {
        var codegen = new BoatWebhooksCodeGen();
        var input = new File("src/test/resources/boat-spring/cardsapi.yaml");
        codegen.setLibrary("spring-boot");
        codegen.setInterfaceOnly(true);
        codegen.setOutputDir(TEST_OUTPUT + "/cards");
        codegen.setInputSpec(input.getAbsolutePath());

        var openApiInput = new OpenAPIParser().readLocation(input.getAbsolutePath(), null, new ParseOptions()).getOpenAPI();
        var clientOptInput = new ClientOptInput();
        clientOptInput.config(codegen);
        clientOptInput.openAPI(openApiInput);

        List<File> files = new DefaultGenerator().opts(clientOptInput).generate();

        File testApi = files.stream().filter(file -> file.getName().equals("WebhookClientApi.java"))
                .findFirst()
                .get();
        MethodDeclaration testPostMethod = StaticJavaParser.parse(testApi)
                .findAll(MethodDeclaration.class)
                .get(1);

        Parameter contentParam = testPostMethod.getParameterByName("prehookRequest").get();
        assertThat(contentParam.getTypeAsString(), equalTo("PrehookRequest"));
    }

    @Test
    void testReplaceBeanValidationCollectionType() {
        var codegen = new BoatWebhooksCodeGen();
        codegen.setUseBeanValidation(true);
        var codegenProperty = new CodegenProperty();
        codegenProperty.isModel = true;
        codegenProperty.baseName = "request"; // not a response

        String result = codegen.replaceBeanValidationCollectionType(
                codegenProperty,"Set<com.backbase.dbs.arrangement.commons.model.TranslationItemDto>");
        assertEquals("Set<@Valid com.backbase.dbs.arrangement.commons.model.TranslationItemDto>", result);
    }

    @Test
    void testFromParameter() {
        BoatWebhooksCodeGen codeGen = new BoatWebhooksCodeGen();
        io.swagger.v3.oas.models.parameters.Parameter swaggerParam = new io.swagger.v3.oas.models.parameters.Parameter();
        swaggerParam.setName("testParam");
        Set<String> imports = new HashSet<>();

        // Call the method under test
        var result = codeGen.fromParameter(swaggerParam, imports);

        // Assert result is not null and has expected properties
        assertThat(result, notNullValue());
        assertEquals("testParam", result.baseName);
    }

    @Test
    void testPostProcessModelProperty_addsBigDecimalSerializerAnnotation() {
        BoatWebhooksCodeGen codeGen = new BoatWebhooksCodeGen();
        // Simulate the condition for shouldSerializeBigDecimalAsString to return true
        CodegenProperty property = new CodegenProperty();
        property.baseType = "BigDecimal";
        property.openApiType = "string";
        property.dataFormat = "number";
        CodegenModel model = new CodegenModel();
        model.imports = new HashSet<>();
        property.vendorExtensions = new HashMap<>();

        codeGen.setSerializeBigDecimalAsString(true);
        codeGen.postProcessModelProperty(model, property);

        assertThat(property.vendorExtensions, hasEntry("x-extra-annotation", "@JsonSerialize(using = BigDecimalCustomSerializer.class)"));
        assertThat(model.imports, hasItems("BigDecimalCustomSerializer", "JsonSerialize"));
    }
}
