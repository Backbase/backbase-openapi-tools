![Java CI](https://github.com/Backbase/backbase-openapi-tools/workflows/Java%20CI/badge.svg)
![Release](https://github.com/Backbase/backbase-openapi-tools/workflows/Release/badge.svg)

# Backbase OpenApi Tools 

The Backbase Open API Tools is a collection of tools created to work efficiently with OpenAPI

It currently consists of

* RAML 1.0 Converter to OpenAPI 3.0 
* Create Diff Report between 2 OpenAPI versions of the same spec (Based on https://github.com/quen2404/openapi-diff)
* Decompose Transformer to remove Composed Schemas from OpenAPI specs to aid in code generators
* Case Transformer to see how your API looks like when going from camelCase to snake_case  (transforms examples too)

The project is very much Work In Progress and will be published on maven central when considered ready enough. 

# Build & Install

```bash
mvn install
```

## CLI Usage

### Convert RAML to Open API 3.0 
```bash
cd boat-terminal
java -jar target/boat-terminal-0.0.1-SNAPSHOT-jar-with-dependencies.jar \
  -f src/test/resources/api.raml
```

### Convert RAML to Open API 3.0 && Pipe output to file
```bash
cd boat-terminal
java -jar target/boat-terminal-0.0.1-SNAPSHOT-jar-with-dependencies.jar \
  -f src/test/resources/api.raml \
  > openapi.yaml
```


### Convert RAML to Open API 3.0 file and verbose logging
```bash
cd boat-terminal
java -jar target/boat-terminal-0.0.1-SNAPSHOT-jar-with-dependencies.jar \
  -f src/test/resources/api.raml \
  -o swagger.yaml \
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

    <build>
        <plugins>
            <plugin>
                <groupId>com.backbase.boat</groupId>
                <artifactId>boat-maven-plugin</artifactId>
                <version>${boat-maven-plugin.version}</version>
                <configuration>
                    <inputFiles>
                        <inputFile>${basedir}/src/main/resources/api.raml</inputFile>
                    </inputFiles>
                </configuration>
            </plugin>
        </plugins>
    </build>
</project>
```

Execution
```bash
mvn boat:raml2openapi
```


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
This API is converted from RAML1.0 using the raml2openapi-maven-plugin and is not final or validated!

Please provide feedback in the `#s-raml2openapi` Slack channel.
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
mvn raml2openapi:export-bom
```
