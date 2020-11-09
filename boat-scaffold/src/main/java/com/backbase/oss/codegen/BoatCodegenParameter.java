package com.backbase.oss.codegen;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.node.ArrayNode;
import io.swagger.v3.oas.models.examples.Example;
import io.swagger.v3.oas.models.parameters.Parameter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.message.BasicNameValuePair;
import org.openapitools.codegen.CodegenParameter;

@Slf4j
public class BoatCodegenParameter extends CodegenParameter {

    public Map<String, Example> examples;

    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final ObjectReader paramReader = objectMapper.readerFor(new TypeReference<List<String>>() {
    });

    public BoatCodegenParameter() {
        super();
    }

    static BoatCodegenParameter fromCodegenParameter(Parameter parameter, CodegenParameter codegenParameter) {

        // Standard properties
        BoatCodegenParameter output = new BoatCodegenParameter();
        output.isFile = codegenParameter.isFile;
        output.hasMore = codegenParameter.hasMore;
        output.isContainer = codegenParameter.isContainer;
        output.secondaryParam = codegenParameter.secondaryParam;
        output.baseName = codegenParameter.baseName;
        output.paramName = codegenParameter.paramName;
        output.dataType = codegenParameter.dataType;
        output.datatypeWithEnum = codegenParameter.datatypeWithEnum;
        output.enumName = codegenParameter.enumName;
        output.dataFormat = codegenParameter.dataFormat;
        output.collectionFormat = codegenParameter.collectionFormat;
        output.isCollectionFormatMulti = codegenParameter.isCollectionFormatMulti;
        output.isPrimitiveType = codegenParameter.isPrimitiveType;
        output.isModel = codegenParameter.isModel;
        output.description = codegenParameter.description;
        output.unescapedDescription = codegenParameter.unescapedDescription;
        output.baseType = codegenParameter.baseType;
        output.isFormParam = codegenParameter.isFormParam;
        output.isQueryParam = codegenParameter.isQueryParam;
        output.isPathParam = codegenParameter.isPathParam;
        output.isHeaderParam = codegenParameter.isHeaderParam;
        output.isCookieParam = codegenParameter.isCookieParam;
        output.isBodyParam = codegenParameter.isBodyParam;
        output.required = codegenParameter.required;
        output.maximum = codegenParameter.maximum;
        output.exclusiveMaximum = codegenParameter.exclusiveMaximum;
        output.minimum = codegenParameter.minimum;
        output.exclusiveMinimum = codegenParameter.exclusiveMinimum;
        output.maxLength = codegenParameter.maxLength;
        output.minLength = codegenParameter.minLength;
        output.pattern = codegenParameter.pattern;
        output.maxItems = codegenParameter.maxItems;
        output.minItems = codegenParameter.minItems;
        output.uniqueItems = codegenParameter.uniqueItems;
        output.multipleOf = codegenParameter.multipleOf;
        output.jsonSchema = codegenParameter.jsonSchema;
        output.defaultValue = codegenParameter.defaultValue;
        output.example = formatExample(parameter, codegenParameter);
        output.isEnum = codegenParameter.isEnum;
        output.setMaxProperties(codegenParameter.getMaxProperties());
        output.setMinProperties(codegenParameter.getMinProperties());
        output.maximum = codegenParameter.maximum;
        output.minimum = codegenParameter.minimum;
        output.pattern = codegenParameter.pattern;

        // Copy Parameter Examples if applicable
        output.examples = formatExamples(parameter, codegenParameter);
        return output;
    }

    public static Map<String, Example> formatExamples(Parameter parameter, CodegenParameter codegenParameter) {


        Map<String, Example> result = new LinkedHashMap<>();

        if (parameter.getExamples() != null && parameter.getExamples().size() > 1) {
            return result;

//            parameter.getExamples().forEach((key, example) -> {
//                result.
//
//                result.put(key, formatExample(parameter, example, codegenParameter));
//            });
        } else {
            return null;
        }
    }


    public static String formatExample(Parameter parameter, CodegenParameter codegenParameter) {
        String result = codegenParameter.example;

        if (parameter.getExample() != null) {
            result = formatExample(parameter, parameter.getExample(), codegenParameter);
        } else if (hasSingleExampleInExamples(parameter)) {

            Optional<Map.Entry<String, Example>> examples = parameter.getExamples().entrySet().stream().findFirst();
            if (examples.isPresent()) {
                Example example = examples.get().getValue();
                result = formatExample(parameter, example.getValue(), codegenParameter);
            }

        }
        return result;
    }

    private static boolean hasSingleExampleInExamples(Parameter parameter) {
        return parameter.getExamples() != null && parameter.getExamples().size() == 1;
    }

    private static String formatExample(Parameter parameter, Object example, CodegenParameter codegenParameter) {
        String result = codegenParameter.example;
        switch (parameter.getStyle()) {
            case FORM:
                if (example instanceof ArrayNode && codegenParameter.isQueryParam) {
                    try {
                        List<String> values = paramReader.readValue((ArrayNode) example);
                        List<BasicNameValuePair> params = values.stream()
                            .map(value -> new BasicNameValuePair(codegenParameter.paramName, value))
                            .collect(Collectors.toList());
                        result = URLEncodedUtils.format(params, Charset.defaultCharset());
                    } catch (IOException e) {
                        log.warn("Failed to format query string parameter: {}", codegenParameter.example);
                        result = codegenParameter.example;
                    }
                }
                break;
            default:
                result = codegenParameter.example;
                break;
        }
        return result;
    }

}
