# boat maven plugin.

The `boat` plugin has multiple goals:

- `bundle` (Not fully implemented)

    Bundles all references in the OpenAPI specification into one
    file.

- `decompose`

    Merges any components using allOf references.

- `diff`

    Calculates a Change log for APIs.

- `export`

    Converts a RAML spec to an OpenAPI spec.

- `export-bom`

    Converts all RAML spec dependencies to OpenAPI Specs.

- `export-dep`

    Exports project dependencies where the ArtifactId ends with
    '-spec'.

- `generate`

    Generates client/server code from a OpenAPI json/yaml
    definition. Finds files name `api.raml`, `client-api.raml` or `service-api.raml`. Processes these files (and the 
    json schemes they refer to) to produce `open-api.yaml` files in the output directory.
   
- `generate-spring-boot-embedded`, `generate` but with opinionated defaults

              <configuration>
                <output>${project.build.directory}/generated-sources/openapi</output>
                <generateSupportingFiles>true</generateSupportingFiles>
                <generatorName>spring</generatorName>
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
                </configOptions>

- `generate-rest-template-embedded`, `generate` but with opinionated defaults

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

- `generate-webclient-embedded`, `generate` but with opinionated defaults

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

- `remove-deprecated`

    Removes deprecated elements in an OpenAPI spec.

- `boat:validate`

    Validates OpenAPI specs.

For more information, run 

`mvn help:describe -Dplugin=com.backbase.oss:boat-maven-plugin -Ddetail`

## Configuration examples

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
```mvn boat:generate```

Or hook up to your build process by adding ```executions``` configuration.
