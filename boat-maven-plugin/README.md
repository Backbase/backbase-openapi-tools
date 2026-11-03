# boat maven plugin.

The `boat` plugin has multiple goals:

## boat:generate

Open API Generator based on https://github.com/OpenAPITools/openapi-generator/tree/master/modules/openapi-generator-maven-plugin. All configuration options as 
defined on openapi-generator-maven-plugin can be applied here too. 

Boat maven plugin uses slightly modified templates for html, java and webclient that help generate specs and clients that work best in a Backbase projects.

All inputSpec parameters for this goal may additionally be configured as an artifact. See [Example inputMavenArtifact parameter](#example-inputMavenArtifact-parameter) or [integration tests](https://github.com/Backbase/backbase-openapi-tools/tree/main/boat-maven-plugin/src/it/example/boat-artifact-input) for examples.

The input spec may also be an AsyncAPI 3.0 document, see [AsyncAPI 3.0](#asyncapi-30) and the `projectionOpenApiVersion`
parameter.

## boat:generate-spring-boot-embedded

Same with `generate` but with opinionated defaults for Spring

    <configuration>
        <inputSpec>${project.basedir}/../api/product-service-api/src/main/resources/openapi.yaml</inputSpec>
        <apiPackage>com.backbase.product.api.service.v2</apiPackage>
        <modelPackage>com.backbase.product.api.service.v2.model</modelPackage>
    </configuration>

... is the same as:

    <configuration>
        <output>${project.build.directory}/generated-sources/openapi</output>
        <generateSupportingFiles>true</generateSupportingFiles>
        <generatorName>spring-boat</generatorName>
        <strictSpec>true</strictSpec>
        <generateApiTests>false</generateApiTests>
        <generateModelTests>false</generateModelTests>
        <inputSpec>${project.basedir}/../api/product-service-api/src/main/resources/openapi.yaml</inputSpec>
        <configOptions>
            <library>spring-boot</library>
            <dateLibrary>java8</dateLibrary>
            <interfaceOnly>true</interfaceOnly>
            <skipDefaultInterface>true</skipDefaultInterface>
            <useBeanValidation>true</useBeanValidation>
            <useTags>true</useTags>
            <java8>true</java8>
            <useOptional>false</useOptional>
            <apiPackage>com.backbase.product.api.service.v2</apiPackage>
            <modelPackage>com.backbase.product.api.service.v2.model</modelPackage>
            <useJakartaEe>true</useJakartaEe>
            <useSpringBoot3>true</useSpringBoot3>
            <containerDefaultToNull>false</containerDefaultToNull>
        </configOptions>
    </configuration>

... explicit `configOptions` override default ones, e.g. in sample below `containerDefaultToNull` overrides default (i.e. `false`) with `true`

    <configuration>
        <inputSpec>${project.basedir}/../api/product-service-api/src/main/resources/openapi.yaml</inputSpec>
        <apiPackage>com.backbase.product.api.service.v2</apiPackage>
        <modelPackage>com.backbase.product.api.service.v2.model</modelPackage>
        <configOptions>
            <containerDefaultToNull>true</containerDefaultToNull>
        </configOptions>
    </configuration>

## boat:generate-spring-boot-embedded-webhooks

Same with `generate-spring-boot-embedded` but with opinionated defaults for Webhooks
It will generate webhook interfaces with prehook and posthook request mapping for each endpoint in the OpenAPI Specs.

    <configuration>
        <output>${project.build.directory}/generated-sources/openapi</output>
        <generateSupportingFiles>true</generateSupportingFiles>
        <generatorName>boat-webhooks</generatorName>
        <strictSpec>true</strictSpec>
        <generateApiTests>false</generateApiTests>
        <generateModelTests>false</generateModelTests>
        <inputSpec>${project.basedir}/../api/product-service-api/src/main/resources/openapi.yaml</inputSpec>
        <configOptions>
            <dateLibrary>java8</dateLibrary>
            <interfaceOnly>true</interfaceOnly>
            <skipDefaultInterface>true</skipDefaultInterface>
            <useBeanValidation>true</useBeanValidation>
            <useTags>true</useTags>
            <java8>true</java8>
            <useOptional>false</useOptional>
            <apiPackage>com.backbase.product.api.service.v2</apiPackage>
            <modelPackage>com.backbase.product.api.service.v2.model</modelPackage>
            <useJakartaEe>true</useJakartaEe>
            <useSpringBoot3>true</useSpringBoot3>
            <containerDefaultToNull>false</containerDefaultToNull>
        </configOptions>
    </configuration>

## boat:generate-rest-template-embedded

Same with `generate` but with opinionated defaults for Rest Template Client

    <configuration>
        <output>${project.build.directory}/generated-sources/openapi</output>
        <generateSupportingFiles>true</generateSupportingFiles>
        <generatorName>java</generatorName>
        <strictSpec>true</strictSpec>
        <generateApiTests>false</generateApiTests>
        <generateModelTests>false</generateModelTests>
        <inputSpec>${project.basedir}/../api/product-service-api/src/main/resources/openapi.yaml</inputSpec>
        <configOptions>
            <library>resttemplate</library>
            <dateLibrary>java8</dateLibrary>
            <interfaceOnly>true</interfaceOnly>
            <skipDefaultInterface>true</skipDefaultInterface>
            <useBeanValidation>true</useBeanValidation>
            <useTags>true</useTags>
            <java8>true</java8>
            <useOptional>false</useOptional>
            <apiPackage>com.backbase.goldensample.product.api.client.v2</apiPackage>
            <modelPackage>com.backbase.goldensample.product.api.client.v2.model</modelPackage>
        </configOptions>
    </configuration>

### Sample usage with additional feature

    <configuration>
        ...
        <additionalProperties>
            <additionalProperty>restTemplateBeanName=interServiceRestTemplate</additionalProperty>
        </additionalProperties>
    </configuration>

### Single request parameter

When enabled, BOAT generates a `*Param` inner class for multi-parameter API operations and uses it as
the method's single argument instead of a positional parameter list. Operations with zero or one parameter
remain unchanged.

    <configuration>
        ...
        <configOptions>
            <useSingleRequestParameter>true</useSingleRequestParameter>
        </configOptions>
    </configuration>

### Property & Enum Name Mappings (new)

Two new optional parameters are supported by the BOAT plugin (mirroring OpenAPI Generator capabilities) to rename generated members without post-processing:

    nameMappings        A map of property names to their new names.
    enumNameMappings    A map of enum (symbol) names to their new names.

You can configure them directly as top-level plugin parameters OR via `configOptions` using the underlying generator keys `name-mappings` and `enum-name-mappings`.

Supported input formats:
1. Top-level list form (preferred for clarity):

    <configuration>
        ...
        <nameMappings>
            <nameMapping>old_property_name=NewPropertyName</nameMapping>
            <nameMapping>id=identifier</nameMapping>
        </nameMappings>
        <enumNameMappings>
            <enumNameMapping>old_enum_name=NewEnumName</enumNameMapping>
            <enumNameMapping>Status=ApiStatus</enumNameMapping>
        </enumNameMappings>
    </configuration>

2. Via system properties / CLI (helpful for experimentation):

    mvn clean compile \
      -Dopenapi.generator.maven.plugin.nameMappings=old_property_name=NewPropertyName,id=identifier \
      -Dopenapi.generator.maven.plugin.enumNameMappings=old_enum_name=NewEnumName,Status=ApiStatus

Rules & notes:
- Key/value pairs use '='. Whitespace around keys/values is trimmed.
- For multiple entries in system property form, separate by comma.
- Duplicate keys: last one wins.
- These mappings apply after any model / type renames from other mapping options.
- `nameMappings` affects property (field) identifiers in generated models & parameter names (where supported by the underlying generator).
- `enumNameMappings` affects enum constant identifiers (not their serialized wire values). To change wire values, adjust the OpenAPI schema itself.

Example combining with other options:

    <plugin>
      <groupId>com.backbase.oss</groupId>
      <artifactId>boat-maven-plugin</artifactId>
      <configuration>
        <nameMappings>
          <nameMapping>created_at=createdAt</nameMapping>
          <nameMapping>updated_at=updatedAt</nameMapping>
        </nameMappings>
        <enumNameMappings>
          <enumNameMapping>status=ResourceStatus</enumNameMapping>
        </enumNameMappings>
      </configuration>
    </plugin>

## boat:generate-webclient-embedded

Same with `generate` but with opinionated defaults for Web Client

    <configuration>
        <output>${project.build.directory}/generated-sources/openapi</output>
        <generateSupportingFiles>true</generateSupportingFiles>
        <generatorName>java</generatorName>
        <strictSpec>true</strictSpec>
        <generateApiTests>false</generateApiTests>
        <generateModelTests>false</generateModelTests>
        <inputSpec>${project.basedir}/../api/product-service-api/src/main/resources/openapi.yaml</inputSpec>
        <configOptions>
            <library>webclient</library>
            <dateLibrary>java8</dateLibrary>
            <interfaceOnly>true</interfaceOnly>
            <skipDefaultInterface>true</skipDefaultInterface>
            <useBeanValidation>true</useBeanValidation>
            <useTags>true</useTags>
            <java8>true</java8>
            <useOptional>false</useOptional>
            <apiPackage>com.backbase.goldensample.product.api.client.v2</apiPackage>
            <modelPackage>com.backbase.goldensample.product.api.client.v2.model</modelPackage>
        </configOptions>
    </configuration>

## boat:generate-spring-cloud-stream-embedded

Generates [Spring Cloud Stream](https://spring.io/projects/spring-cloud-stream) producers and consumers from an
[AsyncAPI 3.0](https://www.asyncapi.com/docs/reference/specification/v3.0.0) document. It uses the `boat-spring-cloud-stream`
generator with the same opinionated defaults as `generate-spring-boot-embedded` (Spring Boot 3, Jakarta EE, bean validation,
`useTags`), except that `interfaceOnly` defaults to `false`.

    <configuration>
        <inputSpec>${project.basedir}/src/main/resources/asyncapi.yaml</inputSpec>
        <apiPackage>com.backbase.orders.events</apiPackage>
        <modelPackage>com.backbase.orders.model</modelPackage>
    </configuration>

The document does not say whether it describes a producer or a consumer: the `action` of every operation decides what
is generated. Classes are grouped by the operation tags, or by the channel when an operation has no tags.

* `receive` operations become methods of the `{Tag}Handler` interface. Implement it as a Spring bean;
  `{Tag}FunctionsConfiguration` registers a `Consumer<Message<T>>` bean per operation, named after the operation id, so its
  binding is `{operationId}-in-0`. When the operation has a `reply`, the handler method returns the reply and the bean is a
  `Function` that sends it on `{operationId}-out-0`, or to the address in the reply `address.location` header through the
  `spring.cloud.stream.sendto.destination` header. Returning `null` sends no reply. The correlation id header is copied to
  the reply.
* `send` operations become methods of `{Tag}Publisher`, which sends through `StreamBridge` to the output binding
  `{operationId}-out-0`. For a channel with address parameters, such as `orders.{region}.status`, the method takes the
  parameters and sends to the resolved address. A `send` operation with a `reply` also gets a
  `{operationId}Reply` method in the handler for the replies.
* Message payloads and headers become models. A payload that references `#/components/schemas/X` uses model `X`; other
  payloads become `{MessageName}Payload` and headers become `{MessageName}Headers`. Typed headers are read from and
  written to the message headers by the generated `AsyncApiHeaders`.
* `src/main/resources/asyncapi-bindings.properties` lists `spring.cloud.function.definition`,
  `spring.cloud.stream.output-bindings` and the destination of every binding. Import it in the application configuration
  with `spring.config.import=classpath:asyncapi-bindings.properties` and add the generated resources directory as a
  resource root, or copy the values.

Additional `configOptions`:

    inverseOperations (Default: false)
        Generate the other party of the document: send operations become receive operations and vice versa.
        Use it to generate a consumer from the document of a producer, or a test double.

    generateBindingProperties (Default: true)
        Generate asyncapi-bindings.properties.

    interfaceOnly (Default: false)
        Only generate the handler interfaces and the models.

Notes on Spring Cloud Stream:

* A receive operation on a parameterized address subscribes to a wildcard destination, e.g. `orders.*.status`. Configure
  the binder to treat it as a pattern (Kafka: `destination-is-pattern` with a regular expression, RabbitMQ:
  `binding-routing-key`). The generator logs a warning for these operations.
* Replies to an address taken from a message header use `spring.cloud.stream.sendto.destination`, which the binder must
  be allowed to create (dynamic destinations).
* A reply address that comes from the payload cannot be resolved by the generated code; the generator logs a warning
  and the reply goes to `{operationId}-out-0`.

See the [AsyncAPI integration tests](https://github.com/Backbase/backbase-openapi-tools/tree/main/boat-maven-plugin/src/it/example/boat-generate/asyncapi)
for working examples.

## AsyncAPI 3.0

The plugin recognizes AsyncAPI 3.0 documents (a root `asyncapi` field) wherever it reads specs. AsyncAPI 2.x documents
are rejected. For code generation the AsyncAPI document must be a local file or come from `inputMavenArtifact`; an
`inputSpec` URL is always read as OpenAPI.

* Message payloads and headers, and component schemas declared as Multi Format Schema Object, may use the AsyncAPI and
  OpenAPI 3.0 schema formats, or JSON Schema: `application/schema+json` or `application/schema+yaml`, with `version`
  `draft-04`, `draft-06`, `draft-07`, `draft-2019-09` or `draft-2020-12`, or without a version. Other formats, such as
  Avro, fail the generation when an operation uses them.
* For code generation the document is projected onto an OpenAPI document with one operation per AsyncAPI operation and
  message, which BOAT bundles and de-duplicates as usual. The projection is written to
  `${output}/.boat/asyncapi-projected.yaml` for troubleshooting. The `projectionOpenApiVersion` parameter (default
  `3.0.3`) sets its OpenAPI version. Use `3.1.0` for JSON Schema 2019-09 and 2020-12, and for keywords OpenAPI 3.0
  cannot express, such as `const`, `if`/`then` or a numeric `exclusiveMinimum`. Keep `3.0.x` for draft-04, whose boolean
  `exclusiveMinimum`/`exclusiveMaximum` and `id` mean something else in OpenAPI 3.1. Warnings name the schemas that
  need another version; schema files referenced from a schema are checked too.
* `boat:generate` with any other generator (`spring`, `java`, ...) generates only the models of an AsyncAPI document;
  `generateApis` is turned off with a log message. The BOAT Java generators (`spring`, `java`, `boat-webhooks`) also
  generate only `BigDecimalCustomSerializer.java` of their supporting files, unless `supportingFilesToGenerate` is set.
  For other generators, configure `supportingFilesToGenerate` or `generateSupportingFiles` yourself.
* `copyTo` locations ending in `META-INF/openapi/openapi.yaml` are changed to `META-INF/asyncapi/asyncapi.yaml` for AsyncAPI
  input, so a module can ship both documents.

### Sharing schemas between OpenAPI and AsyncAPI

An OpenAPI and an AsyncAPI document can reference the same schema files. Register them under `components/schemas` with
the same keys in both documents, and let payloads reference the components rather than the files:

    # openapi.yaml                          # asyncapi.yaml
    components:                             components:
      schemas:                                schemas:
        Order:                                  Order:
          $ref: lib/schemas/order.yaml            $ref: lib/schemas/order.yaml
                                                messages:
                                                  OrderPlaced:
                                                    payload:
                                                      $ref: '#/components/schemas/Order'

Schema files written for another JSON Schema draft are registered as Multi Format Schema Objects; the component key
still names the model, and files they reference resolve to the components registered for them:

    components:
      schemas:
        Order:
          schemaFormat: application/schema+yaml;version=draft-2020-12
          schema:
            $ref: lib/schemas/order.yaml

`boat:bundle` keeps the `schemaFormat` of these components.

Both documents then produce a model named `Order`. Generate them with generators of the same family (for example
`generate-spring-boot-embedded` and `generate-spring-cloud-stream-embedded`) so the model classes are identical. There
are two ways to use the shared models.

Inline: generate both documents into the same `modelPackage` and output directory. The embedded goals don't overwrite
existing files, so each shared model is generated once:

    <execution>
        <id>rest</id>
        <goals><goal>generate-spring-boot-embedded</goal></goals>
        <configuration>
            <inputSpec>${project.basedir}/src/main/resources/openapi.yaml</inputSpec>
            <apiPackage>com.backbase.orders.api</apiPackage>
            <modelPackage>com.backbase.orders.model</modelPackage>
        </configuration>
    </execution>
    <execution>
        <id>events</id>
        <goals><goal>generate-spring-cloud-stream-embedded</goal></goals>
        <configuration>
            <inputSpec>${project.basedir}/src/main/resources/asyncapi.yaml</inputSpec>
            <apiPackage>com.backbase.orders.events</apiPackage>
            <modelPackage>com.backbase.orders.model</modelPackage>
        </configuration>
    </execution>

Reuse: map the shared schemas to existing classes, for example from the REST API models in the same module or in a
dependency. Mapped schemas are not generated again; only the message specific models are generated in the events
`modelPackage`:

    <execution>
        <id>events</id>
        <goals><goal>generate-spring-cloud-stream-embedded</goal></goals>
        <configuration>
            <inputSpec>${project.basedir}/src/main/resources/asyncapi.yaml</inputSpec>
            <apiPackage>com.backbase.orders.events</apiPackage>
            <modelPackage>com.backbase.orders.events.model</modelPackage>
            <schemaMappings>
                <schemaMapping>Order=com.backbase.orders.model.Order</schemaMapping>
                <schemaMapping>Money=com.backbase.orders.model.Money</schemaMapping>
            </schemaMappings>
        </configuration>
    </execution>

## boat:decompose

Merges any components using allOf references.

## boat:diff

Calculates a Change log for APIs.

## boat:remove-deprecated

Removes deprecated elements in an OpenAPI spec.

## boat:validate

Validates OpenAPI and AsyncAPI 3.0 specs.

Configuration can point to a specific file, or a directory. When a directory is specified all files with a `.yaml` 
extension are validated. `failOnWarning` specifies whether to fail the build when validation violations are found,
otherwise, warnings are written to the log for everyone to ignore.

AsyncAPI documents are validated against the AsyncAPI 3.0 JSON Schema, after which BOAT checks that operations reference
declared channels and messages, that address parameters are declared and that schema formats are supported.

    <configuration>
        <input>${project.build.outputDirectory}/specs/</input>
        <failOnWarning>true</failOnWarning>
    </configuration>

## boat:lint

API lint which provides checks for compliance with many of Backbase's API standards

AsyncAPI documents are not linted; they are skipped with a warning. `boat:doc` (for directories) and `boat:diff` (for
batches) skip them in the same way.

Available parameters:

    failOnWarning (Default: false)
        Set this to true to fail in case a warning is found.

    ignoreRules
        List of rules ids which will be ignored.

    inputSpec
        Required: true
        Input spec directory or file.
        Optionaly inputMavenArtifact parameter can be used instead to configure an artifact input.
        
    inputMavenArtifact
        Input spec artifact
        
    output (Default: ${project.build.directory}/boat-lint-reports)
        Output directory for lint reports.

    showIgnoredRules (Default: false)
        Set this to true to show the list of ignored rules..
   
    writeLintReport (Default: true)
        Set this to true to generate lint report.

Example:

    <configuration>
        <inputSpec>${unversioned-filename-spec-dir}/</inputSpec>
        <output>${project.build.directory}/boat-lint-reports</output>
        <writeLintReport>true</writeLintReport>
        <ignoreRules>${ignored-lint-rules}</ignoreRules>
        <showIgnoredRules>true</showIgnoredRules>
    </configuration>
    
To see details and an example of inputMavenArtifact:
 [Example inputMavenArtifact parameter](#example-inputMavenArtifact-parameter)

To see details about this goal:

    mvn help:describe -DgroupId=com.backbase.oss -DartifactId=boat-maven-plugin  -Dgoal=lint -Ddetail`
    
    
## boat:bundle

Bundles a spec by resolving external references.

Configuration can point to a single in- and output file, or to in- and output directories. When directories are
specified, all files specified by the `includes` parameter are bundled.

Available parameters:

    input (Default: ${project.basedir}/src/main/resources)
        Required: true
        Path to a directory or a file to indicate the input Open API files to bundle.

    includes (Default: **/openapi.yaml, **/*api*.yaml)
        Required: false
        List of file patterns to include to bundle.

    excludes (Default: **/lib/**)
        Required: false
        List of file patterns to exclude to bundle.

    output (Default: ${project.build.directory}/openapi)
        Required: true
        Output directory for the bundled OpenAPI specs.

    flattenOutput (Default: false)
        Required: false
        Flatten the output directory structure, i.e. the bundle API specs are directly put into the output folder, even if the respective `input` files are located within a subdirectory in the `input`.

    version
        Required: false
        Optional parameter, to override the bundled spec version.

    versionFileName (Default: false)
        Required: false
        Optional parameter to include the version in the bundled spec file name.
        
    removeExtensions (Default: "")
        Required: false
        Optional parameter to remove extensions from the bundled spec.

    projectionOpenApiVersion (Default: 3.0.3)
        Required: false
        OpenAPI version the schemas of an AsyncAPI document are bundled with, as for boat:generate.

Examples in `json` files are parsed to objects.

AsyncAPI 3.0 documents are bundled too: external references are inlined, payload and header schemas are moved to
`components/schemas` (de-duplicated when `deduplicateSchemas` is `true`) and messages reference them. `version`,
`versionFileName` and `removeExtensions` apply as for OpenAPI. The default `includes` already match `asyncapi.yaml`.
Set `projectionOpenApiVersion` to the value used for generation, for example `3.1.0` for JSON Schema 2020-12 schemas,
so keywords such as `const` are kept.

    <configuration>
        <skip>${bundle.skip}</skip>
        <input>${project.basedir}/src/main/resources/</input>
        <output>${project.build.outputDirectory}/specs/</output>
        <includes>*-api-v*.yaml</includes>
        <removeExtensions>
            <extension>x-extra-annotations</extension>
            <extension>x-implements</extension>
        </removeExtensions>
    </configuration>

For more information, run 

    mvn help:describe -Dplugin=com.backbase.oss:boat-maven-plugin -Dgoal=bundle -Ddetail

Configuration example

```$xml
   <build>
       <plugins>
            <plugin>
                <groupId>com.backbase.oss</groupId>
                <artifactId>boat-maven-plugin</artifactId>
                <version>0.1.0.0-SNAPSHOT</version>
                <configuration>
                    <!-- Showing defaults - do not configure defaults! -->
                    <!-- The input directory -->
                    <input>${project.basedir}/src/main/resources</input>
                    <!-- The output directory -->
                    <output>${project.basedir}/target/openapi</output>
                    <!-- Whether to fail the build on errors -->
                    <failOnError>true</failOnError>
                    <!-- Override the default server entry -->
                    <servers>
                        <server>
                            <url>http://localhost:4010</url>
                            <description>mock-api-server</description>
                        </server>
                    </servers>
                    <!-- Add additional properties ('additions') element to specified types -->
                    <addAdditionalProperties>
                        <schema>User</schema>
                        <schema>UserItem</schema>
                    </addAdditionalProperties>
                    <!-- Adding 'x-java-type' extension when json schema includes 'javaType' (default false) -->
                    <addJavaTypeExtensions>false</addJavaTypeExtensions>
                    <!-- Convert request and response body examples to yaml (default true) -->
                    <convertJsonExamplesToYaml>false</convertJsonExamplesToYaml>
                </configuration>
            </plugin>
        </plugins>
    </build>
```

Usage

    mvn boat:generate

Or hook up to your build process by adding ```executions``` configuration.

## boat:radio

Upload specs (one of more) to Boat-Bay.

Available parameters:

    artifactId (Default: ${project.artifactId})
      User property: artifactId
      Project ArtifactId in Boat-Bay. Defaults to ${project.artifactId}

    boatBayPassword
      User property: boat.bay.password
      Defines the password of the username which can access the Boat-Bay upload
      API. Required if boat-bay APIs are protected.

    boatBayUrl
      Required: true
      User property: boat.bay.url
      Boat-Bay domain. eg. https://boatbay.mycompany.eu

    boatBayUsername
      User property: boat.bay.username
      Defines the username which can access Boat-Bay upload API. Required if
      boat-bay APIs are protected.

    failOnBoatBayErrorResponse (Default: false)
      User property: failOnBoatBayErrorResponse
      Fail the build if boatbay server returns an error

    failOnBreakingChange (Default: false)
      User property: failOnBreakingChange
      Fail the build for breaking changes in specs

    failOnLintViolation (Default: false)
      User property: failOnLintViolation
      Fail the build if the spec has lint violation (Violation with
      Severity.MUST)

    groupId (Default: ${project.groupId})
      User property: groupId
      Project GroupId in Boat-Bay. Defaults to ${project.groupId}

    portalKey
      Required: true
      User property: portalKey
      Project portal Identifier in Boat-Bay.

    radioOutput (Default:
    ${project.build.directory}/target/boat-radio-report)
      Output directory for boat-radio report.

    sourceKey
      Required: true
      User property: sourceKey
      Project source identifier in Boat-Bay.

    specs
      Required: true
      User property: specs
      Array of spec to be uploaded. Spec fields:
      
      key : Spec Key in Boat-Bay. Defaults to filename.lastIndexOf('-'). For
      example - By default my-service-api-v3.1.4.yaml would be evaluated to
      my-service-api
      
      name : Spec Name in Boat-Bay. Defaults to filename.
      
      inputSpec : Location of the OpenAPI spec, as URL or local file glob
      pattern. If the input is a local file, the value of this property is
      considered a glob pattern that must resolve to a unique file. The glob
      pattern allows to express the input specification in a version neutral
      way. For instance, if the actual file is my-service-api-v3.1.4.yaml the
      expression could be my-service-api-v*.yaml.

    version (Default: ${project.version})
      User property: version
      Project Version in Boat-Bay. Defaults to ${project.version}



Configuration example:

```$xml
     <execution>
      <id>upload-specs</id>
      <phase>install</phase>
      <goals>
       <goal>radio</goal>
      </goals>
      <configuration>
       <sourceKey>pet-store-bom</sourceKey>
       <portalKey>example</portalKey>
       <boatBayUrl>https://boatbay.backbase.eu</boatBayUrl>
       <username>admin</username>
       <password>admin</password>
       <specs>
        <spec>
         <inputSpec>${project.build.directory}/spec/bundled/pet-store-client-api-*.yaml</inputSpec>
        </spec>
       </specs>
      </configuration>
     </execution>
```

## boat:transform

Apply transformers to an existing specification.

Available parameters:

    inputs
      Required: true
      User property: boat.transform.inputs
      A list of input specifications.

    mappers
      File name mappers used to generate the output file name, instances of
      org.codehaus.plexus.components.io.filemappers.FileMapper.
      The following mappers can be used without needing to specify the FQCN of
      the implementation.
      
      regexp:
        org.codehaus.plexus.components.io.filemappers.RegExpFileMapper
      merge:
        org.codehaus.plexus.components.io.filemappers.MergeFileMapper
      prefix:
        org.codehaus.plexus.components.io.filemappers.PrefixFileMapper
      suffix:
        org.codehaus.plexus.components.io.filemappers.SuffixFileMapper
      
      The parameter defaults to
      
       <mappers>
            <suffix>-transformed</suffix>
       </mappers>

    options
      Additional options passed to transformers.

    output (Default: ${project.build.directory})
      User property: boat.transform.output
      Target directory of the transformed specifications.

    pipeline
      Required: true
      The list of transformers to be applied to each input specification.

    serverId
      User property: boat.transform.serverId
      Retrieves authorization from Maven's settings.xml.

    skip
      Alias: codegen.skip
      User property: boat.transform.skip
      Whether to skip the execution of this goal.

Configuration example

```$xml
    <configuration>
        <inputs>
            <input>${project.build.directory}/openapi.yaml</input>
        </inputs>
        <mappers>
            <merge>${spec.name}-${spec.version}.yaml</merge>
        </mappers>
        <pipeline>
            <setVersion>${spec.version}</setVersion>
            <extensionFilter>
                <remove>
                    <extension>abstract</extension>
                    <extension>implements</extension>
                    <extension>extra-annotation</extension>
                    <extension>extra-java-code</extension>
                </remove>
            </extensionFilter>
        </pipeline>
    </configuration>
```

## Example inputMavenArtifact parameter

Example:

```$xml
    <inputMavenArtifact>
        <groupId>com.backbase.oss.boat.example</groupId>
        <artifactId>openapi-zips</artifactId>
        <version>1.0.0-SNAPSHOT</version>
        <classifier>api</classifier>
        <type>zip</type>
        <fileName>presentation-client-api/openapi.yaml</fileName>
    </inputMavenArtifact>
```

Parameters:

    groupId
        Required: true
        Input artifacts groupId
    artifactId
        Required: true
        Input artifacts artifactId
    version
        Required: true
        Input artifacts version
    classifier
        Required: true
        Input artifacts classifier (must be api)
    type
        Required: true
        Input artifacts type (must be zip)
    fileName
        Required: true
        directory or file in artifact to be processed by goal
        

This parameter is available as a replacement for the inputSpec parameter in goals [generate](#boat:generate) and [lint](#boat:lint).

It downloads a copy of the artifact if it is not already present, and uses a specified spec (or directory of specs) 
from the artifact as the inputSpec for the goal. 

More examples can be found in integration tests.
