package no.ntnu.eitri.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for UmlField model behavior.
 */
class UmlFieldTest {

    @Nested
    @DisplayName("Field defaults and modifiers")
    class ModelBehavior {

        @Test
        @DisplayName("Default visibility is package")
        void defaultVisibility() {
            UmlField field = UmlField.builder()
                    .name("value")
                    .type("int")
                    .build();

            assertEquals(Visibility.PACKAGE, field.getVisibility());
        }

        @Test
        @DisplayName("Static modifier sets isStatic")
        void staticModifier() {
            UmlField field = UmlField.builder()
                    .name("instance")
                    .type("Singleton")
                    .addModifier(Modifier.STATIC)
                    .build();

            assertTrue(field.isStatic());
        }

        @Test
        @DisplayName("Final modifier sets isFinal")
        void finalModifier() {
            UmlField field = UmlField.builder()
                    .name("MAX")
                    .type("int")
                    .addModifier(Modifier.FINAL)
                    .build();

            assertTrue(field.isFinal());
        }

        @Test
        @DisplayName("ReadOnly flag preserved")
        void readOnlyFlag() {
            UmlField field = UmlField.builder()
                    .name("id")
                    .type("Long")
                    .readOnly(true)
                    .build();

            assertTrue(field.isReadOnly());
        }
    }

    @Nested
    @DisplayName("Type simplification")
    class TypeSimplification {

        @Test
        @DisplayName("Fully qualified type extracts simple name")
        void qualifiedType() {
            UmlField field = UmlField.builder()
                    .name("date")
                    .type("java.time.LocalDate")
                    .visibility(Visibility.PRIVATE)
                    .build();

            assertEquals("LocalDate", field.getTypeSimpleName());
        }

        @Test
        @DisplayName("Generic with qualified type")
        void genericQualifiedType() {
            UmlField field = UmlField.builder()
                    .name("map")
                    .type("java.util.Map<String, Object>")
                    .visibility(Visibility.PRIVATE)
                    .build();

            assertEquals("Map<String, Object>", field.getTypeSimpleName());
        }

        @Test
        @DisplayName("Primitive type unchanged")
        void primitiveType() {
            UmlField field = UmlField.builder()
                    .name("count")
                    .type("int")
                    .visibility(Visibility.PRIVATE)
                    .build();

            assertEquals("int", field.getTypeSimpleName());
        }
    }
}
