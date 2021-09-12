package com.backbase.oss.codegen.doc;

import com.samskivert.mustache.Mustache;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.responses.ApiResponse;
import java.util.ArrayList;
import java.util.List;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.openapitools.codegen.CodegenResponse;

@Slf4j
@EqualsAndHashCode(callSuper = true)
@ToString
@SuppressWarnings("java:S2387")
public class BoatCodegenResponse extends CodegenResponse {

    public static final Mustache.Lambda escapeBackTicks = (fragment, writer) -> {
        String text = fragment.execute();
        writer.write(StringUtils.replace(text, "`", "\\`"));
    };

    private final List<BoatExample> examples = new ArrayList<>();

    public boolean hasExamples() {
        return !examples.isEmpty();
    }

    public boolean hasEmptyBody() {
        return (this.code.equals("204") || this.code.equals("201")) && !this.hasExamples();
    }

    public BoatCodegenResponse(CodegenResponse o, String responseCode, ApiResponse response, OpenAPI openAPI) {
        this.headers.addAll(o.headers);
        this.code = o.code;
        this.is1xx = o.is1xx;
        this.is2xx = o.is2xx;
        this.is3xx = o.is3xx;
        this.is4xx = o.is4xx;
        this.is5xx = o.is5xx;
        this.message = o.message;
        this.hasMore = o.hasMore;
        this.dataType = o.dataType;
        this.baseType = o.baseType;
        this.containerType = o.containerType;
        this.hasHeaders = o.hasHeaders;
        this.isString = o.isString;
        this.isNumeric = o.isNumeric;
        this.isInteger = o.isInteger;
        this.isLong = o.isLong;
        this.isNumber = o.isNumber;
        this.isFloat = o.isFloat;
        this.isDouble = o.isDouble;
        this.isByteArray = o.isByteArray;
        this.isBoolean = o.isBoolean;
        this.isDate = o.isDate;
        this.isDateTime = o.isDateTime;
        this.isUuid = o.isUuid;
        this.isEmail = o.isEmail;
        this.isModel = o.isModel;
        this.isFreeFormObject = o.isFreeFormObject;
        this.isAnyType = o.isAnyType;
        this.isDefault = o.isDefault;
        this.simpleType = o.simpleType;
        this.primitiveType = o.primitiveType;
        this.isMapContainer = o.isMapContainer;
        this.isListContainer = o.isListContainer;
        this.isBinary = o.isBinary;
        this.isFile = o.isFile;
        this.schema = o.schema;
        this.jsonSchema = o.jsonSchema;
        this.vendorExtensions = o.vendorExtensions;

        this.setMaxProperties(o.getMaxProperties());
        this.setMinProperties(o.getMinProperties());
        this.setUniqueItems(o.getUniqueItems());
        this.setMaxItems(o.getMaxItems());
        this.setMinItems(o.getMinItems());
        this.setMaxLength(o.getMaxLength());
        this.setMinLength(o.getMinLength());
        this.setExclusiveMinimum(o.getExclusiveMinimum());
        this.setExclusiveMinimum(o.getExclusiveMinimum());
        this.setMinimum(o.getMinimum());
        this.setMaximum(o.getMaximum());
        this.pattern = o.pattern;
        this.multipleOf = o.multipleOf;

        if (response.getContent() != null) {
            response.getContent().forEach((contentType, mediaType) -> BoatExampleUtils.convertExamples(openAPI,
                    mediaType, responseCode, contentType, examples));
            BoatExampleUtils.inlineExamples(responseCode, examples, openAPI);
        }
    }

    public List<BoatExample> getExamples() {
        return examples;
    }
}
