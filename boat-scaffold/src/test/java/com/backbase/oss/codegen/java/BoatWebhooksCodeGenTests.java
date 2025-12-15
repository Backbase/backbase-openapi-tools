package com.backbase.oss.codegen.java;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.Parameter;
import io.swagger.parser.OpenAPIParser;
import io.swagger.v3.parser.core.models.ParseOptions;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.openapitools.codegen.ClientOptInput;
import org.openapitools.codegen.DefaultGenerator;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;

class BoatWebhooksCodeGenTests {

    static final String PROP_BASE = BoatWebhooksCodeGenTests.class.getSimpleName() + ".";
    static final String TEST_OUTPUT = System.getProperty(PROP_BASE + "output", "target/boat-webhooks-codegen-tests");

    @BeforeAll
    static void before() throws IOException {
        Files.createDirectories(Paths.get(TEST_OUTPUT));
        FileUtils.deleteDirectory(new File(TEST_OUTPUT));
    }

    @Test
    void webhookWithCardsApi() throws IOException {
        var codegen = new BoatWebhooksCodeGen();
        var input = new File("src/test/resources/boat-spring/cardsapi.yaml");
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
    void toApiName_shouldReturnDefaultNameForEmptyString() {
        BoatWebhooksCodeGen codegen = new BoatWebhooksCodeGen();
        String result = codegen.toApiName("");
        assertEquals("WebhookDefaultApi", result);
    }

}
