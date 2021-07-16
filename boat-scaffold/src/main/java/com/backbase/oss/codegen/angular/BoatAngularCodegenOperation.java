package com.backbase.oss.codegen.angular;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.openapitools.codegen.CodegenOperation;

@EqualsAndHashCode(callSuper = true)
@Getter
public class BoatAngularCodegenOperation extends CodegenOperation {

    public final String pattern;

    public BoatAngularCodegenOperation(CodegenOperation o) {
        this.responseHeaders.addAll(o.responseHeaders);
        this.hasAuthMethods = o.hasAuthMethods;
        this.hasConsumes = o.hasConsumes;
        this.hasProduces = o.hasProduces;
        this.hasParams = o.hasParams;
        this.hasOptionalParams = o.hasOptionalParams;
        this.hasRequiredParams = o.hasRequiredParams;
        this.returnTypeIsPrimitive = o.returnTypeIsPrimitive;
        this.returnSimpleType = o.returnSimpleType;
        this.subresourceOperation = o.subresourceOperation;
        this.isMap = o.isMap;
        this.isArray = o.isArray;
        this.isMultipart = o.isMultipart;
        this.isResponseBinary = o.isResponseBinary;
        this.isResponseFile = o.isResponseFile;
        this.hasReference = o.hasReference;
        this.isRestfulIndex = o.isRestfulIndex;
        this.isRestfulShow = o.isRestfulShow;
        this.isRestfulCreate = o.isRestfulCreate;
        this.isRestfulUpdate = o.isRestfulUpdate;
        this.isRestfulDestroy = o.isRestfulDestroy;
        this.isRestful = o.isRestful;
        this.isDeprecated = o.isDeprecated;
        this.isCallbackRequest = o.isCallbackRequest;
        this.path = o.path;
        this.operationId = o.operationId;
        this.returnType = o.returnType;
        this.httpMethod = o.httpMethod;
        this.returnBaseType = o.returnBaseType;
        this.returnContainer = o.returnContainer;
        this.summary = o.summary;
        this.unescapedNotes = o.unescapedNotes;
        this.notes = o.notes;
        this.baseName = o.baseName;
        this.defaultResponse = o.defaultResponse;
        this.discriminator = o.discriminator;
        this.consumes = o.consumes;
        this.produces = o.produces;
        this.prioritizedContentTypes = o.prioritizedContentTypes;
        this.servers = o.servers;
        this.bodyParam = o.bodyParam;
        this.allParams = o.allParams;
        this.bodyParams = o.bodyParams;
        this.pathParams = o.pathParams;
        this.queryParams = o.queryParams;
        this.headerParams = o.headerParams;
        this.formParams = o.formParams;
        this.cookieParams = o.cookieParams;
        this.requiredParams = o.requiredParams;
        this.optionalParams = o.optionalParams;
        this.authMethods = o.authMethods;
        this.tags = o.tags;
        this.responses = o.responses;
        this.callbacks = o.callbacks;
        this.imports = o.imports;
        this.examples = o.examples;
        this.requestBodyExamples = o.requestBodyExamples;
        this.externalDocs = o.externalDocs;
        this.vendorExtensions = o.vendorExtensions;
        this.nickname = o.nickname;
        this.operationIdOriginal = o.operationIdOriginal;
        this.operationIdLowerCase = o.operationIdLowerCase;
        this.operationIdCamelCase = o.operationIdCamelCase;
        this.operationIdSnakeCase = o.operationIdSnakeCase;

        this.pattern = o.path;
    }
}
