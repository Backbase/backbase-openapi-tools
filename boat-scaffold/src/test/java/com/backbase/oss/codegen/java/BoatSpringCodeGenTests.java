package com.backbase.oss.codegen.java;

import com.backbase.oss.codegen.java.BoatSpringCodeGen.NewLineIndent;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.samskivert.mustache.Template.Fragment;
import io.swagger.parser.OpenAPIParser;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.parser.core.models.ParseOptions;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.openapitools.codegen.*;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static com.backbase.oss.codegen.java.BoatSpringCodeGen.USE_PROTECTED_FIELDS;
import static java.util.stream.Collectors.groupingBy;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasEntry;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class BoatSpringCodeGenTests {

    static final String PROP_BASE = BoatSpringCodeGenTests.class.getSimpleName() + ".";
    static final String TEST_OUTPUT = System.getProperty(PROP_BASE + "output", "target/boat-spring-codegen-tests");

    @BeforeAll
    static void before() throws IOException {
        Files.createDirectories(Paths.get(TEST_OUTPUT));
        FileUtils.deleteDirectory(new File(TEST_OUTPUT));
    }

    @Test
    void clientOptsUnicity() {
        final BoatSpringCodeGen gen = new BoatSpringCodeGen();
        gen.cliOptions()
            .stream()
            .collect(groupingBy(CliOption::getOpt))
            .forEach((k, v) -> assertEquals(1, v.size(), k + " is described multiple times"));
    }


    @Test
    void processOptsUseProtectedFields() {
        final BoatJavaCodeGen gen = new BoatJavaCodeGen();
        final Map<String, Object> options = gen.additionalProperties();

        options.put(USE_PROTECTED_FIELDS, "true");

        gen.processOpts();

        assertThat(gen.additionalProperties(), hasEntry("modelFieldsVisibility", "protected"));
    }


    @Test
    void newLineIndent() throws IOException {
        final NewLineIndent indent = new BoatSpringCodeGen.NewLineIndent(2, "_");
        final StringWriter output = new StringWriter();
        final Fragment frag = mock(Fragment.class);

        when(frag.execute()).thenReturn("\n Good \r\n   morning,  \r\n  Dave ");

        indent.execute(frag, output);

        assertThat(output.toString(), equalTo(String.format("__%n__Good%n__  morning,%n__ Dave%n")));
    }

    @Test
    void addServletRequestTestFromOperation(){
        final BoatSpringCodeGen gen = new BoatSpringCodeGen();
        gen.addServletRequest = true;
        CodegenOperation co = gen.fromOperation("/test", "POST", new Operation(), null);
        assertEquals(1, co.allParams.size());
        assertEquals("httpServletRequest", co.allParams.get(0).paramName);
        assertTrue(Arrays.stream(co.allParams.get(0).getClass().getDeclaredFields()).anyMatch(f -> "isHttpServletRequest".equals(f.getName())));
    }

    @Test
    void multipartWithFileAndObject() throws IOException {
        var codegen = new BoatSpringCodeGen();
        var input = new File("src/test/resources/boat-spring/multipart.yaml");
        codegen.setLibrary("spring-boot");
        codegen.setInterfaceOnly(true);
        codegen.setOutputDir(TEST_OUTPUT + "/multipart");
        codegen.setInputSpec(input.getAbsolutePath());

        var openApiInput = new OpenAPIParser().readLocation(input.getAbsolutePath(), null, new ParseOptions()).getOpenAPI();
        var clientOptInput = new ClientOptInput();
        clientOptInput.config(codegen);
        clientOptInput.openAPI(openApiInput);

        List<File> files = new DefaultGenerator().opts(clientOptInput).generate();

        File testApi = files.stream().filter(file -> file.getName().equals("TestApi.java"))
            .findFirst()
            .get();
        MethodDeclaration testPostMethod = StaticJavaParser.parse(testApi)
            .findAll(MethodDeclaration.class)
            .get(1);

        Parameter filesParam = testPostMethod.getParameterByName("files").get();
        Parameter contentParam = testPostMethod.getParameterByName("content").get();

        assertTrue(filesParam.getAnnotationByName("RequestPart").isPresent());
        assertTrue(contentParam.getAnnotationByName("RequestPart").isPresent());
        assertThat(contentParam.getTypeAsString(), equalTo("TestObjectPart"));
        assertThat(filesParam.getTypeAsString(), equalTo("List<MultipartFile>"));
    }

    @Test
    void testReplaceBeanValidationCollectionType() {
        var codegen = new BoatSpringCodeGen();
        codegen.setUseBeanValidation(true);
        var codegenProperty = new CodegenProperty();
        codegenProperty.isModel = true;
        codegenProperty.baseName = "request"; // not a response

        String result = codegen.replaceBeanValidationCollectionType(
                codegenProperty,"Set<@Valid com.backbase.dbs.arrangement.commons.model.TranslationItemDto>");
        assertEquals("Set<com.backbase.dbs.arrangement.commons.model.@Valid TranslationItemDto>", result);
    }
}
