package no.ntnu.eitri.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for Visibility usage in model defaults.
 */
class VisibilityTest {

    @Test
    @DisplayName("UmlField default visibility is PACKAGE")
    void fieldDefaultVisibility() {
        UmlField field = UmlField.builder()
                .name("value")
                .type("int")
                .build();

        assertEquals(Visibility.PACKAGE, field.getVisibility());
    }

    @Test
    @DisplayName("UmlMethod default visibility is PACKAGE")
    void methodDefaultVisibility() {
        UmlMethod method = UmlMethod.builder()
                .name("compute")
                .returnType("int")
                .build();

        assertEquals(Visibility.PACKAGE, method.getVisibility());
    }
}
