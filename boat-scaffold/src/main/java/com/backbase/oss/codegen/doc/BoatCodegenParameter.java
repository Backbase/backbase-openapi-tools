package com.backbase.oss.codegen.doc;

import com.backbase.oss.codegen.CodegenException;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.examples.Example;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.parameters.RequestBody;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.stream.Collectors;
import lombok.Data;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.openapitools.codegen.CodegenParameter;

@Slf4j
@ToString(callSuper = true)
@Data
public class BoatCodegenParameter extends CodegenParameter {

    private List<BoatExample> examples;
    private String dataTypeDisplayName;

    private Parameter parameter;

    private RequestBody requestBody;

    public boolean hasDefaultValue() {
        return StringUtils.isNotEmpty(defaultValue);
    }

    public boolean hasSingleExample() {
        return example != null;
    }

    public boolean hasExamples() {
        return examples != null && !examples.isEmpty();
    }

    public boolean isRange() {
        return minimum != null && maximum != null;
    }

    public BoatCodegenParameter() {
        super();
    }


    @SuppressWarnings("java:S3776")
    public static BoatCodegenParameter fromCodegenParameter(CodegenParameter codegenParameter) {

        // Standard properties
        BoatCodegenParameter output = new BoatCodegenParameter();
        output.isFile = codegenParameter.isFile;
        output.isContainer = codegenParameter.isContainer;
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
        output.defaultValue = "null".equals(codegenParameter.defaultValue) ? null : codegenParameter.defaultValue;
        output.example = codegenParameter.example;
        output.isEnum = codegenParameter.isEnum;
        output.setMaxProperties(codegenParameter.getMaxProperties());
        output.setMinProperties(codegenParameter.getMinProperties());
        output.maximum = codegenParameter.maximum;
        output.minimum = codegenParameter.minimum;
        output.pattern = codegenParameter.pattern;

        if (codegenParameter._enum != null) {
            output._enum = new ArrayList<>(codegenParameter._enum);
        }
        if (codegenParameter.allowableValues != null) {
            output.allowableValues = new HashMap<>(codegenParameter.allowableValues);
        }
        if (codegenParameter.items != null) {
            output.items = codegenParameter.items;
        }
        if (codegenParameter.mostInnerItems != null) {
            output.mostInnerItems = codegenParameter.mostInnerItems;
        }
        if (codegenParameter.vendorExtensions != null) {
            output.vendorExtensions = new HashMap<>(codegenParameter.vendorExtensions);
        }
        output.hasValidation = codegenParameter.hasValidation;
        output.isNullable = codegenParameter.isNullable;
        output.isBinary = codegenParameter.isBinary;
        output.isByteArray = codegenParameter.isByteArray;
        output.isString = codegenParameter.isString;
        output.isNumeric = codegenParameter.isNumeric;
        output.isInteger = codegenParameter.isInteger;
        output.isLong = codegenParameter.isLong;
        output.isDouble = codegenParameter.isDouble;
        output.isFloat = codegenParameter.isFloat;
        output.isNumber = codegenParameter.isNumber;
        output.isBoolean = codegenParameter.isBoolean;
        output.isDate = codegenParameter.isDate;
        output.isDateTime = codegenParameter.isDateTime;
        output.isUuid = codegenParameter.isUuid;
        output.isUri = codegenParameter.isUri;
        output.isEmail = codegenParameter.isEmail;
        output.isFreeFormObject = codegenParameter.isFreeFormObject;
        output.isAnyType = codegenParameter.isAnyType;
        output.isContainer = codegenParameter.isContainer;
        output.isMap = codegenParameter.isMap;
        output.isExplode = codegenParameter.isExplode;
        output.style = codegenParameter.style;

        if (codegenParameter instanceof BoatCodegenParameter) {
            output.examples = ((BoatCodegenParameter) codegenParameter).examples;
            output.dataTypeDisplayName = ((BoatCodegenParameter) codegenParameter).dataTypeDisplayName;
            output.parameter = ((BoatCodegenParameter) codegenParameter).parameter;
            output.setRequestBody(((BoatCodegenParameter) codegenParameter).requestBody);
        } else {
            if (output.dataType != null && output.dataType.startsWith("array")) {
                output.dataTypeDisplayName = "array of " + output.baseType.toLowerCase() + "s";
            } else {
                if (output.dataType != null) {
                    output.dataTypeDisplayName = output.dataType.toLowerCase();
                }
            }
        }
        if (output.getContent() == null) {
            output.setContent(new LinkedHashMap<>());
        }
        return output;
    }


    @Override
    public CodegenParameter copy() {
        return fromCodegenParameter(this);
    }

    public static BoatCodegenParameter fromCodegenParameter(Parameter parameter, CodegenParameter codegenParameter, OpenAPI openAPI) {
        BoatCodegenParameter boatCodegenParameter = fromCodegenParameter(codegenParameter);
        boatCodegenParameter.parameter = parameter;

        if (boatCodegenParameter.examples == null) {
            boatCodegenParameter.examples = new ArrayList<>();
        }
        // Copy Parameter Examples if applicable
        if (parameter.getExample() != null) {
            Object example = parameter.getExample();
            BoatExample boatExample = new BoatExample("example", codegenParameter.baseType, new Example().value(example), false);
            if (example instanceof ObjectNode && ((ObjectNode) example).has("$ref")) {
                boatExample.getExample().set$ref(((ObjectNode) example).get("$ref").asText());
            }
            boatCodegenParameter.examples.add(boatExample);
        }

        if (parameter.getExamples() != null) {
            boatCodegenParameter.examples.addAll(parameter.getExamples().entrySet().stream()
                .map(stringExampleEntry -> new BoatExample(stringExampleEntry.getKey(),
                    codegenParameter.baseType, stringExampleEntry.getValue(), false))
                .collect(Collectors.toList()));

        }

        if (parameter.getContent() != null) {
            parameter.getContent().forEach((contentType, mediaType) ->
                dereferenceExamples(boatCodegenParameter, openAPI, contentType, mediaType));

        }
        return boatCodegenParameter;
    }

    //
    public static CodegenParameter fromCodegenParameter(CodegenParameter codegenParameter, RequestBody body, OpenAPI openAPI) {
        BoatCodegenParameter boatCodegenParameter = fromCodegenParameter(codegenParameter);
        boatCodegenParameter.setRequestBody(body);
        body.getContent().forEach((contentType, mediaType) ->
            dereferenceExamples(boatCodegenParameter, openAPI, contentType, mediaType));
        return boatCodegenParameter;
    }

    private static void dereferenceExamples(BoatCodegenParameter boatCodegenParameter, OpenAPI openAPI, String contentType, MediaType mediaType) {
        if (boatCodegenParameter.examples == null) {
            boatCodegenParameter.examples = new ArrayList<>();
        }
        BoatExampleUtils.convertExamples(openAPI, mediaType, null, contentType, boatCodegenParameter.examples);
        BoatExampleUtils.inlineExamples(boatCodegenParameter.paramName, boatCodegenParameter.examples, openAPI);
    }

}
