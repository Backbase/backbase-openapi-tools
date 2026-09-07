package com.backbase.oss.codegen.java;

import com.backbase.oss.codegen.utils.DeprecationExtensions;
import io.swagger.v3.oas.models.media.Schema;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.openapitools.codegen.CodegenModel;
import org.openapitools.codegen.CodegenOperation;
import org.openapitools.codegen.CodegenProperty;
import org.openapitools.codegen.model.ModelMap;
import org.openapitools.codegen.model.ModelsMap;
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

    public static void applyDeprecationIfNeeded(CodegenOperation codegenOperation, Map<String, Object> additionalProperties) {
        Boolean specDeprecated = (Boolean) additionalProperties.get("boatApiDeprecated");
        if (specDeprecated != null && specDeprecated) {
            codegenOperation.isDeprecated = true;
            String message = (String) additionalProperties.get("boatApiDeprecationMessage");
            if (message != null && !codegenOperation.vendorExtensions.containsKey(DeprecationExtensions.X_BOAT_DEPRECATION_MESSAGE)) {
                codegenOperation.vendorExtensions.put(DeprecationExtensions.X_BOAT_DEPRECATION_MESSAGE, message);
            }
        }
    }

    public static void applyDeprecationToPropertyIfNeeded(CodegenProperty property, Map<String, Object> additionalProperties) {
        Boolean specDeprecated = (Boolean) additionalProperties.get("boatApiDeprecated");
        if (specDeprecated != null && specDeprecated) {
            property.deprecated = true;
            String message = (String) additionalProperties.get("boatApiDeprecationMessage");
            if (message != null && !property.vendorExtensions.containsKey(DeprecationExtensions.X_BOAT_DEPRECATION_MESSAGE)) {
                property.vendorExtensions.put(DeprecationExtensions.X_BOAT_DEPRECATION_MESSAGE, message);
            }
        }
    }

    public static void applyDeprecationToAllModelsIfNeeded(Map<String, ModelsMap> objs, Map<String, Object> additionalProperties) {
        Boolean specDeprecated = (Boolean) additionalProperties.get("boatApiDeprecated");
        if (specDeprecated != null && specDeprecated) {
            String message = (String) additionalProperties.get("boatApiDeprecationMessage");
            for (ModelsMap modelsMap : objs.values()) {
                for (ModelMap modelMap : modelsMap.getModels()) {
                    CodegenModel model = modelMap.getModel();
                    model.isDeprecated = true;
                    if (message != null && !model.vendorExtensions.containsKey(DeprecationExtensions.X_BOAT_DEPRECATION_MESSAGE)) {
                        model.vendorExtensions.put(DeprecationExtensions.X_BOAT_DEPRECATION_MESSAGE, message);
                    }
                }
            }
        }
    }

    @RequiredArgsConstructor(staticName = "of")
    @Getter
    static class CodegenValueType {
        private final String value;
    }
}
