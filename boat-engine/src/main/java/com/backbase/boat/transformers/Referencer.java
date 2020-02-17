package com.backbase.boat.transformers;

import com.backbase.boat.ExportException;
import com.backbase.boat.Exporter;
import com.backbase.boat.serializer.SerializerUtils;
import com.google.common.base.CaseFormat;
import io.swagger.parser.OpenAPIParser;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.media.ArraySchema;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.parser.core.models.ParseOptions;
import io.swagger.v3.parser.core.models.SwaggerParseResult;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Referencer {

    private static final Logger log = LoggerFactory.getLogger(Exporter.class);

    public static void transform(URL url) throws ExportException, IOException {
        File openApiFile = new File(url.getFile());
        if (!openApiFile.exists()) {
            throw new ExportException("OpenAPI file does not exist: " + openApiFile);
        }

        File outputDirectory = new File(openApiFile.getParentFile(), "referenced");
        File schemasOutputDirectory = new File(outputDirectory, "components/schemas");
        schemasOutputDirectory.mkdirs();

        OpenAPIParser openAPIParser = new OpenAPIParser();
        ParseOptions parseOptions = new ParseOptions();
        parseOptions.setFlatten(true);
        SwaggerParseResult swaggerParseResult = openAPIParser.readLocation(url.toString(), new ArrayList<>(), parseOptions);
        OpenAPI openAPI = swaggerParseResult.getOpenAPI();

        Map<String, Schema> newSchemas = new LinkedHashMap<>();

        for (Map.Entry<String, Schema> entry : openAPI.getComponents().getSchemas().entrySet()) {
            String name = entry.getKey();
            Schema schema = entry.getValue();
            if (schema.get$ref() == null) {
                replacePropertyReferences(schema);
                String schemaAsYaml = SerializerUtils.toYamlString(schema);
                String fileName = CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_HYPHEN, name);
                File schemaFile = new File(schemasOutputDirectory, fileName + ".yaml");
                Files.write(schemaFile.toPath(), schemaAsYaml.getBytes());
                Path relativize = outputDirectory.toPath().relativize(schemaFile.toPath());
                Schema newSchema = new Schema();
                newSchema.setName(name);
                newSchema.$ref(relativize.toString());
                newSchemas.put(name, newSchema);
            }
        }


        openAPI.getComponents().setSchemas(newSchemas);
        String referencedSchema = SerializerUtils.toYamlString(openAPI);
        Files.write(new File(outputDirectory, "openapi.yaml").toPath(), referencedSchema.getBytes());
    }

    private static void replacePropertyReferences(Schema schema) {
        Map<String, Schema> properties = schema.getProperties();
        replaceRef(schema);
        if (schema.getProperties() != null) {
            properties.values().forEach(property -> {
                if (property.get$ref() != null) {
                    replaceRef(property);
                }
                replacePropertyReferences(property);
            });
        }
        if (schema instanceof ArraySchema) {
            replaceRef(((ArraySchema) schema).getItems());
        }

    }

    private static void replaceRef(Schema property) {
        String ref = property.get$ref();
        if (ref != null && ref.startsWith("#/components/schemas/")) {
            String componentName = StringUtils.substringAfterLast(ref, "/");
            String to = CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_HYPHEN, componentName);
            property.set$ref("./" + to + ".yaml");
        }
    }

}
