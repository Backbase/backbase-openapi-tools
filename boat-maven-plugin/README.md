#boat maven plugin.

Three mojos:
- Generate
- Export
- compare

## boat:generate

Finds files name `api.raml`, `client-api.raml` or `service-api.raml`.
Processes these files (and the json schemes they refer to) to produce `open-api.yaml` files in the output directory. 

Configuration example
```$xml
   <build>
       <plugins>
            <plugin>
                <groupId>com.backbase.codegen</groupId>
                <artifactId>boat-maven-plugin</artifactId>
                <version>0.0.31-SNAPSHOT</version>
                <configuration>
                    <!-- Showing defaults - do not configure defaults! -->
                    <!-- The input directory -->
                    <input>${project.basedir}/src/main/resources</input>
                    <!-- The output directory -->
                    <output>${project.basedir}/target/openapi</output>
                    <!-- Whether to fail the build on errors -->
                    <failOnError>true</failOnError>
                </configuration>
            </plugin>
        </plugins>
    </build>
```

Usage
```mvn boat:generate```

Or hook up to your build process by adding ```executions``` configuration.