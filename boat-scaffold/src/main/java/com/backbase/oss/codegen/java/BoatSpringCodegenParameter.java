package com.backbase.oss.codegen.java;

import lombok.Getter;
import org.apache.commons.lang3.StringUtils;
import org.openapitools.codegen.CodegenParameter;

public class BoatSpringCodegenParameter extends CodegenParameter {

    @Getter
    public String packageName;
    @Getter
    public String simpleName;

    public BoatSpringCodegenParameter(CodegenParameter codegenParameter) {
        super();
        super.isFormParam = codegenParameter.isFormParam;
        super.isQueryParam = codegenParameter.isQueryParam;
        super.isPathParam = codegenParameter.isPathParam;
        super.isHeaderParam = codegenParameter.isHeaderParam;
        super.isCookieParam = codegenParameter.isCookieParam;
        super.isBodyParam = codegenParameter.isBodyParam;
        super.isContainer = codegenParameter.isContainer;
        super.isCollectionFormatMulti = codegenParameter.isCollectionFormatMulti;
        super.isPrimitiveType = codegenParameter.isPrimitiveType;
        super.isModel = codegenParameter.isModel;
        super.isExplode = codegenParameter.isExplode;
        super.isDeepObject = codegenParameter.isDeepObject;
        super.isAllowEmptyValue = codegenParameter.isAllowEmptyValue;
        super.baseName = codegenParameter.baseName;
        super.paramName = codegenParameter.paramName;
        super.dataType = codegenParameter.dataType;
        super.datatypeWithEnum = codegenParameter.datatypeWithEnum;
        super.dataFormat = codegenParameter.dataFormat;
        super.contentType = codegenParameter.contentType;
        super.collectionFormat = codegenParameter.collectionFormat;
        super.description = codegenParameter.description;
        super.unescapedDescription = codegenParameter.unescapedDescription;
        super.baseType = codegenParameter.baseType;
        super.defaultValue = codegenParameter.defaultValue;
        super.enumDefaultValue = codegenParameter.enumDefaultValue;
        super.enumName = codegenParameter.enumName;
        super.style = codegenParameter.style;
        super.nameInLowerCase = codegenParameter.nameInLowerCase;
        super.example = codegenParameter.example;
        super.jsonSchema = codegenParameter.jsonSchema;
        super.isString = codegenParameter.isString;
        super.isNumeric = codegenParameter.isNumeric;
        super.isInteger = codegenParameter.isInteger;
        super.isLong = codegenParameter.isLong;
        super.isNumber = codegenParameter.isNumber;
        super.isFloat = codegenParameter.isFloat;
        super.isDouble = codegenParameter.isDouble;
        super.isDecimal = codegenParameter.isDecimal;
        super.isByteArray = codegenParameter.isByteArray;
        super.isBinary = codegenParameter.isBinary;
        super.isBoolean = codegenParameter.isBoolean;
        super.isDate = codegenParameter.isDate;
        super.isDateTime = codegenParameter.isDateTime;
        super.isUuid = codegenParameter.isUuid;
        super.isUri = codegenParameter.isUri;
        super.isEmail = codegenParameter.isEmail;
        super.isPassword = codegenParameter.isPassword;
        super.isFreeFormObject = codegenParameter.isFreeFormObject;
        super.isAnyType = codegenParameter.isAnyType;
        super.isShort = codegenParameter.isShort;
        super.isUnboundedInteger = codegenParameter.isUnboundedInteger;
        super.isArray = codegenParameter.isArray;
        super.isMap = codegenParameter.isMap;
        super.isFile = codegenParameter.isFile;
        super.isEnum = codegenParameter.isEnum;
        super.isEnumRef = codegenParameter.isEnumRef;
        super._enum = codegenParameter._enum;
        super.allowableValues = codegenParameter.allowableValues;
        super.items = codegenParameter.items;
        super.additionalProperties = codegenParameter.additionalProperties;
        super.vars = codegenParameter.vars;
        super.requiredVars = codegenParameter.requiredVars;
        super.mostInnerItems = codegenParameter.mostInnerItems;
        super.vendorExtensions = codegenParameter.vendorExtensions;
        super.hasValidation = codegenParameter.hasValidation;
        super.isNullable = codegenParameter.isNullable;
        super.isDeprecated = codegenParameter.isDeprecated;
        super.required = codegenParameter.required;
        super.maximum = codegenParameter.maximum;
        super.exclusiveMaximum = codegenParameter.exclusiveMaximum;
        super.minimum = codegenParameter.minimum;
        super.exclusiveMinimum = codegenParameter.exclusiveMinimum;
        super.maxLength = codegenParameter.maxLength;
        super.minLength = codegenParameter.minLength;
        super.pattern = codegenParameter.pattern;
        super.maxItems = codegenParameter.maxItems;
        super.minItems = codegenParameter.minItems;
        super.uniqueItems = codegenParameter.uniqueItems;
        super.multipleOf = codegenParameter.multipleOf;
        super.isNull = codegenParameter.isNull;
        super.isVoid = codegenParameter.isVoid;
        super.setAdditionalPropertiesIsAnyType(codegenParameter.getAdditionalPropertiesIsAnyType());
        super.setHasVars(codegenParameter.getHasVars());
        super.setSchema(codegenParameter.getSchema());
        super.setUniqueItemsBoolean(codegenParameter.getUniqueItemsBoolean());
        super.setMaxProperties(codegenParameter.getMaxProperties());
        super.setMinProperties(codegenParameter.getMinProperties());
        super.setHasRequired(codegenParameter.getHasRequired());
        super.setHasDiscriminatorWithNonEmptyMapping(codegenParameter.getHasDiscriminatorWithNonEmptyMapping());
        super.setComposedSchemas(codegenParameter.getComposedSchemas());
        super.setHasMultipleTypes(codegenParameter.getHasMultipleTypes());
        super.setContent(codegenParameter.getContent());
        super.setRequiredVarsMap(codegenParameter.getRequiredVarsMap());
        super.setRef(codegenParameter.getRef());
        super.setAdditionalProperties(codegenParameter.getAdditionalProperties());
        if (StringUtils.contains(dataType, ".")) {
            packageName = StringUtils.substringBeforeLast(dataType, ".") + ".";
            simpleName = StringUtils.substringAfterLast(dataType, ".");
        } else {
            packageName = "";
            simpleName = datatypeWithEnum;
        }
    }

    public String toString() {
        return new StringBuilder(super.toString())
                .append("packageName:'").append(packageName)
                .append("',simpleName:'").append(simpleName)
                .append("'").toString();
    }

}
