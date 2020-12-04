package com.backbase.oss.boat;

import com.backbase.oss.boat.loader.OpenAPILoader;
import com.backbase.oss.boat.loader.OpenAPILoaderException;
import io.swagger.v3.oas.models.OpenAPI;
import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.checkerframework.checker.units.qual.A;
import org.codehaus.plexus.logging.console.ConsoleLogger;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.sonatype.plexus.build.incremental.DefaultBuildContext;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

@Slf4j
public class GeneratorTests {

    @Test
    public void testDereference() throws OpenAPILoaderException {
        String spec = System.getProperty("spec", getClass().getResource("/oas-examples/petstore.yaml").getFile());
        OpenAPI load = OpenAPILoader.load(new File(spec), true, false);
        System.out.println(load.toString());
        assertEquals(getExpectedApi(),load.toString());
    }

    @Test
    public void testHtml2() throws MojoExecutionException {

        String spec = System.getProperty("spec", getClass().getResource("/oas-examples/petstore.yaml").getFile());
        GenerateMojo mojo = new GenerateMojo();
        File input = new File(spec);
        File output = new File("target/boat-docs-generate");
        if (!output.exists()) {
            output.mkdirs();
        }

        DefaultBuildContext defaultBuildContext = new DefaultBuildContext();
        defaultBuildContext.enableLogging(new ConsoleLogger());

        mojo.getLog();
        mojo.buildContext = defaultBuildContext;
        mojo.project = new MavenProject();
        mojo.generatorName = "html2";
        mojo.inputSpec = input.getAbsolutePath();
        mojo.output = output;
        mojo.skip = false;
        mojo.skipIfSpecIsUnchanged = false;
        mojo.execute();
        String[] expectedGeneratedDocs = {"index.html",
                ".openapi-generator-ignore",".openapi-generator"};
        assertArrayEquals(expectedGeneratedDocs,output.list());

    }

    @Test
    public void testBoatDocs() throws MojoExecutionException {

        String spec = System.getProperty("spec", getClass().getResource("/oas-examples/petstore.yaml").getFile());

        log.info("Generating docs for: {}", spec);

        GenerateDocMojo mojo = new GenerateDocMojo();
        File input = new File(spec);
        File output = new File("target/boat-docs");
        if (!output.exists()) {
            output.mkdirs();
        }

        DefaultBuildContext defaultBuildContext = new DefaultBuildContext();
        defaultBuildContext.enableLogging(new ConsoleLogger());

        mojo.getLog();
        mojo.buildContext = defaultBuildContext;
        mojo.project = new MavenProject();
        mojo.inputSpec = input.getAbsolutePath();
        mojo.output = output;
        mojo.skip = false;
        mojo.skipIfSpecIsUnchanged = false;
        mojo.bundleSpecs = true;
        mojo.dereferenceComponents = true;
        mojo.execute();

        String[] expectedGeneratedDocs = {"index.html",
                ".openapi-generator-ignore",".openapi-generator"};
        assertArrayEquals(expectedGeneratedDocs,output.list());
    }

    @Test
    public void testBundledBoatDocs() throws MojoExecutionException, MojoFailureException {

        String spec = System.getProperty("spec", getClass().getResource("/oas-examples/petstore.yaml").getFile());

        log.info("Generating docs for: {}", spec);


        BundleMojo bundleMojo = new BundleMojo();
        bundleMojo.setInput(new File(spec));
        File dereferenced = new File("target/boat-docs-bundled/dereferenced-openapi.yml");
        bundleMojo.setOutput(dereferenced);
        bundleMojo.execute();


        GenerateDocMojo mojo = new GenerateDocMojo();
        File output = new File("target/boat-docs-bundled/");
        if (!output.exists()) {
            output.mkdirs();
        }

        DefaultBuildContext defaultBuildContext = new DefaultBuildContext();
        defaultBuildContext.enableLogging(new ConsoleLogger());

        mojo.getLog();
        mojo.buildContext = defaultBuildContext;
        mojo.project = new MavenProject();
        mojo.inputSpec = dereferenced.getAbsolutePath();
        mojo.output = output;
        mojo.skip = false;
        mojo.skipIfSpecIsUnchanged = false;
        mojo.bundleSpecs = false;
        mojo.dereferenceComponents = false;
        mojo.execute();
        String[] actualGeneratedFiles = output.list();
        Arrays.sort(actualGeneratedFiles);
        String[] expectedFiles= {".openapi-generator",".openapi-generator-ignore","dereferenced-openapi.yml","index.html"};
        assertArrayEquals(expectedFiles,actualGeneratedFiles);
    }


    @Test
    public void testBeanValidation() throws MojoExecutionException {
        GenerateMojo mojo = new GenerateMojo();

        String inputFile = getClass().getResource("/oas-examples/petstore.yaml").getFile();
        File input = new File(inputFile);
        File output = new File("target/spring-mvc");
        if (!output.exists()) {
            output.mkdirs();
        }

        DefaultBuildContext defaultBuildContext = new DefaultBuildContext();
        defaultBuildContext.enableLogging(new ConsoleLogger());

        Map<String,String> configOption = new HashMap<>();
        configOption.put("library", "spring-mvc");
        configOption.put("dateLibrary", "java8");
        configOption.put("apiPackage", "com.backbase.accesscontrol.service.rest.spec.api");
        configOption.put("modelPackage", "com.backbase.accesscontrol.service.rest.spec.model");
        configOption.put("useBeanValidation", "true");
        configOption.put("useClassLevelBeanValidation", "false");


        mojo.getLog();
        mojo.buildContext = defaultBuildContext;
        mojo.project = new MavenProject();
        mojo.generatorName = "spring";
        mojo.configOptions = configOption;
        mojo.inputSpec = input.getAbsolutePath();
        mojo.output = output;
        mojo.skip = false;
        mojo.skipIfSpecIsUnchanged = false;
        mojo.execute();
        String[] actualFilesGenerated = output.list();
        Arrays.sort(actualFilesGenerated);
        String[] expected = {".openapi-generator",".openapi-generator-ignore","README.md","pom.xml","src"};
        assertArrayEquals(expected,actualFilesGenerated);

    }

    @Test
    public void testWebClient() throws MojoExecutionException {
        GenerateWebClientEmbeddedMojo mojo = new GenerateWebClientEmbeddedMojo();

        String inputFile = getClass().getResource("/oas-examples/petstore.yaml").getFile();
        File input = new File(inputFile);
        File output = new File("target/webclient");
        if (!output.exists()) {
            output.mkdirs();
        }

        DefaultBuildContext defaultBuildContext = new DefaultBuildContext();
        defaultBuildContext.enableLogging(new ConsoleLogger());

        mojo.buildContext = defaultBuildContext;
        mojo.project = new MavenProject();
        mojo.inputSpec = input.getAbsolutePath();
        mojo.output = output;
        mojo.skip = false;
        mojo.skipIfSpecIsUnchanged = false;
        mojo.execute();

        String[] actualFilesGenerated = output.list();
        Arrays.sort(actualFilesGenerated);
        String[] expected = {".openapi-generator",".openapi-generator-ignore","api","gradle","src"};
        assertArrayEquals(expected,actualFilesGenerated);



    }

