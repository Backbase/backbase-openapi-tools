# boat maven plugin.

The boat plugin has 9 goals:

- bundle

    Bundles all references in the OpenAPI specification into one
    file.

- decompose

    Merges any components using allOf references.

- diff

    Calculates a Change log for APIs.

- export

    Converts a RAML spec to an OpenAPI spec.

- export-bom

    Converts all RAML spec dependencies to OpenAPI Specs.

- export-dep

    Exports project dependencies where the ArtifactId ends with
    '-spec'.

- generate

    Generates client/server code from an OpenAPI json/yaml
    definition.

- remove-deprecated

    Removes deprecated elements in an OpenAPI spec.

- boat:validate

    Validates OpenAPI specs.

For more information, run 

`mvn help:describe -Dplugin=com.backbase.oss:boat-maven-plugin -Ddetail`

## boat:generate

Finds files name `api.raml`, `client-api.raml` or `service-api.raml`.
Processes these files (and the json schemes they refer to) to produce `open-api.yaml` files in the output directory. 

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
```mvn boat:generate```

Or hook up to your build process by adding ```executions``` configuration.
