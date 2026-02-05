package no.ntnu.eitri.config;

import java.util.ArrayList;
import java.util.List;

/**
 * Validation result containing typed diagnostics.
 */
public final class ValidationResult {

    private final List<ValidationError> errors = new ArrayList<>();

    public void add(ValidationError error) {
        if (error != null) {
            errors.add(error);
        }
    }

    public List<ValidationError> getErrors() {
        return List.copyOf(errors);
    }

    public boolean isValid() {
        return errors.stream().noneMatch(e -> e.severity() == ValidationSeverity.ERROR);
    }

    public String formatMessages() {
        StringBuilder sb = new StringBuilder();
        for (ValidationError error : errors) {
            if (!sb.isEmpty()) {
                sb.append("\n");
            }
            sb.append(error.message());
        }
        return sb.toString();
    }
}