    private String getExpectedApi(){
        String expectedApi= "class OpenAPI {\n" +
                "    openapi: 3.0.0\n" +
                "    info: class Info {\n" +
                "        title: Swagger Petstore\n" +
                "        description: null\n" +
                "        termsOfService: null\n" +
                "        contact: null\n" +
                "        license: class License {\n" +
                "            name: MIT\n" +
                "            url: null\n" +
                "        }\n" +
                "        version: 1.0.0\n" +
                "    }\n" +
                "    externalDocs: null\n" +
                "    servers: [class Server {\n" +
                "        url: http://petstore.swagger.io/v1\n" +
                "        description: null\n" +
                "        variables: null\n" +
                "    }]\n" +
                "    security: null\n" +
                "    tags: null\n" +
                "    paths: class Paths {\n" +
                "        {/pets=class PathItem {\n" +
                "            summary: null\n" +
                "            description: null\n" +
                "            get: class Operation {\n" +
                "                tags: [pets]\n" +
                "                summary: List all pets\n" +
                "                description: null\n" +
                "                externalDocs: null\n" +
                "                operationId: listPets\n" +
                "                parameters: [class QueryParameter {\n" +
                "                    class Parameter {\n" +
                "                        name: limit\n" +
                "                        in: null\n" +
                "                        description: How many items to return at one time (max 100)\n" +
                "                        required: false\n" +
                "                        deprecated: null\n" +
                "                        allowEmptyValue: null\n" +
                "                        style: form\n" +
                "                        explode: true\n" +
                "                        allowReserved: null\n" +
                "                        schema: class IntegerSchema {\n" +
                "                            class Schema {\n" +
                "                                type: integer\n" +
                "                                format: int32\n" +
                "                                $ref: null\n" +
                "                                description: null\n" +
                "                                title: null\n" +
                "                                multipleOf: null\n" +
                "                                maximum: null\n" +
                "                                exclusiveMaximum: null\n" +
                "                                minimum: null\n" +
                "                                exclusiveMinimum: null\n" +
                "                                maxLength: null\n" +
                "                                minLength: null\n" +
                "                                pattern: null\n" +
                "                                maxItems: null\n" +
                "                                minItems: null\n" +
                "                                uniqueItems: null\n" +
                "                                maxProperties: null\n" +
                "                                minProperties: null\n" +
                "                                required: null\n" +
                "                                not: null\n" +
                "                                properties: null\n" +
                "                                additionalProperties: null\n" +
                "                                nullable: null\n" +
                "                                readOnly: null\n" +
                "                                writeOnly: null\n" +
                "                                example: null\n" +
                "                                externalDocs: null\n" +
                "                                deprecated: null\n" +
                "                                discriminator: null\n" +
                "                                xml: null\n" +
                "                            }\n" +
                "                        }\n" +
                "                        examples: null\n" +
                "                        example: null\n" +
                "                        content: null\n" +
                "                        $ref: null\n" +
                "                    }\n" +
                "                    in: query\n" +
                "                }]\n" +
                "                requestBody: null\n" +
                "                responses: class ApiResponses {\n" +
                "                    {200=class ApiResponse {\n" +
                "                        description: A paged array of pets\n" +
                "                        headers: {x-next=class Header {\n" +
                "                            description: A link to the next page of responses\n" +
                "                            required: null\n" +
                "                            deprecated: null\n" +
                "                            style: simple\n" +
                "                            explode: false\n" +
                "                            schema: class StringSchema {\n" +
                "                                class Schema {\n" +
                "                                    type: string\n" +
                "                                    format: null\n" +
                "                                    $ref: null\n" +
                "                                    description: null\n" +
                "                                    title: null\n" +
                "                                    multipleOf: null\n" +
                "                                    maximum: null\n" +
                "                                    exclusiveMaximum: null\n" +
                "                                    minimum: null\n" +
                "                                    exclusiveMinimum: null\n" +
                "                                    maxLength: null\n" +
                "                                    minLength: null\n" +
                "                                    pattern: null\n" +
                "                                    maxItems: null\n" +
                "                                    minItems: null\n" +
                "                                    uniqueItems: null\n" +
                "                                    maxProperties: null\n" +
                "                                    minProperties: null\n" +
                "                                    required: null\n" +
                "                                    not: null\n" +
                "                                    properties: null\n" +
                "                                    additionalProperties: null\n" +
                "                                    nullable: null\n" +
                "                                    readOnly: null\n" +
                "                                    writeOnly: null\n" +
                "                                    example: null\n" +
                "                                    externalDocs: null\n" +
                "                                    deprecated: null\n" +
                "                                    discriminator: null\n" +
                "                                    xml: null\n" +
                "                                }\n" +
                "                            }\n" +
                "                            examples: null\n" +
                "                            example: null\n" +
                "                            content: null\n" +
                "                            $ref: null\n" +
                "                        }}\n" +
                "                        content: class Content {\n" +
                "                            {application/json=class MediaType {\n" +
                "                                schema: class ArraySchema {\n" +
                "                                    class Schema {\n" +
                "                                        type: array\n" +
                "                                        format: null\n" +
                "                                        $ref: null\n" +
                "                                        description: null\n" +
                "                                        title: null\n" +
                "                                        multipleOf: null\n" +
                "                                        maximum: null\n" +
                "                                        exclusiveMaximum: null\n" +
                "                                        minimum: null\n" +
                "                                        exclusiveMinimum: null\n" +
                "                                        maxLength: null\n" +
                "                                        minLength: null\n" +
                "                                        pattern: null\n" +
                "                                        maxItems: null\n" +
                "                                        minItems: null\n" +
                "                                        uniqueItems: null\n" +
                "                                        maxProperties: null\n" +
                "                                        minProperties: null\n" +
                "                                        required: null\n" +
                "                                        not: null\n" +
                "                                        properties: null\n" +
                "                                        additionalProperties: null\n" +
                "                                        nullable: null\n" +
                "                                        readOnly: null\n" +
                "                                        writeOnly: null\n" +
                "                                        example: null\n" +
                "                                        externalDocs: null\n" +
                "                                        deprecated: null\n" +
                "                                        discriminator: null\n" +
                "                                        xml: null\n" +
                "                                    }\n" +
                "                                    items: class ObjectSchema {\n" +
                "                                        class Schema {\n" +
                "                                            type: object\n" +
                "                                            format: null\n" +
                "                                            $ref: null\n" +
                "                                            description: null\n" +
                "                                            title: null\n" +
                "                                            multipleOf: null\n" +
                "                                            maximum: null\n" +
                "                                            exclusiveMaximum: null\n" +
                "                                            minimum: null\n" +
                "                                            exclusiveMinimum: null\n" +
                "                                            maxLength: null\n" +
                "                                            minLength: null\n" +
                "                                            pattern: null\n" +
                "                                            maxItems: null\n" +
                "                                            minItems: null\n" +
                "                                            uniqueItems: null\n" +
                "                                            maxProperties: null\n" +
                "                                            minProperties: null\n" +
                "                                            required: [id, name]\n" +
                "                                            not: null\n" +
                "                                            properties: {id=class IntegerSchema {\n" +
                "                                                class Schema {\n" +
                "                                                    type: integer\n" +
                "                                                    format: int64\n" +
                "                                                    $ref: null\n" +
                "                                                    description: null\n" +
                "                                                    title: null\n" +
                "                                                    multipleOf: null\n" +
                "                                                    maximum: null\n" +
                "                                                    exclusiveMaximum: null\n" +
                "                                                    minimum: null\n" +
                "                                                    exclusiveMinimum: null\n" +
                "                                                    maxLength: null\n" +
                "                                                    minLength: null\n" +
                "                                                    pattern: null\n" +
                "                                                    maxItems: null\n" +
                "                                                    minItems: null\n" +
                "                                                    uniqueItems: null\n" +
                "                                                    maxProperties: null\n" +
                "                                                    minProperties: null\n" +
                "                                                    required: null\n" +
                "                                                    not: null\n" +
                "                                                    properties: null\n" +
                "                                                    additionalProperties: null\n" +
                "                                                    nullable: null\n" +
                "                                                    readOnly: null\n" +
                "                                                    writeOnly: null\n" +
                "                                                    example: null\n" +
                "                                                    externalDocs: null\n" +
                "                                                    deprecated: null\n" +
                "                                                    discriminator: null\n" +
                "                                                    xml: null\n" +
                "                                                }\n" +
                "                                            }, name=class StringSchema {\n" +
                "                                                class Schema {\n" +
                "                                                    type: string\n" +
                "                                                    format: null\n" +
                "                                                    $ref: null\n" +
                "                                                    description: null\n" +
                "                                                    title: null\n" +
                "                                                    multipleOf: null\n" +
                "                                                    maximum: null\n" +
                "                                                    exclusiveMaximum: null\n" +
                "                                                    minimum: null\n" +
                "                                                    exclusiveMinimum: null\n" +
                "                                                    maxLength: null\n" +
                "                                                    minLength: null\n" +
                "                                                    pattern: null\n" +
                "                                                    maxItems: null\n" +
                "                                                    minItems: null\n" +
                "                                                    uniqueItems: null\n" +
                "                                                    maxProperties: null\n" +
                "                                                    minProperties: null\n" +
                "                                                    required: null\n" +
                "                                                    not: null\n" +
                "                                                    properties: null\n" +
                "                                                    additionalProperties: null\n" +
                "                                                    nullable: null\n" +
                "                                                    readOnly: null\n" +
                "                                                    writeOnly: null\n" +
                "                                                    example: null\n" +
                "                                                    externalDocs: null\n" +
                "                                                    deprecated: null\n" +
                "                                                    discriminator: null\n" +
                "                                                    xml: null\n" +
                "                                                }\n" +
                "                                            }, tag=class StringSchema {\n" +
                "                                                class Schema {\n" +
                "                                                    type: string\n" +
                "                                                    format: null\n" +
                "                                                    $ref: null\n" +
                "                                                    description: null\n" +
                "                                                    title: null\n" +
                "                                                    multipleOf: null\n" +
                "                                                    maximum: null\n" +
                "                                                    exclusiveMaximum: null\n" +
                "                                                    minimum: null\n" +
                "                                                    exclusiveMinimum: null\n" +
                "                                                    maxLength: null\n" +
                "                                                    minLength: null\n" +
                "                                                    pattern: null\n" +
                "                                                    maxItems: null\n" +
                "                                                    minItems: null\n" +
                "                                                    uniqueItems: null\n" +
                "                                                    maxProperties: null\n" +
                "                                                    minProperties: null\n" +
                "                                                    required: null\n" +
                "                                                    not: null\n" +
                "                                                    properties: null\n" +
                "                                                    additionalProperties: null\n" +
                "                                                    nullable: null\n" +
                "                                                    readOnly: null\n" +
                "                                                    writeOnly: null\n" +
                "                                                    example: null\n" +
                "                                                    externalDocs: null\n" +
                "                                                    deprecated: null\n" +
                "                                                    discriminator: null\n" +
                "                                                    xml: null\n" +
                "                                                }\n" +
                "                                            }}\n" +
                "                                            additionalProperties: null\n" +
                "                                            nullable: null\n" +
                "                                            readOnly: null\n" +
                "                                            writeOnly: null\n" +
                "                                            example: null\n" +
                "                                            externalDocs: null\n" +
                "                                            deprecated: null\n" +
                "                                            discriminator: null\n" +
                "                                            xml: null\n" +
                "                                        }\n" +
                "                                    }\n" +
                "                                }\n" +
                "                                examples: null\n" +
                "                                example: null\n" +
                "                                encoding: null\n" +
                "                            }}\n" +
                "                        }\n" +
                "                        links: null\n" +
                "                        extensions: null\n" +
                "                        $ref: null\n" +
                "                    }, default=class ApiResponse {\n" +
                "                        description: unexpected error\n" +
                "                        headers: null\n" +
                "                        content: class Content {\n" +
                "                            {application/json=class MediaType {\n" +
                "                                schema: class ObjectSchema {\n" +
                "                                    class Schema {\n" +
                "                                        type: object\n" +
                "                                        format: null\n" +
                "                                        $ref: null\n" +
                "                                        description: null\n" +
                "                                        title: null\n" +
                "                                        multipleOf: null\n" +
                "                                        maximum: null\n" +
                "                                        exclusiveMaximum: null\n" +
                "                                        minimum: null\n" +
                "                                        exclusiveMinimum: null\n" +
                "                                        maxLength: null\n" +
                "                                        minLength: null\n" +
                "                                        pattern: null\n" +
                "                                        maxItems: null\n" +
                "                                        minItems: null\n" +
                "                                        uniqueItems: null\n" +
                "                                        maxProperties: null\n" +
                "                                        minProperties: null\n" +
                "                                        required: [code, message]\n" +
                "                                        not: null\n" +
                "                                        properties: {code=class IntegerSchema {\n" +
                "                                            class Schema {\n" +
                "                                                type: integer\n" +
                "                                                format: int32\n" +
                "                                                $ref: null\n" +
                "                                                description: null\n" +
                "                                                title: null\n" +
                "                                                multipleOf: null\n" +
                "                                                maximum: null\n" +
                "                                                exclusiveMaximum: null\n" +
                "                                                minimum: null\n" +
                "                                                exclusiveMinimum: null\n" +
                "                                                maxLength: null\n" +
                "                                                minLength: null\n" +
                "                                                pattern: null\n" +
                "                                                maxItems: null\n" +
                "                                                minItems: null\n" +
                "                                                uniqueItems: null\n" +
                "                                                maxProperties: null\n" +
                "                                                minProperties: null\n" +
                "                                                required: null\n" +
                "                                                not: null\n" +
                "                                                properties: null\n" +
                "                                                additionalProperties: null\n" +
                "                                                nullable: null\n" +
                "                                                readOnly: null\n" +
                "                                                writeOnly: null\n" +
                "                                                example: null\n" +
                "                                                externalDocs: null\n" +
                "                                                deprecated: null\n" +
                "                                                discriminator: null\n" +
                "                                                xml: null\n" +
                "                                            }\n" +
                "                                        }, message=class StringSchema {\n" +
                "                                            class Schema {\n" +
                "                                                type: string\n" +
                "                                                format: null\n" +
                "                                                $ref: null\n" +
                "                                                description: null\n" +
                "                                                title: null\n" +
                "                                                multipleOf: null\n" +
                "                                                maximum: null\n" +
                "                                                exclusiveMaximum: null\n" +
                "                                                minimum: null\n" +
                "                                                exclusiveMinimum: null\n" +
                "                                                maxLength: null\n" +
                "                                                minLength: null\n" +
                "                                                pattern: null\n" +
                "                                                maxItems: null\n" +
                "                                                minItems: null\n" +
                "                                                uniqueItems: null\n" +
                "                                                maxProperties: null\n" +
                "                                                minProperties: null\n" +
                "                                                required: null\n" +
                "                                                not: null\n" +
                "                                                properties: null\n" +
                "                                                additionalProperties: null\n" +
                "                                                nullable: null\n" +
                "                                                readOnly: null\n" +
                "                                                writeOnly: null\n" +
                "                                                example: null\n" +
                "                                                externalDocs: null\n" +
                "                                                deprecated: null\n" +
                "                                                discriminator: null\n" +
                "                                                xml: null\n" +
                "                                            }\n" +
                "                                        }}\n" +
                "                                        additionalProperties: null\n" +
                "                                        nullable: null\n" +
                "                                        readOnly: null\n" +
                "                                        writeOnly: null\n" +
                "                                        example: null\n" +
                "                                        externalDocs: null\n" +
                "                                        deprecated: null\n" +
                "                                        discriminator: null\n" +
                "                                        xml: null\n" +
                "                                    }\n" +
                "                                }\n" +
                "                                examples: null\n" +
                "                                example: null\n" +
                "                                encoding: null\n" +
                "                            }}\n" +
                "                        }\n" +
                "                        links: null\n" +
                "                        extensions: null\n" +
                "                        $ref: null\n" +
                "                    }}\n" +
                "                    extensions: null\n" +
                "                }\n" +
                "                callbacks: null\n" +
                "                deprecated: null\n" +
                "                security: null\n" +
                "                servers: null\n" +
                "            }\n" +
                "            put: null\n" +
                "            post: class Operation {\n" +
                "                tags: [pets]\n" +
                "                summary: Create a pet\n" +
                "                description: null\n" +
                "                externalDocs: null\n" +
                "                operationId: createPets\n" +
                "                parameters: null\n" +
                "                requestBody: null\n" +
                "                responses: class ApiResponses {\n" +
                "                    {201=class ApiResponse {\n" +
                "                        description: Null response\n" +
                "                        headers: null\n" +
                "                        content: null\n" +
                "                        links: null\n" +
                "                        extensions: null\n" +
                "                        $ref: null\n" +
                "                    }, default=class ApiResponse {\n" +
                "                        description: unexpected error\n" +
                "                        headers: null\n" +
                "                        content: class Content {\n" +
                "                            {application/json=class MediaType {\n" +
                "                                schema: class ObjectSchema {\n" +
                "                                    class Schema {\n" +
                "                                        type: object\n" +
                "                                        format: null\n" +
                "                                        $ref: null\n" +
                "                                        description: null\n" +
                "                                        title: null\n" +
                "                                        multipleOf: null\n" +
                "                                        maximum: null\n" +
                "                                        exclusiveMaximum: null\n" +
                "                                        minimum: null\n" +
                "                                        exclusiveMinimum: null\n" +
                "                                        maxLength: null\n" +
                "                                        minLength: null\n" +
                "                                        pattern: null\n" +
                "                                        maxItems: null\n" +
                "                                        minItems: null\n" +
                "                                        uniqueItems: null\n" +
                "                                        maxProperties: null\n" +
                "                                        minProperties: null\n" +
                "                                        required: [code, message]\n" +
                "                                        not: null\n" +
                "                                        properties: {code=class IntegerSchema {\n" +
                "                                            class Schema {\n" +
                "                                                type: integer\n" +
                "                                                format: int32\n" +
                "                                                $ref: null\n" +
                "                                                description: null\n" +
                "                                                title: null\n" +
                "                                                multipleOf: null\n" +
                "                                                maximum: null\n" +
                "                                                exclusiveMaximum: null\n" +
                "                                                minimum: null\n" +
                "                                                exclusiveMinimum: null\n" +
                "                                                maxLength: null\n" +
                "                                                minLength: null\n" +
                "                                                pattern: null\n" +
                "                                                maxItems: null\n" +
                "                                                minItems: null\n" +
                "                                                uniqueItems: null\n" +
                "                                                maxProperties: null\n" +
                "                                                minProperties: null\n" +
                "                                                required: null\n" +
                "                                                not: null\n" +
                "                                                properties: null\n" +
                "                                                additionalProperties: null\n" +
                "                                                nullable: null\n" +
                "                                                readOnly: null\n" +
                "                                                writeOnly: null\n" +
                "                                                example: null\n" +
                "                                                externalDocs: null\n" +
                "                                                deprecated: null\n" +
                "                                                discriminator: null\n" +
                "                                                xml: null\n" +
                "                                            }\n" +
                "                                        }, message=class StringSchema {\n" +
                "                                            class Schema {\n" +
                "                                                type: string\n" +
                "                                                format: null\n" +
                "                                                $ref: null\n" +
                "                                                description: null\n" +
                "                                                title: null\n" +
                "                                                multipleOf: null\n" +
                "                                                maximum: null\n" +
                "                                                exclusiveMaximum: null\n" +
                "                                                minimum: null\n" +
                "                                                exclusiveMinimum: null\n" +
                "                                                maxLength: null\n" +
                "                                                minLength: null\n" +
                "                                                pattern: null\n" +
                "                                                maxItems: null\n" +
                "                                                minItems: null\n" +
                "                                                uniqueItems: null\n" +
                "                                                maxProperties: null\n" +
                "                                                minProperties: null\n" +
                "                                                required: null\n" +
                "                                                not: null\n" +
                "                                                properties: null\n" +
                "                                                additionalProperties: null\n" +
                "                                                nullable: null\n" +
                "                                                readOnly: null\n" +
                "                                                writeOnly: null\n" +
                "                                                example: null\n" +
                "                                                externalDocs: null\n" +
                "                                                deprecated: null\n" +
                "                                                discriminator: null\n" +
                "                                                xml: null\n" +
                "                                            }\n" +
                "                                        }}\n" +
                "                                        additionalProperties: null\n" +
                "                                        nullable: null\n" +
                "                                        readOnly: null\n" +
                "                                        writeOnly: null\n" +
                "                                        example: null\n" +
                "                                        externalDocs: null\n" +
                "                                        deprecated: null\n" +
                "                                        discriminator: null\n" +
                "                                        xml: null\n" +
                "                                    }\n" +
                "                                }\n" +
                "                                examples: null\n" +
                "                                example: null\n" +
                "                                encoding: null\n" +
                "                            }}\n" +
                "                        }\n" +
                "                        links: null\n" +
                "                        extensions: null\n" +
                "                        $ref: null\n" +
                "                    }}\n" +
                "                    extensions: null\n" +
                "                }\n" +
                "                callbacks: null\n" +
                "                deprecated: null\n" +
                "                security: null\n" +
                "                servers: null\n" +
                "            }\n" +
                "            delete: null\n" +
                "            options: null\n" +
                "            head: null\n" +
                "            patch: null\n" +
                "            trace: null\n" +
                "            servers: null\n" +
                "            parameters: null\n" +
                "            $ref: null\n" +
                "        }, /pets/{petId}=class PathItem {\n" +
                "            summary: null\n" +
                "            description: null\n" +
                "            get: class Operation {\n" +
                "                tags: [pets]\n" +
                "                summary: Info for a specific pet\n" +
                "                description: null\n" +
                "                externalDocs: null\n" +
                "                operationId: showPetById\n" +
                "                parameters: [class PathParameter {\n" +
                "                    class Parameter {\n" +
                "                        name: petId\n" +
                "                        in: null\n" +
                "                        description: The id of the pet to retrieve\n" +
                "                        required: null\n" +
                "                        deprecated: null\n" +
                "                        allowEmptyValue: null\n" +
                "                        style: simple\n" +
                "                        explode: false\n" +
                "                        allowReserved: null\n" +
                "                        schema: class StringSchema {\n" +
                "                            class Schema {\n" +
                "                                type: string\n" +
                "                                format: null\n" +
                "                                $ref: null\n" +
                "                                description: null\n" +
                "                                title: null\n" +
                "                                multipleOf: null\n" +
                "                                maximum: null\n" +
                "                                exclusiveMaximum: null\n" +
                "                                minimum: null\n" +
                "                                exclusiveMinimum: null\n" +
                "                                maxLength: null\n" +
                "                                minLength: null\n" +
                "                                pattern: null\n" +
                "                                maxItems: null\n" +
                "                                minItems: null\n" +
                "                                uniqueItems: null\n" +
                "                                maxProperties: null\n" +
                "                                minProperties: null\n" +
                "                                required: null\n" +
                "                                not: null\n" +
                "                                properties: null\n" +
                "                                additionalProperties: null\n" +
                "                                nullable: null\n" +
                "                                readOnly: null\n" +
                "                                writeOnly: null\n" +
                "                                example: null\n" +
                "                                externalDocs: null\n" +
                "                                deprecated: null\n" +
                "                                discriminator: null\n" +
                "                                xml: null\n" +
                "                            }\n" +
                "                        }\n" +
                "                        examples: null\n" +
                "                        example: null\n" +
                "                        content: null\n" +
                "                        $ref: null\n" +
                "                    }\n" +
                "                    in: path\n" +
                "                    required: true\n" +
                "                }]\n" +
                "                requestBody: null\n" +
                "                responses: class ApiResponses {\n" +
                "                    {200=class ApiResponse {\n" +
                "                        description: Expected response to a valid request\n" +
                "                        headers: null\n" +
                "                        content: class Content {\n" +
                "                            {application/json=class MediaType {\n" +
                "                                schema: class ObjectSchema {\n" +
                "                                    class Schema {\n" +
                "                                        type: object\n" +
                "                                        format: null\n" +
                "                                        $ref: null\n" +
                "                                        description: null\n" +
                "                                        title: null\n" +
                "                                        multipleOf: null\n" +
                "                                        maximum: null\n" +
                "                                        exclusiveMaximum: null\n" +
                "                                        minimum: null\n" +
                "                                        exclusiveMinimum: null\n" +
                "                                        maxLength: null\n" +
                "                                        minLength: null\n" +
                "                                        pattern: null\n" +
                "                                        maxItems: null\n" +
                "                                        minItems: null\n" +
                "                                        uniqueItems: null\n" +
                "                                        maxProperties: null\n" +
                "                                        minProperties: null\n" +
                "                                        required: [id, name]\n" +
                "                                        not: null\n" +
                "                                        properties: {id=class IntegerSchema {\n" +
                "                                            class Schema {\n" +
                "                                                type: integer\n" +
                "                                                format: int64\n" +
                "                                                $ref: null\n" +
                "                                                description: null\n" +
                "                                                title: null\n" +
                "                                                multipleOf: null\n" +
                "                                                maximum: null\n" +
                "                                                exclusiveMaximum: null\n" +
                "                                                minimum: null\n" +
                "                                                exclusiveMinimum: null\n" +
                "                                                maxLength: null\n" +
                "                                                minLength: null\n" +
                "                                                pattern: null\n" +
                "                                                maxItems: null\n" +
                "                                                minItems: null\n" +
                "                                                uniqueItems: null\n" +
                "                                                maxProperties: null\n" +
                "                                                minProperties: null\n" +
                "                                                required: null\n" +
                "                                                not: null\n" +
                "                                                properties: null\n" +
                "                                                additionalProperties: null\n" +
                "                                                nullable: null\n" +
                "                                                readOnly: null\n" +
                "                                                writeOnly: null\n" +
                "                                                example: null\n" +
                "                                                externalDocs: null\n" +
                "                                                deprecated: null\n" +
                "                                                discriminator: null\n" +
                "                                                xml: null\n" +
                "                                            }\n" +
                "                                        }, name=class StringSchema {\n" +
                "                                            class Schema {\n" +
                "                                                type: string\n" +
                "                                                format: null\n" +
                "                                                $ref: null\n" +
                "                                                description: null\n" +
                "                                                title: null\n" +
                "                                                multipleOf: null\n" +
                "                                                maximum: null\n" +
                "                                                exclusiveMaximum: null\n" +
                "                                                minimum: null\n" +
                "                                                exclusiveMinimum: null\n" +
                "                                                maxLength: null\n" +
                "                                                minLength: null\n" +
                "                                                pattern: null\n" +
                "                                                maxItems: null\n" +
                "                                                minItems: null\n" +
                "                                                uniqueItems: null\n" +
                "                                                maxProperties: null\n" +
                "                                                minProperties: null\n" +
                "                                                required: null\n" +
                "                                                not: null\n" +
                "                                                properties: null\n" +
                "                                                additionalProperties: null\n" +
                "                                                nullable: null\n" +
                "                                                readOnly: null\n" +
                "                                                writeOnly: null\n" +
                "                                                example: null\n" +
                "                                                externalDocs: null\n" +
                "                                                deprecated: null\n" +
                "                                                discriminator: null\n" +
                "                                                xml: null\n" +
                "                                            }\n" +
                "                                        }, tag=class StringSchema {\n" +
                "                                            class Schema {\n" +
                "                                                type: string\n" +
                "                                                format: null\n" +
                "                                                $ref: null\n" +
                "                                                description: null\n" +
                "                                                title: null\n" +
                "                                                multipleOf: null\n" +
                "                                                maximum: null\n" +
                "                                                exclusiveMaximum: null\n" +
                "                                                minimum: null\n" +
                "                                                exclusiveMinimum: null\n" +
                "                                                maxLength: null\n" +
                "                                                minLength: null\n" +
                "                                                pattern: null\n" +
                "                                                maxItems: null\n" +
                "                                                minItems: null\n" +
                "                                                uniqueItems: null\n" +
                "                                                maxProperties: null\n" +
                "                                                minProperties: null\n" +
                "                                                required: null\n" +
                "                                                not: null\n" +
                "                                                properties: null\n" +
                "                                                additionalProperties: null\n" +
                "                                                nullable: null\n" +
                "                                                readOnly: null\n" +
                "                                                writeOnly: null\n" +
                "                                                example: null\n" +
                "                                                externalDocs: null\n" +
                "                                                deprecated: null\n" +
                "                                                discriminator: null\n" +
                "                                                xml: null\n" +
                "                                            }\n" +
                "                                        }}\n" +
                "                                        additionalProperties: null\n" +
                "                                        nullable: null\n" +
                "                                        readOnly: null\n" +
                "                                        writeOnly: null\n" +
                "                                        example: null\n" +
                "                                        externalDocs: null\n" +
                "                                        deprecated: null\n" +
                "                                        discriminator: null\n" +
                "                                        xml: null\n" +
                "                                    }\n" +
                "                                }\n" +
                "                                examples: null\n" +
                "                                example: null\n" +
                "                                encoding: null\n" +
                "                            }}\n" +
                "                        }\n" +
                "                        links: null\n" +
                "                        extensions: null\n" +
                "                        $ref: null\n" +
                "                    }, default=class ApiResponse {\n" +
                "                        description: unexpected error\n" +
                "                        headers: null\n" +
                "                        content: class Content {\n" +
                "                            {application/json=class MediaType {\n" +
                "                                schema: class ObjectSchema {\n" +
                "                                    class Schema {\n" +
                "                                        type: object\n" +
                "                                        format: null\n" +
                "                                        $ref: null\n" +
                "                                        description: null\n" +
                "                                        title: null\n" +
                "                                        multipleOf: null\n" +
                "                                        maximum: null\n" +
                "                                        exclusiveMaximum: null\n" +
                "                                        minimum: null\n" +
                "                                        exclusiveMinimum: null\n" +
                "                                        maxLength: null\n" +
                "                                        minLength: null\n" +
                "                                        pattern: null\n" +
                "                                        maxItems: null\n" +
                "                                        minItems: null\n" +
                "                                        uniqueItems: null\n" +
                "                                        maxProperties: null\n" +
                "                                        minProperties: null\n" +
                "                                        required: [code, message]\n" +
                "                                        not: null\n" +
                "                                        properties: {code=class IntegerSchema {\n" +
                "                                            class Schema {\n" +
                "                                                type: integer\n" +
                "                                                format: int32\n" +
                "                                                $ref: null\n" +
                "                                                description: null\n" +
                "                                                title: null\n" +
                "                                                multipleOf: null\n" +
                "                                                maximum: null\n" +
                "                                                exclusiveMaximum: null\n" +
                "                                                minimum: null\n" +
                "                                                exclusiveMinimum: null\n" +
                "                                                maxLength: null\n" +
                "                                                minLength: null\n" +
                "                                                pattern: null\n" +
                "                                                maxItems: null\n" +
                "                                                minItems: null\n" +
                "                                                uniqueItems: null\n" +
                "                                                maxProperties: null\n" +
                "                                                minProperties: null\n" +
                "                                                required: null\n" +
                "                                                not: null\n" +
                "                                                properties: null\n" +
                "                                                additionalProperties: null\n" +
                "                                                nullable: null\n" +
                "                                                readOnly: null\n" +
                "                                                writeOnly: null\n" +
                "                                                example: null\n" +
                "                                                externalDocs: null\n" +
                "                                                deprecated: null\n" +
                "                                                discriminator: null\n" +
                "                                                xml: null\n" +
                "                                            }\n" +
                "                                        }, message=class StringSchema {\n" +
                "                                            class Schema {\n" +
                "                                                type: string\n" +
                "                                                format: null\n" +
                "                                                $ref: null\n" +
                "                                                description: null\n" +
                "                                                title: null\n" +
                "                                                multipleOf: null\n" +
                "                                                maximum: null\n" +
                "                                                exclusiveMaximum: null\n" +
                "                                                minimum: null\n" +
                "                                                exclusiveMinimum: null\n" +
                "                                                maxLength: null\n" +
                "                                                minLength: null\n" +
                "                                                pattern: null\n" +
                "                                                maxItems: null\n" +
                "                                                minItems: null\n" +
                "                                                uniqueItems: null\n" +
                "                                                maxProperties: null\n" +
                "                                                minProperties: null\n" +
                "                                                required: null\n" +
                "                                                not: null\n" +
                "                                                properties: null\n" +
                "                                                additionalProperties: null\n" +
                "                                                nullable: null\n" +
                "                                                readOnly: null\n" +
                "                                                writeOnly: null\n" +
                "                                                example: null\n" +
                "                                                externalDocs: null\n" +
                "                                                deprecated: null\n" +
                "                                                discriminator: null\n" +
                "                                                xml: null\n" +
                "                                            }\n" +
                "                                        }}\n" +
                "                                        additionalProperties: null\n" +
                "                                        nullable: null\n" +
                "                                        readOnly: null\n" +
                "                                        writeOnly: null\n" +
                "                                        example: null\n" +
                "                                        externalDocs: null\n" +
                "                                        deprecated: null\n" +
                "                                        discriminator: null\n" +
                "                                        xml: null\n" +
                "                                    }\n" +
                "                                }\n" +
                "                                examples: null\n" +
                "                                example: null\n" +
                "                                encoding: null\n" +
                "                            }}\n" +
                "                        }\n" +
                "                        links: null\n" +
                "                        extensions: null\n" +
                "                        $ref: null\n" +
                "                    }}\n" +
                "                    extensions: null\n" +
                "                }\n" +
                "                callbacks: null\n" +
                "                deprecated: null\n" +
                "                security: null\n" +
                "                servers: null\n" +
                "            }\n" +
                "            put: null\n" +
                "            post: null\n" +
                "            delete: null\n" +
                "            options: null\n" +
                "            head: null\n" +
                "            patch: null\n" +
                "            trace: null\n" +
                "            servers: null\n" +
                "            parameters: null\n" +
                "            $ref: null\n" +
                "        }}\n" +
                "    }\n" +
                "    components: class Components {\n" +
                "        schemas: {Pet=class ObjectSchema {\n" +
                "            class Schema {\n" +
                "                type: object\n" +
                "                format: null\n" +
                "                $ref: null\n" +
                "                description: null\n" +
                "                title: null\n" +
                "                multipleOf: null\n" +
                "                maximum: null\n" +
                "                exclusiveMaximum: null\n" +
                "                minimum: null\n" +
                "                exclusiveMinimum: null\n" +
                "                maxLength: null\n" +
                "                minLength: null\n" +
                "                pattern: null\n" +
                "                maxItems: null\n" +
                "                minItems: null\n" +
                "                uniqueItems: null\n" +
                "                maxProperties: null\n" +
                "                minProperties: null\n" +
                "                required: [id, name]\n" +
                "                not: null\n" +
                "                properties: {id=class IntegerSchema {\n" +
                "                    class Schema {\n" +
                "                        type: integer\n" +
                "                        format: int64\n" +
                "                        $ref: null\n" +
                "                        description: null\n" +
                "                        title: null\n" +
                "                        multipleOf: null\n" +
                "                        maximum: null\n" +
                "                        exclusiveMaximum: null\n" +
                "                        minimum: null\n" +
                "                        exclusiveMinimum: null\n" +
                "                        maxLength: null\n" +
                "                        minLength: null\n" +
                "                        pattern: null\n" +
                "                        maxItems: null\n" +
                "                        minItems: null\n" +
                "                        uniqueItems: null\n" +
                "                        maxProperties: null\n" +
                "                        minProperties: null\n" +
                "                        required: null\n" +
                "                        not: null\n" +
                "                        properties: null\n" +
                "                        additionalProperties: null\n" +
                "                        nullable: null\n" +
                "                        readOnly: null\n" +
                "                        writeOnly: null\n" +
                "                        example: null\n" +
                "                        externalDocs: null\n" +
                "                        deprecated: null\n" +
                "                        discriminator: null\n" +
                "                        xml: null\n" +
                "                    }\n" +
                "                }, name=class StringSchema {\n" +
                "                    class Schema {\n" +
                "                        type: string\n" +
                "                        format: null\n" +
                "                        $ref: null\n" +
                "                        description: null\n" +
                "                        title: null\n" +
                "                        multipleOf: null\n" +
                "                        maximum: null\n" +
                "                        exclusiveMaximum: null\n" +
                "                        minimum: null\n" +
                "                        exclusiveMinimum: null\n" +
                "                        maxLength: null\n" +
                "                        minLength: null\n" +
                "                        pattern: null\n" +
                "                        maxItems: null\n" +
                "                        minItems: null\n" +
                "                        uniqueItems: null\n" +
                "                        maxProperties: null\n" +
                "                        minProperties: null\n" +
                "                        required: null\n" +
                "                        not: null\n" +
                "                        properties: null\n" +
                "                        additionalProperties: null\n" +
                "                        nullable: null\n" +
                "                        readOnly: null\n" +
                "                        writeOnly: null\n" +
                "                        example: null\n" +
                "                        externalDocs: null\n" +
                "                        deprecated: null\n" +
                "                        discriminator: null\n" +
                "                        xml: null\n" +
                "                    }\n" +
                "                }, tag=class StringSchema {\n" +
                "                    class Schema {\n" +
                "                        type: string\n" +
                "                        format: null\n" +
                "                        $ref: null\n" +
                "                        description: null\n" +
                "                        title: null\n" +
                "                        multipleOf: null\n" +
                "                        maximum: null\n" +
                "                        exclusiveMaximum: null\n" +
                "                        minimum: null\n" +
                "                        exclusiveMinimum: null\n" +
                "                        maxLength: null\n" +
                "                        minLength: null\n" +
                "                        pattern: null\n" +
                "                        maxItems: null\n" +
                "                        minItems: null\n" +
                "                        uniqueItems: null\n" +
                "                        maxProperties: null\n" +
                "                        minProperties: null\n" +
                "                        required: null\n" +
                "                        not: null\n" +
                "                        properties: null\n" +
                "                        additionalProperties: null\n" +
                "                        nullable: null\n" +
                "                        readOnly: null\n" +
                "                        writeOnly: null\n" +
                "                        example: null\n" +
                "                        externalDocs: null\n" +
                "                        deprecated: null\n" +
                "                        discriminator: null\n" +
                "                        xml: null\n" +
                "                    }\n" +
                "                }}\n" +
                "                additionalProperties: null\n" +
                "                nullable: null\n" +
                "                readOnly: null\n" +
                "                writeOnly: null\n" +
                "                example: null\n" +
                "                externalDocs: null\n" +
                "                deprecated: null\n" +
                "                discriminator: null\n" +
                "                xml: null\n" +
                "            }\n" +
                "        }, Pets=class ArraySchema {\n" +
                "            class Schema {\n" +
                "                type: array\n" +
                "                format: null\n" +
                "                $ref: null\n" +
                "                description: null\n" +
                "                title: null\n" +
                "                multipleOf: null\n" +
                "                maximum: null\n" +
                "                exclusiveMaximum: null\n" +
                "                minimum: null\n" +
                "                exclusiveMinimum: null\n" +
                "                maxLength: null\n" +
                "                minLength: null\n" +
                "                pattern: null\n" +
                "                maxItems: null\n" +
                "                minItems: null\n" +
                "                uniqueItems: null\n" +
                "                maxProperties: null\n" +
                "                minProperties: null\n" +
                "                required: null\n" +
                "                not: null\n" +
                "                properties: null\n" +
                "                additionalProperties: null\n" +
                "                nullable: null\n" +
                "                readOnly: null\n" +
                "                writeOnly: null\n" +
                "                example: null\n" +
                "                externalDocs: null\n" +
                "                deprecated: null\n" +
                "                discriminator: null\n" +
                "                xml: null\n" +
                "            }\n" +
                "            items: class ObjectSchema {\n" +
                "                class Schema {\n" +
                "                    type: object\n" +
                "                    format: null\n" +
                "                    $ref: null\n" +
                "                    description: null\n" +
                "                    title: null\n" +
                "                    multipleOf: null\n" +
                "                    maximum: null\n" +
                "                    exclusiveMaximum: null\n" +
                "                    minimum: null\n" +
                "                    exclusiveMinimum: null\n" +
                "                    maxLength: null\n" +
                "                    minLength: null\n" +
                "                    pattern: null\n" +
                "                    maxItems: null\n" +
                "                    minItems: null\n" +
                "                    uniqueItems: null\n" +
                "                    maxProperties: null\n" +
                "                    minProperties: null\n" +
                "                    required: [id, name]\n" +
                "                    not: null\n" +
                "                    properties: {id=class IntegerSchema {\n" +
                "                        class Schema {\n" +
                "                            type: integer\n" +
                "                            format: int64\n" +
                "                            $ref: null\n" +
                "                            description: null\n" +
                "                            title: null\n" +
                "                            multipleOf: null\n" +
                "                            maximum: null\n" +
                "                            exclusiveMaximum: null\n" +
                "                            minimum: null\n" +
                "                            exclusiveMinimum: null\n" +
                "                            maxLength: null\n" +
                "                            minLength: null\n" +
                "                            pattern: null\n" +
                "                            maxItems: null\n" +
                "                            minItems: null\n" +
                "                            uniqueItems: null\n" +
                "                            maxProperties: null\n" +
                "                            minProperties: null\n" +
                "                            required: null\n" +
                "                            not: null\n" +
                "                            properties: null\n" +
                "                            additionalProperties: null\n" +
                "                            nullable: null\n" +
                "                            readOnly: null\n" +
                "                            writeOnly: null\n" +
                "                            example: null\n" +
                "                            externalDocs: null\n" +
                "                            deprecated: null\n" +
                "                            discriminator: null\n" +
                "                            xml: null\n" +
                "                        }\n" +
                "                    }, name=class StringSchema {\n" +
                "                        class Schema {\n" +
                "                            type: string\n" +
                "                            format: null\n" +
                "                            $ref: null\n" +
                "                            description: null\n" +
                "                            title: null\n" +
                "                            multipleOf: null\n" +
                "                            maximum: null\n" +
                "                            exclusiveMaximum: null\n" +
                "                            minimum: null\n" +
                "                            exclusiveMinimum: null\n" +
                "                            maxLength: null\n" +
                "                            minLength: null\n" +
                "                            pattern: null\n" +
                "                            maxItems: null\n" +
                "                            minItems: null\n" +
                "                            uniqueItems: null\n" +
                "                            maxProperties: null\n" +
                "                            minProperties: null\n" +
                "                            required: null\n" +
                "                            not: null\n" +
                "                            properties: null\n" +
                "                            additionalProperties: null\n" +
                "                            nullable: null\n" +
                "                            readOnly: null\n" +
                "                            writeOnly: null\n" +
                "                            example: null\n" +
                "                            externalDocs: null\n" +
                "                            deprecated: null\n" +
                "                            discriminator: null\n" +
                "                            xml: null\n" +
                "                        }\n" +
                "                    }, tag=class StringSchema {\n" +
                "                        class Schema {\n" +
                "                            type: string\n" +
                "                            format: null\n" +
                "                            $ref: null\n" +
                "                            description: null\n" +
                "                            title: null\n" +
                "                            multipleOf: null\n" +
                "                            maximum: null\n" +
                "                            exclusiveMaximum: null\n" +
                "                            minimum: null\n" +
                "                            exclusiveMinimum: null\n" +
                "                            maxLength: null\n" +
                "                            minLength: null\n" +
                "                            pattern: null\n" +
                "                            maxItems: null\n" +
                "                            minItems: null\n" +
                "                            uniqueItems: null\n" +
                "                            maxProperties: null\n" +
                "                            minProperties: null\n" +
                "                            required: null\n" +
                "                            not: null\n" +
                "                            properties: null\n" +
                "                            additionalProperties: null\n" +
                "                            nullable: null\n" +
                "                            readOnly: null\n" +
                "                            writeOnly: null\n" +
                "                            example: null\n" +
                "                            externalDocs: null\n" +
                "                            deprecated: null\n" +
                "                            discriminator: null\n" +
                "                            xml: null\n" +
                "                        }\n" +
                "                    }}\n" +
                "                    additionalProperties: null\n" +
                "                    nullable: null\n" +
                "                    readOnly: null\n" +
                "                    writeOnly: null\n" +
                "                    example: null\n" +
                "                    externalDocs: null\n" +
                "                    deprecated: null\n" +
                "                    discriminator: null\n" +
                "                    xml: null\n" +
                "                }\n" +
                "            }\n" +
                "        }, Error=class ObjectSchema {\n" +
                "            class Schema {\n" +
                "                type: object\n" +
                "                format: null\n" +
                "                $ref: null\n" +
                "                description: null\n" +
                "                title: null\n" +
                "                multipleOf: null\n" +
                "                maximum: null\n" +
                "                exclusiveMaximum: null\n" +
                "                minimum: null\n" +
                "                exclusiveMinimum: null\n" +
                "                maxLength: null\n" +
                "                minLength: null\n" +
                "                pattern: null\n" +
                "                maxItems: null\n" +
                "                minItems: null\n" +
                "                uniqueItems: null\n" +
                "                maxProperties: null\n" +
                "                minProperties: null\n" +
                "                required: [code, message]\n" +
                "                not: null\n" +
                "                properties: {code=class IntegerSchema {\n" +
                "                    class Schema {\n" +
                "                        type: integer\n" +
                "                        format: int32\n" +
                "                        $ref: null\n" +
                "                        description: null\n" +
                "                        title: null\n" +
                "                        multipleOf: null\n" +
                "                        maximum: null\n" +
                "                        exclusiveMaximum: null\n" +
                "                        minimum: null\n" +
                "                        exclusiveMinimum: null\n" +
                "                        maxLength: null\n" +
                "                        minLength: null\n" +
                "                        pattern: null\n" +
                "                        maxItems: null\n" +
                "                        minItems: null\n" +
                "                        uniqueItems: null\n" +
                "                        maxProperties: null\n" +
                "                        minProperties: null\n" +
                "                        required: null\n" +
                "                        not: null\n" +
                "                        properties: null\n" +
                "                        additionalProperties: null\n" +
                "                        nullable: null\n" +
                "                        readOnly: null\n" +
                "                        writeOnly: null\n" +
                "                        example: null\n" +
                "                        externalDocs: null\n" +
                "                        deprecated: null\n" +
                "                        discriminator: null\n" +
                "                        xml: null\n" +
                "                    }\n" +
                "                }, message=class StringSchema {\n" +
                "                    class Schema {\n" +
                "                        type: string\n" +
                "                        format: null\n" +
                "                        $ref: null\n" +
                "                        description: null\n" +
                "                        title: null\n" +
                "                        multipleOf: null\n" +
                "                        maximum: null\n" +
                "                        exclusiveMaximum: null\n" +
                "                        minimum: null\n" +
                "                        exclusiveMinimum: null\n" +
                "                        maxLength: null\n" +
                "                        minLength: null\n" +
                "                        pattern: null\n" +
                "                        maxItems: null\n" +
                "                        minItems: null\n" +
                "                        uniqueItems: null\n" +
                "                        maxProperties: null\n" +
                "                        minProperties: null\n" +
                "                        required: null\n" +
                "                        not: null\n" +
                "                        properties: null\n" +
                "                        additionalProperties: null\n" +
                "                        nullable: null\n" +
                "                        readOnly: null\n" +
                "                        writeOnly: null\n" +
                "                        example: null\n" +
                "                        externalDocs: null\n" +
                "                        deprecated: null\n" +
                "                        discriminator: null\n" +
                "                        xml: null\n" +
                "                    }\n" +
                "                }}\n" +
                "                additionalProperties: null\n" +
                "                nullable: null\n" +
                "                readOnly: null\n" +
                "                writeOnly: null\n" +
                "                example: null\n" +
                "                externalDocs: null\n" +
                "                deprecated: null\n" +
                "                discriminator: null\n" +
                "                xml: null\n" +
                "            }\n" +
                "        }}\n" +
                "        responses: null\n" +
                "        parameters: null\n" +
                "        examples: null\n" +
                "        requestBodies: null\n" +
                "        headers: null\n" +
                "        securitySchemes: null\n" +
                "        links: null\n" +
                "        callbacks: null\n" +
                "    }\n" +
                "}";
        return expectedApi;
    }
}
