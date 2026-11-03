package com.backbase.oss.boat.asyncapi;

import java.util.ArrayList;
import java.util.List;
import lombok.Getter;

@Getter
public class AsyncApiValidationResult {

    private final List<String> errors = new ArrayList<>();
    private final List<String> warnings = new ArrayList<>();
    private String title;

    void setTitle(String title) {
        this.title = title;
    }

    void error(String message) {
        errors.add(message);
    }

    void warning(String message) {
        warnings.add(message);
    }

    public boolean isValid() {
        return errors.isEmpty();
    }

    public boolean hasWarnings() {
        return !warnings.isEmpty();
    }
}
