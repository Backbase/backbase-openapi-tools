![Java CI](https://github.com/Backbase/backbase-openapi-tools/workflows/Java%20CI/badge.svg)
![Release](https://github.com/Backbase/backbase-openapi-tools/workflows/Release/badge.svg)
[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=com.backbase.oss%3Abackbase-openapi-tools&metric=alert_status)](https://sonarcloud.io/dashboard?id=com.backbase.oss%3Abackbase-openapi-tools)

# Backbase OpenApi Tools 

The Backbase Open API Tools is a collection of tools created to work efficiently with OpenAPI

It currently consists of

* RAML 1.0 Converter to OpenAPI 3.0 
* Create Diff Report between 2 OpenAPI versions of the same spec (Based on https://github.com/quen2404/openapi-diff)
* Decompose Transformer to remove Composed Schemas from OpenAPI specs to aid in code generators
* Case Transformer to see how your API looks like when going from camelCase to snake_case  (transforms examples too)

The project is very much Work In Progress and will be published on maven central when considered ready enough. 

# Release Notes
BOAT is still under development and subject to change. 

## 0.1.4

* Fixed template for HTML2 generator

## 0.1.3 – Halve Maen

* Added Code Generator Mojo from on [openapi-generator.tech](https://openapi-generator.tech/) with custom templates for Java, JavaSpring and HTML2
* Renamed `export` to `export-dep` mojo for converting RAML specs to oas from dependencies
* Added `export` mojo for converting RAML specs from input file
* Added Normaliser transformer for transforming examples names to be used in Java code generation  as example names cannot have special characters.
* Improve Title and Descriptions of converted RAML specs
* Always wrap examples in example object
* Many code improvements to be not ashamed of Sonar Reports.  


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