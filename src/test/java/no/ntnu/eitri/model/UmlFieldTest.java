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

        @Test
        @DisplayName("Nested generic types are simplified recursively")
        void nestedGenericType() {
            UmlField field = UmlField.builder()
                    .name("index")
                    .type("java.util.Map<java.lang.String, java.util.List<com.acme.Item>>")
                    .build();

            assertEquals("Map<String, List<Item>>", field.getTypeSimpleName());
        }
    }

    @Nested
    @DisplayName("Immutability and equality")
    class ImmutabilityAndEquality {

        @Test
        @DisplayName("Collections are immutable after build")
        void immutableCollections() {
            UmlField field = UmlField.builder()
                    .name("value")
                    .type("java.lang.String")
                    .addModifier(Modifier.FINAL)
                    .addAnnotation("NotNull")
                    .build();

            assertThrows(UnsupportedOperationException.class, () -> field.getModifiers().add(Modifier.STATIC));
            assertThrows(UnsupportedOperationException.class, () -> field.getAnnotations().add("Readonly"));
        }

        @Test
        @DisplayName("equals/hashCode depend on name and full type")
        void equalsAndHashCode() {
            UmlField a = UmlField.builder().name("value").type("java.lang.String").build();
            UmlField b = UmlField.builder().name("value").type("java.lang.String").readOnly(true).build();
            UmlField c = UmlField.builder().name("value").type("String").build();

            assertEquals(a, b);
            assertEquals(a.hashCode(), b.hashCode());
            assertNotEquals(a, c);
        }
    }
}
