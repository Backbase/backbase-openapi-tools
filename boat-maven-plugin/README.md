# boat maven plugin.

The `boat` plugin has multiple goals:

## boat:generate

Open API Generator based on https://github.com/OpenAPITools/openapi-generator/tree/master/modules/openapi-generator-maven-plugin. All configuration options as 
defined on openapi-generator-maven-plugin can be applied here too. 

Boat maven plugin uses slightly modified templates for html, java and webclient that help generate specs and clients that work best in a Backbase projects.

All inputSpec parameters for this goal may additionally be configured as an artifact. See [Example inputMavenArtifact parameter](#example-inputMavenArtifact-parameter) or [integration tests](https://github.com/Backbase/backbase-openapi-tools/tree/main/boat-maven-plugin/src/it/example/boat-artifact-input) for examples.

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
            <useClassLevelBeanValidation>false</useClassLevelBeanValidation>
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
            <useClassLevelBeanValidation>false</useClassLevelBeanValidation>
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
            <additionalProperty>createApiComponent=false</additionalProperty>
        </additionalProperties>
    </configuration>

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
            <useClassLevelBeanValidation>false</useClassLevelBeanValidation>
            <useTags>true</useTags>
            <java8>true</java8>
            <useOptional>false</useOptional>
            <apiPackage>com.backbase.goldensample.product.api.client.v2</apiPackage>
            <modelPackage>com.backbase.goldensample.product.api.client.v2.model</modelPackage>
        </configOptions>
    </configuration>

## boat:decompose

Merges any components using allOf references.

## boat:diff

Calculates a Change log for APIs.

## boat:remove-deprecated

Removes deprecated elements in an OpenAPI spec.

## boat:validate

Validates OpenAPI specs.

Configuration can point to a specific file, or a directory. When a directory is specified all files with a `.yaml` 
extension are validated. `failOnWarning` specifies whether to fail the build when validation violations are found,
otherwise, warnings are written to the log for everyone to ignore.

    <configuration>
        <input>${project.build.outputDirectory}/specs/</input>
        <failOnWarning>true</failOnWarning>
    </configuration>

## boat:lint

API lint which provides checks for compliance with many of Backbase's API standards

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

Examples in `json` files are parsed to objects.

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
