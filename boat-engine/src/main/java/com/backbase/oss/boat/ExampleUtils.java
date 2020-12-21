package com.backbase.oss.boat;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.stream.Collectors;
import org.raml.v2.api.model.v10.datamodel.ExampleSpec;
import org.raml.v2.api.model.v10.datamodel.TypeInstance;
import org.raml.v2.api.model.v10.datamodel.TypeInstanceProperty;

@SuppressWarnings("java:S5411")
class ExampleUtils {


    private static final ObjectMapper mapper = new ObjectMapper();

    private ExampleUtils() {
        throw new AssertionError("Private constructor");
    }


    public static Object getExampleObject(ExampleSpec ramlExample, boolean convertJsonExamplesToYaml) {
        if (ramlExample == null) {
            return null;
        }
        Object value;
        TypeInstance structuredValue = ramlExample.structuredValue();
        for (TypeInstanceProperty property : structuredValue.properties()) {
            if (property.isArray()) {
                return property.values().stream().map(TypeInstance::value).collect(Collectors.toList());
            } else {
                return property.value().value();
            }
        }
        if (ramlExample.structuredValue() != null) {
            value = ramlExample.structuredValue().value() != null ? prettyPrint(ramlExample.structuredValue().value().toString(), convertJsonExamplesToYaml) : null;
        } else {
            value = ramlExample.value() != null ? prettyPrint(ramlExample.value(), convertJsonExamplesToYaml) : null;
        }
        return value;
    }

    private static Object prettyPrint(String example, boolean convertJsonExamplesToYaml) {
        try {
            Object json = mapper.readValue(example, Object.class);
            if (convertJsonExamplesToYaml) {
                return json;
            } else {
                return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(json);
            }
        } catch (IOException e) {
            return example;
        }
    }

}
