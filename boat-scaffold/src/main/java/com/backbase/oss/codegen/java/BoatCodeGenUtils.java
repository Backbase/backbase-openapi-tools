package com.backbase.oss.codegen.java;

import io.swagger.v3.oas.models.media.Schema;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.openapitools.codegen.CodegenProperty;
import org.openapitools.codegen.utils.ModelUtils;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
class BoatCodeGenUtils {

    /**
     * @return {@link CodegenValueType} to be used or empty if given schema is not array
     */
    public static Optional<CodegenValueType> getCollectionCodegenValue(CodegenProperty cp, Schema schema,
        boolean containerDefaultToNull, Map<String, String> types) {
        CodegenValueType valueType = null;
        if (ModelUtils.isSet(schema) && (schema.getDefault() == null)) {
            valueType = CodegenValueType.of(
                formatValue(cp, containerDefaultToNull, types.getOrDefault("set", "LinkedHashSet")));
        } else if (ModelUtils.isArraySchema(schema) && (schema.getDefault() == null)) {
            valueType = CodegenValueType.of(
                formatValue(cp, containerDefaultToNull, types.getOrDefault("array", "ArrayList")));
        }
        return Optional.ofNullable(valueType);
    }

    private static String formatValue(CodegenProperty cp, boolean defaultToNull, String javaSimpleType) {
        return (cp.required || !defaultToNull)
            ? String.format(Locale.ROOT, "new %s<>()", javaSimpleType)
            : null;
    }

    @RequiredArgsConstructor(staticName = "of")
    @Getter
    static class CodegenValueType {
        private final String value;
    }
}
