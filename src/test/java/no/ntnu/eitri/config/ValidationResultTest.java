package no.ntnu.eitri.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ValidationResultTest {

    @Test
    void validityDependsOnErrorSeverity() {
        ValidationResult result = new ValidationResult();
        result.add(ValidationError.warning("WARN", "warn", "field"));
        assertTrue(result.isValid());

        result.add(ValidationError.error("ERR", "err", "field"));
        assertFalse(result.isValid());
    }

    @Test
    void formatMessagesJoinsWithNewlines() {
        ValidationResult result = new ValidationResult();
        result.add(ValidationError.error("A", "first", "field"));
        result.add(ValidationError.error("B", "second", "field"));

        assertEquals("first\nsecond", result.formatMessages());
    }
}
