package com.backbase.oss.boat.asyncapi;

/**
 * Naming helpers for types derived from AsyncAPI names.
 */
public final class AsyncApiNames {

    private AsyncApiNames() {
    }

    /**
     * Converts a message or operation name to a PascalCase type name: {@code order-placed} and {@code orderPlaced}
     * both become {@code OrderPlaced}.
     */
    public static String toTypeName(String name) {
        if (name == null || name.isEmpty()) {
            return "Message";
        }
        StringBuilder result = new StringBuilder();
        boolean upperNext = true;
        for (char c : name.toCharArray()) {
            if (!Character.isLetterOrDigit(c)) {
                upperNext = true;
            } else if (upperNext) {
                result.append(Character.toUpperCase(c));
                upperNext = false;
            } else {
                result.append(c);
            }
        }
        if (result.length() == 0) {
            return "Message";
        }
        if (Character.isDigit(result.charAt(0))) {
            result.insert(0, '_');
        }
        return result.toString();
    }
}
