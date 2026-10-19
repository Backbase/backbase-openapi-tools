package com.backbase.oss.codegen.utils;

import io.swagger.v3.oas.models.info.Info;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DeprecationExtensionsTests {

    @Test
    void isSpecDeprecated_nullInfo() {
        assertFalse(DeprecationExtensions.isSpecDeprecated(null));
    }

    @Test
    void isSpecDeprecated_nullExtensions() {
        Info info = new Info();
        assertFalse(DeprecationExtensions.isSpecDeprecated(info));
    }

    @Test
    void isSpecDeprecated_booleanTrue() {
        Info info = new Info();
        Map<String, Object> extensions = new HashMap<>();
        extensions.put("x-deprecated", true);
        info.setExtensions(extensions);
        assertTrue(DeprecationExtensions.isSpecDeprecated(info));
    }

    @Test
    void isSpecDeprecated_booleanFalse() {
        Info info = new Info();
        Map<String, Object> extensions = new HashMap<>();
        extensions.put("x-deprecated", false);
        info.setExtensions(extensions);
        assertFalse(DeprecationExtensions.isSpecDeprecated(info));
    }

    @Test
    void isSpecDeprecated_stringTrue() {
        Info info = new Info();
        Map<String, Object> extensions = new HashMap<>();
        extensions.put("x-deprecated", "true");
        info.setExtensions(extensions);
        assertTrue(DeprecationExtensions.isSpecDeprecated(info));
    }

    @Test
    void isSpecDeprecated_stringTrueUppercase() {
        Info info = new Info();
        Map<String, Object> extensions = new HashMap<>();
        extensions.put("x-deprecated", "TRUE");
        info.setExtensions(extensions);
        assertTrue(DeprecationExtensions.isSpecDeprecated(info));
    }

    @Test
    void isSpecDeprecated_stringTrueMixed() {
        Info info = new Info();
        Map<String, Object> extensions = new HashMap<>();
        extensions.put("x-deprecated", "TrUe");
        info.setExtensions(extensions);
        assertTrue(DeprecationExtensions.isSpecDeprecated(info));
    }

    @Test
    void isSpecDeprecated_stringFalse() {
        Info info = new Info();
        Map<String, Object> extensions = new HashMap<>();
        extensions.put("x-deprecated", "false");
        info.setExtensions(extensions);
        assertFalse(DeprecationExtensions.isSpecDeprecated(info));
    }

    @Test
    void isSpecDeprecated_stringEmpty() {
        Info info = new Info();
        Map<String, Object> extensions = new HashMap<>();
        extensions.put("x-deprecated", "");
        info.setExtensions(extensions);
        assertFalse(DeprecationExtensions.isSpecDeprecated(info));
    }

    @Test
    void isSpecDeprecated_numberZero() {
        Info info = new Info();
        Map<String, Object> extensions = new HashMap<>();
        extensions.put("x-deprecated", 0);
        info.setExtensions(extensions);
        assertFalse(DeprecationExtensions.isSpecDeprecated(info));
    }

    @Test
    void isSpecDeprecated_numberOne() {
        Info info = new Info();
        Map<String, Object> extensions = new HashMap<>();
        extensions.put("x-deprecated", 1);
        info.setExtensions(extensions);
        assertFalse(DeprecationExtensions.isSpecDeprecated(info));
    }

    @Test
    void isSpecDeprecated_keyMissing() {
        Info info = new Info();
        Map<String, Object> extensions = new HashMap<>();
        info.setExtensions(extensions);
        assertFalse(DeprecationExtensions.isSpecDeprecated(info));
    }

    @Test
    void getSunsetDate_nullInfo() {
        Optional<LocalDate> result = DeprecationExtensions.getSunsetDate(null);
        assertTrue(result.isEmpty());
    }

    @Test
    void getSunsetDate_nullExtensions() {
        Info info = new Info();
        Optional<LocalDate> result = DeprecationExtensions.getSunsetDate(info);
        assertTrue(result.isEmpty());
    }

    @Test
    void getSunsetDate_validDate() {
        Info info = new Info();
        Map<String, Object> extensions = new HashMap<>();
        extensions.put("x-sunset-date", "2026-12-31");
        info.setExtensions(extensions);
        Optional<LocalDate> result = DeprecationExtensions.getSunsetDate(info);
        assertTrue(result.isPresent());
        assertEquals(LocalDate.of(2026, 12, 31), result.get());
    }

    @Test
    void getSunsetDate_invalidDate() {
        Info info = new Info();
        Map<String, Object> extensions = new HashMap<>();
        extensions.put("x-sunset-date", "not-a-date");
        info.setExtensions(extensions);
        Optional<LocalDate> result = DeprecationExtensions.getSunsetDate(info);
        assertTrue(result.isEmpty());
    }

    @Test
    void getSunsetDate_emptyString() {
        Info info = new Info();
        Map<String, Object> extensions = new HashMap<>();
        extensions.put("x-sunset-date", "");
        info.setExtensions(extensions);
        Optional<LocalDate> result = DeprecationExtensions.getSunsetDate(info);
        assertTrue(result.isEmpty());
    }

    @Test
    void getSunsetDate_keyMissing() {
        Info info = new Info();
        Map<String, Object> extensions = new HashMap<>();
        info.setExtensions(extensions);
        Optional<LocalDate> result = DeprecationExtensions.getSunsetDate(info);
        assertTrue(result.isEmpty());
    }

    @Test
    void buildDeprecationMessage_withoutDate() {
        String message = DeprecationExtensions.buildDeprecationMessage(Optional.empty());
        assertEquals("This API is deprecated.", message);
    }

    @Test
    void buildDeprecationMessage_withDate() {
        LocalDate date = LocalDate.of(2026, 12, 31);
        String message = DeprecationExtensions.buildDeprecationMessage(Optional.of(date));
        assertEquals("This API is deprecated and will be removed on 2026-12-31.", message);
    }
}
