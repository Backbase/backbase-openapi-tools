package com.backbase.oss.codegen;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.examples.Example;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.parameters.RequestBody;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.openapitools.codegen.CodegenParameter;

@Slf4j
@ToString(callSuper = true)
public class BoatCodegenParameter extends CodegenParameter {

    public List<BoatExample> examples;

    public boolean hasSingleExample() {
        return example != null;
    }

    public boolean hasExamples() {
        return examples != null && !examples.isEmpty();
    }

    public BoatCodegenParameter() {
        super();
    }

    static BoatCodegenParameter fromCodegenParameter(CodegenParameter codegenParameter) {

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

        output.isEnum = codegenParameter.isEnum;
        output.setMaxProperties(codegenParameter.getMaxProperties());
        output.setMinProperties(codegenParameter.getMinProperties());
        output.maximum = codegenParameter.maximum;
        output.minimum = codegenParameter.minimum;
        output.pattern = codegenParameter.pattern;
        output.example = codegenParameter.example;
        if(codegenParameter instanceof BoatCodegenParameter) {
            output.examples = ((BoatCodegenParameter)codegenParameter).examples;
        }

        return output;
    }


    @Override
    public CodegenParameter copy() {
        BoatCodegenParameter boatCodegenParameter = fromCodegenParameter(this);
        boatCodegenParameter.examples = this.examples;
        boatCodegenParameter.example = this.example;
        return boatCodegenParameter;
    }

    static BoatCodegenParameter fromCodegenParameter(Parameter parameter, CodegenParameter codegenParameter) {
        BoatCodegenParameter output = fromCodegenParameter(codegenParameter);
        // Copy Parameter Examples if applicable\
        if (parameter.getExamples() != null) {
            output.examples = parameter.getExamples().entrySet().stream()
                .map(stringExampleEntry -> new BoatExample(stringExampleEntry.getKey(), null, stringExampleEntry.getValue()))
                .collect(Collectors.toList());
        }
        return output;
    }

    //
    public static CodegenParameter fromCodegenParameter(CodegenParameter codegenParameter, RequestBody body, Set<String> imports, String bodyParameterName, OpenAPI openAPI) {
        BoatCodegenParameter output = fromCodegenParameter(codegenParameter);
        body.getContent().forEach((contentType, mediaType) -> {
            if (output.examples == null) {
                output.examples = new ArrayList<>();
            }
            List<BoatExample> examples = new ArrayList<>();

            if (mediaType.getExample() != null) {
                examples.add(new BoatExample(null, null, new Example().value(mediaType.getExample())));
            } else if (mediaType.getExamples() != null) {
                mediaType.getExamples().forEach((key, example) -> {
                    examples.add(new BoatExample(key, contentType, example));
                });
            }
            // dereference examples
            examples.stream().filter(boatExample -> boatExample.example.get$ref() != null)
                .forEach(boatExample -> {

                    String ref = StringUtils.substringAfterLast(boatExample.example.get$ref(), "/");
                    Example example = openAPI.getComponents().getExamples().get(ref);
                    if (example == null) {
                        log.warn("Example ref: {} refers to an example that does not exist", ref);
                    } else {
                        log.debug("Replacing Example ref: {} with example from components: {}", ref, example);
                        boatExample.example = example;
                    }
                });
            output.examples.addAll(examples);
        });
        return output;
    }
}
