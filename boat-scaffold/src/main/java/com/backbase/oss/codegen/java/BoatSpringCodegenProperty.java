package com.backbase.oss.codegen.java;

import lombok.Getter;
import org.apache.commons.lang3.StringUtils;
import org.openapitools.codegen.CodegenProperty;

import java.util.Objects;

public class BoatSpringCodegenProperty extends CodegenProperty {

    @Getter
    public String packageName;
    @Getter
    public String simpleName;

    public BoatSpringCodegenProperty(CodegenProperty codegenProperty) {
        super();
        super.openApiType =                         codegenProperty.openApiType;
        super.baseName =                            codegenProperty.baseName;
        super.complexType =                         codegenProperty.complexType;
        super.getter =                              codegenProperty.getter;
        super.setter =                              codegenProperty.setter;
        super.description =                         codegenProperty.description;
        super.dataType =                            codegenProperty.dataType;
        super.datatypeWithEnum =                    codegenProperty.datatypeWithEnum;
        super.dataFormat =                          codegenProperty.dataFormat;
        super.name =                                codegenProperty.name;
        super.min =                                 codegenProperty.min;
        super.max =                                 codegenProperty.max;
        super.defaultValue =                        codegenProperty.defaultValue;
        super.defaultValueWithParam =               codegenProperty.defaultValueWithParam;
        super.baseType =                            codegenProperty.baseType;
        super.containerType =                       codegenProperty.containerType;
        super.title =                               codegenProperty.title;
        super.unescapedDescription =                codegenProperty.unescapedDescription;
        super.maxLength =                          codegenProperty.maxLength;
        super.minLength =                          codegenProperty.minLength;
        super.pattern =                             codegenProperty.pattern;
        super.example =                             codegenProperty.example;
        super.jsonSchema =                          codegenProperty.jsonSchema;
        super.minimum =                             codegenProperty.minimum;
        super.maximum =                             codegenProperty.maximum;
        super.multipleOf =                          codegenProperty.multipleOf;
        super.exclusiveMinimum =                   codegenProperty.exclusiveMinimum;
        super.exclusiveMaximum =                   codegenProperty.exclusiveMaximum;
        super.required =                           codegenProperty.required;
        super.deprecated =                         codegenProperty.deprecated;
        super.hasMoreNonReadOnly =                 codegenProperty.hasMoreNonReadOnly;
        super.isPrimitiveType =                    codegenProperty.isPrimitiveType;
        super.isModel =                            codegenProperty.isModel;
        super.isContainer =                        codegenProperty.isContainer;
        super.isString =                           codegenProperty.isString;
        super.isNumeric =                          codegenProperty.isNumeric;
        super.isInteger =                          codegenProperty.isInteger;
        super.isShort =                            codegenProperty.isShort;
        super.isLong =                             codegenProperty.isLong;
        super.isUnboundedInteger =                 codegenProperty.isUnboundedInteger;
        super.isNumber =                           codegenProperty.isNumber;
        super.isFloat =                            codegenProperty.isFloat;
        super.isDouble =                           codegenProperty.isDouble;
        super.isDecimal =                          codegenProperty.isDecimal;
        super.isByteArray =                        codegenProperty.isByteArray;
        super.isBinary =                           codegenProperty.isBinary;
        super.isFile =                             codegenProperty.isFile;
        super.isBoolean =                          codegenProperty.isBoolean;
        super.isDate =                             codegenProperty.isDate;
        super.isDateTime =                         codegenProperty.isDateTime;
        super.isUuid =                             codegenProperty.isUuid;
        super.isUri =                              codegenProperty.isUri;
        super.isEmail =                            codegenProperty.isEmail;
        super.isPassword =                         codegenProperty.isPassword;
        super.isNull =                             codegenProperty.isNull;
        super.isVoid =                             codegenProperty.isVoid;
        super.isFreeFormObject =                   codegenProperty.isFreeFormObject;
        super.isAnyType =                          codegenProperty.isAnyType;
        super.isArray =                            codegenProperty.isArray;
        super.isMap =                              codegenProperty.isMap;
        super.isEnum =                             codegenProperty.isEnum;
        super.isInnerEnum =                        codegenProperty.isInnerEnum;
        super.isEnumRef =                          codegenProperty.isEnumRef;
        super.isReadOnly =                         codegenProperty.isReadOnly;
        super.isWriteOnly =                        codegenProperty.isWriteOnly;
        super.isNullable =                         codegenProperty.isNullable;
        super.isSelfReference =                    codegenProperty.isSelfReference;
        super.isCircularReference =                codegenProperty.isCircularReference;
        super.isDiscriminator =                    codegenProperty.isDiscriminator;
        super.isNew =                              codegenProperty.isNew;
        super.isOverridden =                       codegenProperty.isOverridden;
        super._enum =                              codegenProperty._enum;
        super.allowableValues =                    codegenProperty.allowableValues;
        super.items =                              codegenProperty.items;
        super.additionalProperties =               codegenProperty.additionalProperties;
        super.vars =                               codegenProperty.vars;
        super.requiredVars =                       codegenProperty.requiredVars;
        super.mostInnerItems =                     codegenProperty.mostInnerItems;
        super.vendorExtensions =                   codegenProperty.vendorExtensions;
        super.hasValidation =                      codegenProperty.hasValidation;
        super.isInherited =                        codegenProperty.isInherited;
        super.discriminatorValue =                 codegenProperty.discriminatorValue;
        super.nameInLowerCase =                    codegenProperty.nameInLowerCase;
        super.nameInCamelCase =                    codegenProperty.nameInCamelCase;
        super.nameInSnakeCase =                    codegenProperty.nameInSnakeCase;
        super.enumName =                           codegenProperty.enumName;
        super.maxItems =                           codegenProperty.maxItems;
        super.minItems =                           codegenProperty.minItems;
        super.isXmlAttribute =                     codegenProperty.isXmlAttribute = false;
        super.xmlPrefix =                          codegenProperty.xmlPrefix;
        super.xmlName =                            codegenProperty.xmlName;
        super.xmlNamespace =                       codegenProperty.xmlNamespace;
        super.isXmlWrapped =                       codegenProperty.isXmlWrapped = false;
        super.setAdditionalPropertiesIsAnyType (      codegenProperty.getAdditionalPropertiesIsAnyType());
        super.setHasVars (                            codegenProperty.getHasVars());
        super.setHasRequired (                        codegenProperty.getHasRequired());
        super.setHasDiscriminatorWithNonEmptyMapping (codegenProperty.getHasDiscriminatorWithNonEmptyMapping());
        super.setComposedSchemas (                    codegenProperty.getComposedSchemas ());
        super.setHasMultipleTypes (                   codegenProperty.getHasMultipleTypes());
        super.setRequiredVarsMap (                    codegenProperty.getRequiredVarsMap());
        super.setRef (                                codegenProperty.getRef());
        super.setSchemaIsFromAdditionalProperties (   codegenProperty.getSchemaIsFromAdditionalProperties());
        super.setIsBooleanSchemaTrue (                codegenProperty.getIsBooleanSchemaTrue());
        super.setIsBooleanSchemaFalse (               codegenProperty.getIsBooleanSchemaFalse());
        super.setFormat (                             codegenProperty.getFormat());
        super.setDependentRequired (                  codegenProperty.getDependentRequired());
        super.setContains (                           codegenProperty.getContains());
        super.setMaxProperties (                      codegenProperty.getMaxProperties());
        super.setMinProperties (                      codegenProperty.getMinProperties());
        super.setUniqueItems (                        codegenProperty.getUniqueItems());
        super.setUniqueItemsBoolean (                 codegenProperty.getUniqueItemsBoolean());
        if (StringUtils.contains(complexType, ".")) {
            packageName = StringUtils.substringBeforeLast(complexType, ".") + ".";
            simpleName = StringUtils.substringAfterLast(complexType, ".");
        } else {
            packageName = "";
            simpleName = datatypeWithEnum;
        }
        if (!required) {
            defaultValue = null;
        }
    }
    @Override
    public String toString() {
        return new StringBuilder(super.toString())
                .append("packageName:'").append(packageName)
                .append("',simpleName:'").append(simpleName)
                .append("'").toString();
    }

    @Override
    public boolean equals(Object o) {
        return super.equals(o) &&
                o instanceof BoatSpringCodegenProperty &&
                Objects.equals(((BoatSpringCodegenProperty)o).packageName, this.packageName) &&
                Objects.equals(((BoatSpringCodegenProperty)o).simpleName, this.simpleName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), packageName, simpleName);
    }

}
