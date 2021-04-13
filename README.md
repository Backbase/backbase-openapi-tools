![Build](https://github.com/Backbase/backbase-openapi-tools/workflows/BOAT/badge.svg)
[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=com.backbase.oss%3Abackbase-openapi-tools&metric=alert_status)](https://sonarcloud.io/dashboard?id=com.backbase.oss%3Abackbase-openapi-tools)
[![Mvn Central](https://maven-badges.herokuapp.com/maven-central/com.backbase.oss/backbase-openapi-tools/badge.svg)](https://mvnrepository.com/artifact/com.backbase.oss/boat-maven-plugin)
# Backbase OpenApi Tools 

The Backbase Open API Tools is a collection of tools created to work efficiently with OpenAPI

It currently consists of

* RAML 1.0 Converter to OpenAPI 3.0 
* Create Diff Report between 2 OpenAPI versions of the same spec (Based on https://github.com/quen2404/openapi-diff)
* Decompose Transformer to remove Composed Schemas from OpenAPI specs to aid in code generators
* Case Transformer to see how your API looks like when going from camelCase to snake_case  (transforms examples too)
* [Code Generator](boat-maven-plugin/README.md) based on [openapi-generator.tech](https://openapi-generator.tech/) with optimized templates and fixes.
* Lint mojo based on Zalando Zally API Guidelines

The project is very much Work In Progress and will be published on maven central when considered ready enough. 

# Release Notes
BOAT is still under development and subject to change.

## 0.14.2
* *Angular Generator*
  * Added support for Angular version ranges in peer dependencies
  
## 0.14.1
* *Angular Generator*
  * Added support for Angular 11
  
## 0.14.0
* *Angular Generator*
  * Simplify Angular generator options by removing the unused ones (withInterfaces,providedInRoot)
  * fix Mock is not generated if Http status equals to 201 (SDK-2388)

## 0.13.0
* *Lint*
  * Added rules. 
    * Check plurals on paths resource names. 
    * Check prefix for paths should contain version.
  * Enabled rules.
    * Use Standard HTTP Status Codes.
## 0.12.0
* *General*
  * Improved code quality
  * Added more unit tests
  * Added more realistic example projects in boat-maven-plugin
  * Added help:describe support for boat-maven-plugin
  * Fixed building in Windows 
* *Boat Docs*
  * Render response examples from response instead of schema object
* *Lint*
  * Added additional rules. 
    * Check x-icon value in the info block.
    * Check info block description.
    * Check tags allowed.
    * Check info block title.
    * Check prefix for paths. i.e. "client-api", "service-api", "integration-api"
  * Ignore Zalando Ruleset by default in boat-maven-plugin
  
## 0.11.4
* *Java Templates*
  * Correct the import and use of @Qualifier in ApiClient template
  * Only change base type when date useSetForUniqueNames is set true
    
* *Boat Docs*
  * Handle examples for MediaTypes without Schemas (such as text/csv)
  
## 0.11.3

* *Angular Generator*
  * Apply the correct return type when multiple responses are present
  * Generate mocks for examples defined in dereferenced schemas
  
## 0.11.1

* *Java Generator - boat-spring library*
  * Fixed reactive spring templates
  * Avoid importing `HttpServletResponse` when using reactive
* *Angular Generator*
  * Handle empty bodies properly in Mock generation
  * Update foundation-ang to latest version

## 0.11.0

* *Maven Plugin*
   * Added `removeExtensions` mojo parameter to `boat:bundle` to filter out the given vendor extensions from bundle.
   * Added `includes` mojo parameter to `boat:bundle` as a glob pattern selecting the specification files (defaults to `*.yaml`).
   * Added `apisToGenerate` mojo parameter to `boat:generate`
   * Set the default of `httpUserAgent` to `${project.artifactId}-${project.version}`.

* *Java Generator - resttemplate library*
  * Added `useWithModifiers` option to use the `with` prefix for POJO modifiers (defaults to `false`).
  * added `useSetForUniqueItems` to map arrays containing `uniqueItems` to `Set` (defaults to `false`).
  * Added `useClassLevelBeanValidation` option (defaults to `false`).
  * Added `useJacksonConversion` to use Jackson for parameters conversion instead of `toString` (defaults to `false`).
  * Added `restTemplateBeanName` to qualify the autowired RestTemplate bean.

* *Angular Generator*
  * Added an Angular client generator for version 10 and up. 
    The generator template is inherited from the standard one at [openapi-generator.tech](https://openapi-generator.tech/), with the addition of mock responses and a several fixes, among which:
    * Handling of reserved typescript words
    * Added support for Typescript and Javascript for escaping of strings in generators
    * Escaping of model properties when not using `camelCase`
    * Support for multiple `MediaTypes`
  * To enable mocks generation, set the `withMocks` option to `true`  

## 0.10.0
* *Maven Plugin*
   * `boat:lint` mojo will generate an HTML report based on API Guidelines 
   * `boat:docs` mojo will generate HTML documentation from OpenAPI showing multiple examples and requests as well as Custom Annotations
* General Bug Fixes
* Linting Rule Engine extended with reserved word linting
* **NOTE**: The lint rules are still in development. The documentation is still in the works. 

## 0.9.0
* *Maven Plugin*
  * Added `version` parameter to `bundle` goal.
  * Added `bundleSpecs` parameter to `generate` goal to automatically bundle specs into single file
* Modernised BOAT Terminal
* Improved BOAT:Docs Templates
* Properly dereference examples

## 0.8.0
* Improved styling HTML docs
* preview BOAT:QUAY linting mojo for linting OpenAPI specs.
* Avoid circular references when derefenencing OpenAPI specs

## 0.7.0
* Render multiple requests and examples in boat-docs
* Created HTML templates for boat-docs
* Pretty Print JSON Examples
* Added boat:doc mojo for generating beautiful HTML2 docs

* * Spring Generator*
  * Restored `HttpServletRequest` parameter (regression).
* Added boat:yard to create static website based on a collection of specs

## 0.6.0
* simple fix to check for null value in openApi.getComponents().getSchemas()
* ability to resolve references like #/components/schemas/myObject/items or #/components/schemas/myObject/properties/embeddedObject
* simple fix to avoid npe in StaticHtml2Generation escaping response message.


## 0.5.0

* Add DereferenceComponentsPropertiesTransformer (that does a bit extra)
* Fix recursive referencing in UnAliasTransformer


## 0.4.0
* Added bundle skip
* Changed numbering scheme

## 0.3.0

* *Maven Plugin*
  * Added `bundle.skip` parameter to `bundle` goal (defaults to false).

* *HTML2 Generator*
  * Removes examples
  * Adds title of API to the left navigation
  * Removes unnecessary spaces in the docs
  * Fixes item focus on left navigation
  * Updates Json Schema Ref Parser library
  * Updates Json schema view library
  * Adds support for allOf with Json schema merge all of https://github.com/mokkabonna/json-schema-merge-allof
  * Fixes header x- params being escaped. eg X-Total-Count to XMinusTotalMunisCount
  * Fixes markdown in description not being escaped and breaking javascript.
  * Fixes missing references to extended simple types (set `unAlias` option to true).
  * Fixes missing references because confusion over whether to reference name or classname.
  * Moved the code generation into a separate module to be used by other BOAT components.
  * Cleaning up dependencies
  * Added boat:bundle mojo to bundle fragments into a single spec.
  * boat:bundle unaliases the spec. 

* *Spring Generator*
  * Added `useWithModifiers` to use the `with` prefix for POJO modifiers (defaults to `false`; for compatibility with the old RAML generator must be set to `true`).
  * Fixed x-abstract extension (not generated)
  * Reset the defaults of the options added in 0.2.7 to avoid breaking changes.
    - useLombokAnnotations: false
    - openApiNullable: true
    - useSetForUniqueItems: false

## 0.2.7

* *Spring Generator*
  * added in-container validation, e.g. `List<@Size(max = 36) String>` (see [JSR-380 - Container element constraints](https://beanvalidation.org/2.0/spec/#constraintdeclarationvalidationprocess-containerelementconstraints)).
  * added vendor extensions: `x-abstract`, `x-implements`.
  * added `useLombokAnnotations` option (defaults to `true`)
  * added `openApiNullable` option (taken from 5.0,  breaking change, defaults to `false`, set to `true` if not ready).
  * added `useSetForUniqueItems` to map arrays with `uniqueItems` to `Set` instead of `List` (breaking change, defaults to `true`, set to `false` if not ready).
  * added `additionalDependencies` to be used in `spring-boot/pom.mustache` template.
  * formatted method parameters.

* *Maven Plugin*
  * added `addTestCompileSourceRoot` which adds the output directory to the project as a test source root.
  * added `apiNameSuffix` to customise the name of the API interface.
  * corrected `generatorName` property to point to `openapi.generator.maven.plugin.generatorName`.
  * fixed the code generated for properties of type `Map` in model.
  * refactored `GenerateMojo` so `mvn boat:generate -Dcodegen.configHelp -Dopenapi.generator.maven.plugin.generatorName=spring` works correctly.
  * test the generated code in the integration test phase

## 0.2.6
* Ensure RAML traits that are converted to OAS extensions are all using lower case. 

## 0.2.5 
* Fixed a bug how duplicate names are generated if RAML source has duplicate names for references. The parent resource name is now prepended to the schema name without removing the last character of the parent resource name
* Fixed a bug when in RAML resources were inline references instead of global type references for Request Bodies causing Response Schemas being referenced as Request Bodies

## 0.2.4 - Breaking Change!
* **Changed how operationIds are generated**. The previous implementation ended up generating very long and confusing names. 
    The improved generator greatly improves the names of operationId when converting from RAML to OAS3
* Default version of OpenAPI is now **3.0.3**
* Generated STUBS and Clients must be refactored to use the new names! It should not affect the names of Schemas converted from RAML. 


## 0.2.3
* Use RAML Display Name as Summary on Http Operations when converting to OAS3
* Also include integration-spec and artifacts ending on specs as default for conversion using `export-dep`
* Fix HTML2 Titles

## 0.2.2
* Fixed enum conversion. Empty enums are now set to null again when converting from raml to OpenAPI
* Added more robust code gen mojos


## 0.2.1
* Improved Open API Diff
* Sonar Fixes 

## 0.2.0
* Created new Code Generation Mojos with opinionated settings for
** Java Client with Spring WebClient (Reactive)
** Java Server Stubs for WebFlux (Reactive)
** Java Client with Spring Rest Template (Non Reactive)
** Java Server Stubs for Spring Rest Controller (Non Reactive)
** Improved Java Client API's to better cope with reserved words
* Export Dependencies will now traverse through the artifact to find all raml specs
* Improved RAML 2 Open API conversion
* Upgraded OpenAPI Diff library to more current version
* Mojo's can now break the build by setting `continueOnError` to false

## 0.1.9
* Improved how services are named after base url conversion was introduced.

## 0.1.8
* Reversed normalization of schema names as that causes stack overflow errors. 
* Fixed Base URL Conversion from RAML to OpenAPI
* Specify schema type when adding additional properties in Maven plugin using `additionalPropertiesType` configuration option


## 0.1.7
* Added configurable flag to add HttpServletRequest parameters to codegen'd server stubs.
* Extract inline examples from the obtained OpenAPI spec and put them under '<output-dir>/examples/' as json files.
* Changed the normalization of Schema Names to ensure existing casing is not lost

## 0.1.6
* Added documentation on boat-maven-plugin
* Upgraded YAML Libraries to improve output of YAML files
* Use standardized swagger YAML output
* Added Bean Validator in Code Generator
* Changed Open API Loader to correctly resolve references from reading input location instead of string

## 0.1.5

* Upgraded openapi-generator to 4.3.0
* Fixed java doc in the Java templates to allow usage in Java 11 projects
* Rename variable name `accept` to `acceptMediaType` in Java templates to allow OpenAPI Specs with parameters called `accept`


## 0.1.4

* Fixed template for HTML2 generator
* Include conversion of api.raml files found in dependencies

## 0.1.3  

* Added Code Generator Mojo from on [openapi-generator.tech](https://openapi-generator.tech/) with custom templates for Java, JavaSpring and HTML2
* Renamed `export` to `export-dep` mojo for converting RAML specs to oas from dependencies
* Added `export` mojo for converting RAML specs from input file
* Added Normaliser transformer for transforming examples names to be used in Java code generation  as example names cannot have special characters.
* Improve Title and Descriptions of converted RAML specs
* Always wrap examples in example object
* Many code improvements to be not ashamed of Sonar Reports.  


# Build & Install

```shell script
mvn install
```

## CLI Usage

### Convert RAML to Open API 3.0 
```shell script
cd boat-terminal
java -jar target/boat-terminal-0.0.1-SNAPSHOT-jar-with-dependencies.jar \
  -f src/test/resources/api.raml
```


### Convert RAML to Open API 3.0 && Pipe output to file
```shell script
cd boat-terminal
java -jar target/boat-terminal-0.0.1-SNAPSHOT-jar-with-dependencies.jar \
  -f src/test/resources/api.raml \
  > openapi.yaml
```


### Convert RAML to Open API 3.0 file and verbose logging
```shell script
cd boat-terminal
java -jar target/boat-terminal-0.0.1-SNAPSHOT-jar-with-dependencies.jar \
  -f src/test/resources/api.raml \
  -o swagger.yaml \
  -v
```

### Convert RAML to Open API 3.0 with examples exploded into <output-dir>/examples/
```shell script
cd boat-terminal
java -jar target/boat-terminal-0.0.1-SNAPSHOT-jar-with-dependencies.jar \
  -f src/test/resources/api.raml \
  -o swagger.yaml \
  -d my-open-api-spec/ \
  -v
```


## Maven Plugin Usage

Configuration

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    
    <modelVersion>4.0.0</modelVersion>

    <groupId>my.project</groupId>
    <artifactId>my-specs-definition</artifactId>
    <version>1.0</version>
    <packaging>pom</packaging>

    <properties>
      <boat-maven-plugin.version>0.1.4</boat-maven-plugin.version>
    </properties>

    <build>
      <plugins>
        <plugin>
          <groupId>com.backbase.oss</groupId>
          <artifactId>boat-maven-plugin</artifactId>
          <version>${boat-maven-plugin.version}</version>
            <executions>
              <execution>
                <id>export-raml-spec</id>
                <phase>generate-sources</phase>
                <goals>
                  <goal>export</goal>
                </goals>
                <configuration>
                  <inputFile>${basedir}/src/main/resources/client-api.raml</inputFile>
                </configuration>
              </execution>
            </executions>
        </plugin>
     </plugins>
    </build>
</project>
```

The following command will convert the given `client-api.raml` file into Open API 3.0 format.
 
```bash
mvn boat:export
```

**NOTE:** RAML file name should end with `-api.raml`, `service-api.raml` or `client-api.raml`. 

## Export All Specifications in Bill-Of-Materials pom file
If you want to export all specifications referenced in a pom file, you can use the following mojo

```xml
    <build>
        <plugins>
            <plugin>
                <groupId>com.backbase.boat</groupId>
                <artifactId>boat-maven-plugin</artifactId>
                <version>${boat-maven-plugin.version}</version>
                <configuration>
                    <specBom>
                        <groupId>com.backbase.dbs</groupId>-->
                        <artifactId>banking-services-bom</artifactId>
                        <version>[2.16.0,)</version>
                        <type>pom</type>
                        <!-- Bom equal or higher than 2.16 -->
                    </specBom>
                    <output>${project.basedir}/raml-2-openapi-specs</output>
                    <xLogoUrl>http://www.backbase.com/wp-content/uploads/2017/04/backbase-logo-png.png</xLogoUrl>
                    <xLogoAltText>Backbase</xLogoAltText>
                    <markdownBottom># Disclaimer
This API is converted from RAML1.0 using the boat-maven-plugin and is not final or validated!
                    </markdownBottom>
                    <addChangeLog>true</addChangeLog>
                </configuration>
            </plugin>
        </plugins>
    </build>

```

### Configuration Options

* The `addChangeLog` option will automagically insert a change log between all referenced versions 
* The `includeVersionsRegEx` can be used to filter out certain versions. By default it's set to `^(\d+\.)?(\d+\.)?(\d+)$` to only allow x.x.x versions. To also include patch versions, set it to `^(\d+\.)?(\d+\.)?(\d+\.)?(\*|\d+)$`

```bash
mvn boat:export-bom
```

## Generate API docs

Configuration

```xml
<!-- ... -->

<build>
  <plugins>
    <plugin>
      <groupId>com.backbase.oss</groupId>
      <artifactId>boat-maven-plugin</artifactId>
      <version>${boat-maven-plugin.version}</version>
        <executions>
          <execution>
            <id>generate-docs</id>
            <phase>generate-sources</phase>
            <goals>
              <goal>generate</goal>
            </goals>
            <configuration>
              <inputSpec>${project.basedir}/src/main/resources/api.yaml</inputSpec>
              <output>${project.build.directory}/generated-sources</output>
              <generatorName>html2</generatorName>
            </configuration>
          </execution>
        </executions>
    </plugin>
 </plugins>
</build>

<!-- ... -->
```

The following command will generate `index.html` file in the specified output folder that contains API endpoints description.  
 
```bash
mvn boat:generate@generate-docs
```

## Generate API interfaces

Configuration
```
<build>
  <plugins>
    <plugin>
      <groupId>com.backbase.oss</groupId>
      <artifactId>boat-maven-plugin</artifactId>
      <version>${boat-maven-plugin.version}</version>
      <executions>
        <execution>
          <id>generate-api-code</id>
          <goals>
            <goal>generate</goal>
          </goals>
          <phase>generate-sources</phase>
          <configuration>
            <inputSpec>${project.basedir}/src/main/resources/api.yaml</inputSpec>
            <output>${project.build.directory}/generated-sources/api</output>
            <generatorName>spring</generatorName>
            [...]
            <configOptions>
              <library>spring-boot</library>
              <apiPackage>com.example.my.service.api.interfaces</apiPackage>
              <modelPackage>com.example.my.service.models</modelPackage>
              <hideGenerationTimestamp>true</hideGenerationTimestamp>
              <dateLibrary>java8</dateLibrary>
              <interfaceOnly>true</interfaceOnly>
              <skipDefaultInterface>true</skipDefaultInterface>
              <useBeanValidation>true</useBeanValidation>
              <useTags>true</useTags>
              <java8>true</java8>
              <useOptional>false</useOptional>
              [...]
            </configOptions>
          </configuration>
        </execution>
      </executions>
    </plugin>
 </plugins>
</build>
```

A comprehensive list of the Configuration options can be found below.

| Option | Property | Description |
|--------|----------|-------------|
| `verbose` |  `openapi.generator.maven.plugin.verbose` | verbose mode (`false` by default)
| `inputSpec` |  `openapi.generator.maven.plugin.inputSpec` | OpenAPI Spec file path
| `language` |  `openapi.generator.maven.plugin.language` | target generation language (deprecated, replaced by `generatorName` as values here don't represent only 'language' any longer)
| `generatorName` |  `openapi.generator.maven.plugin.generatorName` | target generator name
| `output` |  `openapi.generator.maven.plugin.output` | target output path (default is `${project.build.directory}/generated-sources/openapi`. Can also be set globally through the `openapi.generator.maven.plugin.output` property)
| `gitHost` | `openapi.generator.maven.plugin.gitHost` | The git host, e.g. gitlab.com
| `gitUserId` |  `openapi.generator.maven.plugin.gitUserId` | sets git information of the project
| `gitRepoId` | `openapi.generator.maven.plugin.gitRepoId` | sets the repo ID (e.g. openapi-generator)
| `templateDirectory` |  `openapi.generator.maven.plugin.templateDirectory` | directory with mustache templates
| `templateResourcePath` |  `openapi.generator.maven.plugin.templateResourcePath` | directory with mustache templates via resource path. This option will overwrite any option defined in `templateDirectory`.
| `engine` | `openapi.generator.maven.plugin.engine` | The name of templating engine to use, "mustache" (default) or "handlebars" (beta)
| `auth` |  `openapi.generator.maven.plugin.auth` | adds authorization headers when fetching the OpenAPI definitions remotely. Pass in a URL-encoded string of `name:header` with a comma separating multiple values
| `configurationFile` |  `openapi.generator.maven.plugin.configurationFile` | Path to separate json configuration file. File content should be in a json format {"optionKey":"optionValue", "optionKey1":"optionValue1"...} Supported options can be different for each language. Run `config-help -g {generator name}` command for language specific config options
| `skipOverwrite` |  `openapi.generator.maven.plugin.skipOverwrite` | Specifies if the existing files should be overwritten during the generation. (`false` by default)
| `apiPackage` |  `openapi.generator.maven.plugin.apiPackage` | the package to use for generated api objects/classes
| `modelPackage` |  `openapi.generator.maven.plugin.modelPackage` | the package to use for generated model objects/classes
| `invokerPackage` |  `openapi.generator.maven.plugin.invokerPackage` | the package to use for the generated invoker objects
| `packageName` | `openapi.generator.maven.plugin.packageName` | the default package name to use for the generated objects
| `groupId` | `openapi.generator.maven.plugin.groupId`  | sets project information in generated pom.xml/build.gradle or other build script. Language-specific conversions occur in non-jvm generators
| `artifactId` |  `openapi.generator.maven.plugin.artifactId` | sets project information in generated pom.xml/build.gradle or other build script. Language-specific conversions occur in non-jvm generators
| `artifactVersion` |  `openapi.generator.maven.plugin.artifactVersion` | sets project information in generated pom.xml/build.gradle or other build script. Language-specific conversions occur in non-jvm generators
| `library` |  `openapi.generator.maven.plugin.library` | library template (sub-template)
| `modelNamePrefix` |  `openapi.generator.maven.plugin.modelNamePrefix` | Sets the prefix for model classes and enums
| `modelNameSuffix` |  `openapi.generator.maven.plugin.modelNameSuffix` | Sets the suffix for model classes and enums
| `ignoreFileOverride` |  `openapi.generator.maven.plugin.ignoreFileOverride` | specifies the full path to a `.openapi-generator-ignore` used for pattern based overrides of generated outputs
| `httpUserAgent` | `openapi.generator.maven.plugin.httpUserAgent` | Sets custom User-Agent header value
| `removeOperationIdPrefix` |  `openapi.generator.maven.plugin.removeOperationIdPrefix` | remove operationId prefix (e.g. user_getName => getName)
| `logToStderr` |  `openapi.generator.maven.plugin.logToStderr` | write all log messages (not just errors) to STDOUT
| `enablePostProcessFile` |  `openapi.generator.maven.plugin.` | enable file post-processing hook
| `skipValidateSpec` |  `openapi.generator.maven.plugin.skipValidateSpec` | Whether or not to skip validating the input spec prior to generation. By default, invalid specifications will result in an error.
| `strictSpec` |  `openapi.generator.maven.plugin.strictSpec` | Whether or not to treat an input document strictly against the spec. 'MUST' and 'SHALL' wording in OpenAPI spec is strictly adhered to. e.g. when false, no fixes will be applied to documents which pass validation but don't follow the spec.
| `generateAliasAsModel` |  `openapi.generator.maven.plugin.generateAliasAsModel` | generate alias (array, map) as model
| `configOptions` |  N/A | a **map** of language-specific parameters. To show a full list of generator-specified parameters (options), please use `configHelp` (explained below)
| `instantiationTypes` |  `openapi.generator.maven.plugin.instantiationTypes` | sets instantiation type mappings in the format of type=instantiatedType,type=instantiatedType. For example (in Java): `array=ArrayList,map=HashMap`. In other words array types will get instantiated as ArrayList in generated code. You can also have multiple occurrences of this option
| `importMappings` |  `openapi.generator.maven.plugin.importMappings` | specifies mappings between a given class and the import that should be used for that class in the format of type=import,type=import. You can also have multiple occurrences of this option
| `typeMappings` |  `openapi.generator.maven.plugin.typeMappings` | sets mappings between OpenAPI spec types and generated code types in the format of OpenAPIType=generatedType,OpenAPIType=generatedType. For example: `array=List,map=Map,string=String`. You can also have multiple occurrences of this option
| `languageSpecificPrimitives` |  `openapi.generator.maven.plugin.languageSpecificPrimitives` | specifies additional language specific primitive types in the format of type1,type2,type3,type3. For example: `String,boolean,Boolean,Double`. You can also have multiple occurrences of this option
| `additionalProperties` |  `openapi.generator.maven.plugin.additionalProperties` | sets additional properties that can be referenced by the mustache templates in the format of name=value,name=value. You can also have multiple occurrences of this option
| `serverVariableOverrides` | `openapi.generator.maven.plugin.serverVariableOverrides` | A map of server variable overrides for specs that support server URL templating
| `reservedWordsMappings` |  `openapi.generator.maven.plugin.reservedWordsMappings` | specifies how a reserved name should be escaped to. Otherwise, the default `_<name>` is used. For example `id=identifier`. You can also have multiple occurrences of this option
| `generateApis` |  `openapi.generator.maven.plugin.generateApis` | generate the apis (`true` by default). Specific apis may be defined as a CSV via `apisToGenerate`.
| `apisToGenerate` |  `openapi.generator.maven.plugin.apisToGenerate` | A comma separated list of apis to generate.  All apis is the default.
| `generateModels` |  `openapi.generator.maven.plugin.generateModels` | generate the models (`true` by default). Specific models may be defined as a CSV via `modelsToGenerate`.
| `modelsToGenerate` |  `openapi.generator.maven.plugin.modelsToGenerate` | A comma separated list of models to generate.  All models is the default.
| `generateSupportingFiles` |  `openapi.generator.maven.plugin.generateSupportingFiles` | generate the supporting files (`true` by default)
| `supportingFilesToGenerate` |  `openapi.generator.maven.plugin.supportingFilesToGenerate` | A comma separated list of supporting files to generate.  All files is the default.
| `generateModelTests` |  `openapi.generator.maven.plugin.generateModelTests` | generate the model tests (`true` by default. Only available if `generateModels` is `true`)
| `generateModelDocumentation` |  `openapi.generator.maven.plugin.generateModelDocumentation` | generate the model documentation (`true` by default. Only available if `generateModels` is `true`)
| `generateApiTests` |  `openapi.generator.maven.plugin.generateApiTests` | generate the api tests (`true` by default. Only available if `generateApis` is `true`)
| `generateApiDocumentation` |  `openapi.generator.maven.plugin.generateApiDocumentation` | generate the api documentation (`true` by default. Only available if `generateApis` is `true`)
| `withXml` |  `openapi.generator.maven.plugin.withXml` | enable XML annotations inside the generated models and API (only works with Java `language` and libraries that provide support for JSON and XML)
| `skip` |  `codegen.skip` | skip code generation (`false` by default. Can also be set globally through the `codegen.skip` property)
| `skipIfSpecIsUnchanged` |  `codegen.skipIfSpecIsUnchanged` | Skip the execution if the source file is older than the output folder (`false` by default. Can also be set globally through the `codegen.skipIfSpecIsUnchanged` property)
| `addCompileSourceRoot` |  `openapi.generator.maven.plugin.addCompileSourceRoot` | Add the output directory to the project as a source root, so that the generated java types are compiled and included in the project artifact (`true` by default). Mutually exclusive with `addTestCompileSourceRoot`.
| `addTestCompileSourceRoot` |  `openapi.generator.maven.plugin.addTestCompileSourceRoot` | Add the output directory to the project as a test source root, so that the generated java types are compiled only for the test classpath of the project (`false` by default). Mutually exclusive with `addCompileSourceRoot`.
| `environmentVariables` | N/A | A **map** of items conceptually similar to "environment variables" or "system properties". These are merged into a map of global settings available to all aspects of the generation flow. Use this map for any options documented elsewhere as `systemProperties`.
| `configHelp` |  `codegen.configHelp` | dumps the configuration help for the specified library (generates no sources)

For the `spring` generator, the additional configuration options are:

| Option | Description |
|--------|-------------|
| `sortParamsByRequiredFlag` | Sort method arguments to place required parameters before optional parameters. (Default: true) |
| `sortModelPropertiesByRequiredFlag` | Sort model properties to place required parameters before optional parameters. (Default: true) |
| `ensureUniqueParams` | Whether to ensure parameter names are unique in an operation (rename parameters that are not). (Default: true) |
| `allowUnicodeIdentifiers` | boolean, toggles whether unicode identifiers are allowed in names or not, default is false (Default: false) |
| `prependFormOrBodyParameters` | Add form or body parameters to the beginning of the parameter list. (Default: false) |
| `modelPackage` | package for generated models (Default: org.openapitools.model) |
| `apiPackage` | package for generated api classes (Default: org.openapitools.api) |
| `invokerPackage` | root package for generated code (Default: org.openapitools.api) |
| `groupId` | groupId in generated pom.xml (Default: org.openapitools) |
| `artifactId` | artifactId in generated pom.xml. This also becomes part of the generated library's filename (Default: openapi-spring) |
| `artifactVersion` | artifact version in generated pom.xml. This also becomes part of the generated library's filename (Default: 1.0.0) |
| `artifactUrl` | artifact URL in generated pom.xml (Default: https://github.com/openapitools/openapi-generator) |
| `artifactDescription` | artifact description in generated pom.xml (Default: OpenAPI Java) |
| `scmConnection` | SCM connection in generated pom.xml (Default: scm:git:git@github.com:openapitools/openapi-generator.git) |
| `scmDeveloperConnection` | SCM developer connection in generated pom.xml (Default: scm:git:git@github.com:openapitools/openapi-generator.git) |
| `scmUrl` | SCM URL in generated pom.xml (Default: https://github.com/openapitools/openapi-generator) |
| `developerName` | developer name in generated pom.xml (Default: OpenAPI-Generator Contributors) |
| `developerEmail` | developer email in generated pom.xml (Default: team@openapitools.org) |
| `developerOrganization` | developer organization in generated pom.xml (Default: OpenAPITools.org) |
| `developerOrganizationUrl` | developer organization URL in generated pom.xml (Default: http://openapitools.org) |
| `licenseName` | The name of the license (Default: Unlicense) |
| `licenseUrl` | The URL of the license (Default: http://unlicense.org) |
| `sourceFolder` | source folder for generated code (Default: src/main/java) |
| `serializableModel` | boolean - toggle "implements Serializable" for generated models (Default: false) |
| `bigDecimalAsString` | Treat BigDecimal values as Strings to avoid precision loss. (Default: false) |
| `fullJavaUtil` | whether to use fully qualified name for classes under java.util. This option only works for Java API client (Default: false) |
| `hideGenerationTimestamp` | Hides the generation timestamp when files are generated. (Default: false) |
| `withXml` | whether to include support for application/xml content type and include XML annotations in the model (works with libraries that provide support for JSON and XML) (Default: false) |
| `dateLibrary` | Option. Date library to use (Default: threetenbp)<br>joda - Joda (for legacy app only)<br>legacy - Legacy java.util.Date (if you really have a good reason not to use threetenbp<br>java8-localdatetime - Java 8 using LocalDateTime (for legacy app only)<br>java8 - Java 8 native JSR310 (preferred for jdk 1.8+) - note: this also sets "java8" to true<br>threetenbp - Backport of JSR310 (preferred for jdk < 1.8) |
| `java8` | Option. Use Java8 classes instead of third party equivalents (Default: false)<br>true - Use Java 8 classes such as Base64. Use java8 default interface when a responseWrapper is used<br>false - Various third party libraries as needed |
| `disableHtmlEscaping` | Disable HTML escaping of JSON strings when using gson (needed to avoid problems with byte[] fields) (Default: false) |
| `booleanGetterPrefix` | Set booleanGetterPrefix (Default: get) |
| `additionalModelTypeAnnotations` | Additional annotations for model type(class level annotations) |
| `parentGroupId` | parent groupId in generated pom N.B. parentGroupId, parentArtifactId and parentVersion must all be specified for any of them to take effect |
| `parentArtifactId` | parent artifactId in generated pom N.B. parentGroupId, parentArtifactId and parentVersion must all be specified for any of them to take effect |
| `parentVersion` | parent version in generated pom N.B. parentGroupId, parentArtifactId and parentVersion must all be specified for any of them to take effect |
| `snapshotVersion` | Uses a SNAPSHOT version. true - Use a SnapShot Versionfalse - Use a Release Version |
| `title` | server title name or client service name (Default: OpenAPI Spring) |
| `configPackage` | configuration package for generated code (Default: org.openapitools.configuration) |
| `basePackage` | base package (invokerPackage) for generated code (Default: org.openapitools) |
| `interfaceOnly` | Whether to generate only API interface stubs without the server files. (Default: false) |
| `delegatePattern` | Whether to generate the server files using the delegate pattern (Default: false) |
| `singleContentTypes` | Whether to select only one produces/consumes content-type by operation. (Default: false) |
| `skipDefaultInterface` | Whether to generate default implementations for java8 interfaces (Default: false) |
| `async` | use async Callable controllers (Default: false) |
| `reactive` | wrap responses in Mono/Flux Reactor types (spring-boot only) (Default: false) |
| `responseWrapper` | wrap the responses in given type (Future, Callable, CompletableFuture,ListenableFuture, DeferredResult, HystrixCommand, RxObservable, RxSingle or fully qualified type) |
| `virtualService` | Generates the virtual service. For more details refer - https://github.com/elan-venture/virtualan/wiki (Default: false) |
| `useTags` | use tags for creating interface and controller classnames (Default: false) |
| `useBeanValidation` | Use BeanValidation API annotations (Default: true) |
| `performBeanValidation` | Use Bean Validation Impl. to perform BeanValidation (Default: false) |
| `useClassLevelBeanValidation` | Adds @Validated annotation to API interfaces (Default: false) |
| `useLombokAnnotations` | Use Lombok annotations to generate properties accessors and `hashCode`/`equals` methods (Default: false) |
| `addServletRequest` | Adds ServletRequest objects to API method definitions (Default: false) |
| `addBindingResult` | Adds BindingResult to Api method definitions' request bodies if UseBeanValidation true, for this to be effective you must configure UseBeanValidation, this is not done automatically (Default: false)|
| `implicitHeaders` | Skip header parameters in the generated API methods using @ApiImplicitParams annotation. (Default: false) |
| `swaggerDocketConfig` | Generate Spring OpenAPI Docket configuration class. (Default: false) |
| `apiFirst` | Generate the API from the OAI spec at server compile time (API first approach) (Default: false) |
| `useOptional` | Use Optional container for optional parameters (Default: false) |
| `hateoas` | Use Spring HATEOAS library to allow adding HATEOAS links (Default: false) |
| `returnSuccessCode` | Generated server returns 2xx code (Default: false) |
| `unhandledException` | Declare operation methods to throw a generic exception and allow unhandled exceptions (useful for Spring `@ControllerAdvice` directives). (Default: false) |
| `library` | library template (sub-template) (Default: spring-boot)<br>spring-boot - Spring-boot Server application using the SpringFox integration.<br>spring-mvc - Spring-MVC Server application using the SpringFox integration.<br>spring-cloud - Spring-Cloud-Feign client with Spring-Boot auto-configured settings.|
