package com.backbase.oss.codegen.utils;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Map;
import java.util.Optional;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;

public final class DeprecationExtensions {
    public static final String X_DEPRECATED = "x-deprecated";
    public static final String X_SUNSET_DATE = "x-sunset-date";
    public static final String X_BOAT_DEPRECATION_MESSAGE = "x-boat-deprecation-message";
    private static final String BOAT_API_DEPRECATED = "boatApiDeprecated";
    private static final String BOAT_API_DEPRECATION_MESSAGE = "boatApiDeprecationMessage";

    private DeprecationExtensions() {
        // utility class
    }

    /**
     * Sets the deprecation related additional properties.
     *
     * @param openAPI The spec.
     * @param properties The additional properties to populate.
     */
    public static void populateDeprecationAdditionalProperties(OpenAPI openAPI, Map<String, Object> properties) {
        boolean specDeprecated = DeprecationExtensions.isSpecDeprecated(openAPI.getInfo());
        Optional<LocalDate> sunsetDate = DeprecationExtensions.getSunsetDate(openAPI.getInfo());
        properties.put(BOAT_API_DEPRECATED, specDeprecated);
        properties.put(BOAT_API_DEPRECATION_MESSAGE,
                DeprecationExtensions.buildDeprecationMessage(sunsetDate));
    }

    public static boolean isSpecDeprecated(Info info) {
        if (info == null || info.getExtensions() == null) {
            return false;
        }
        return isTrue(info.getExtensions().get(X_DEPRECATED));
    }

    public static Optional<LocalDate> getSunsetDate(Info info) {
        if (info == null || info.getExtensions() == null) {
            return Optional.empty();
        }
        Object dateValue = info.getExtensions().get(X_SUNSET_DATE);
        if (dateValue == null) {
            return Optional.empty();
        }
        String dateStr = dateValue.toString().trim();
        if (dateStr.isEmpty()) {
            return Optional.empty();
        }
        try {
            LocalDate date = LocalDate.parse(dateStr, DateTimeFormatter.ISO_LOCAL_DATE);
            return Optional.of(date);
        } catch (DateTimeParseException e) {
            return Optional.empty();
        }
    }

    public static String buildDeprecationMessage(Optional<LocalDate> sunsetDate) {
        if (sunsetDate.isEmpty()) {
            return "This API is deprecated.";
        }
        return "This API is deprecated and will be removed on " + sunsetDate.get() + ".";
    }

    private static boolean isTrue(Object value) {
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        if (value instanceof String) {
            return ((String) value).equalsIgnoreCase("true");
        }
        return false;
    }
}
