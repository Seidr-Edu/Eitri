package no.ntnu.eitri.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ValidationErrorTest {

    @Test
    void errorFactorySetsSeverityAndFields() {
        ValidationError error = ValidationError.error("CODE", "message", "field");

        assertEquals("CODE", error.code());
        assertEquals("message", error.message());
        assertEquals("field", error.field());
        assertEquals(ValidationSeverity.ERROR, error.severity());
    }

    @Test
    void warningFactorySetsSeverityAndFields() {
        ValidationError warning = ValidationError.warning("CODE", "message", "field");

        assertEquals(ValidationSeverity.WARNING, warning.severity());
    }
}
