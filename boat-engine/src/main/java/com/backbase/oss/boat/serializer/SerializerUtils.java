package com.backbase.oss.boat.serializer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import io.swagger.v3.core.util.Yaml;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.media.Schema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SerializerUtils {
    private static final Logger LOGGER = LoggerFactory.getLogger(SerializerUtils.class);

    public static String toYamlString(OpenAPI openAPI) {
        if (openAPI == null) {
            return null;
        }
        SimpleModule module = new SimpleModule("OpenAPIModule");
        module.addSerializer(OpenAPI.class, new OpenAPISerializer());
        try {
            ObjectMapper mapper = Yaml.mapper()
                .registerModule(module)
                .configure(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY, false);

            YAMLFactory factory = (YAMLFactory) mapper.getFactory();

//            factory.disable(YAMLGenerator.Feature.SPLIT_LINES);
//            factory.enable(YAMLGenerator.Feature.LITERAL_BLOCK_STYLE);

            return mapper.writeValueAsString(openAPI);

//            return Yaml.pretty(openAPI);
//                    .replace("\r\n", "\n");
        } catch (Exception e) {
            System.err.println("Can not create yaml content");
            e.printStackTrace();
        }
        return null;
    }

    public static String toYamlString(Schema schema) {
        SimpleModule module = new SimpleModule("OpenAPISchemaModule");
        module.addSerializer(Schema.class, new SchemaSerializer());
        try {
            return Yaml.mapper()
                .registerModule(module)
                .configure(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY, false)
                .writeValueAsString(schema)
                .replace("\r\n", "\n");
        } catch (JsonProcessingException e) {
            System.err.println("Can not create yaml content");
            e.printStackTrace();
        }
        return null;
    }
}
