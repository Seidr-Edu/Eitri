package no.ntnu.eitri.config;

/**
 * A typed validation diagnostic.
 */
public record ValidationError(
        String code,
        String message,
        String field,
        ValidationSeverity severity
) {
    public static ValidationError error(String code, String message, String field) {
        return new ValidationError(code, message, field, ValidationSeverity.ERROR);
    }

    public static ValidationError warning(String code, String message, String field) {
        return new ValidationError(code, message, field, ValidationSeverity.WARNING);
    }
}
