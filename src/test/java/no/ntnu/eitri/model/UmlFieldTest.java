package no.ntnu.eitri.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for UmlField and its PlantUML rendering.
 */
class UmlFieldTest {

    @Nested
    @DisplayName("PlantUML rendering")
    class PlantUmlRendering {

        @Test
        @DisplayName("Simple public field")
        void publicField() {
            UmlField field = UmlField.builder()
                    .name("name")
                    .type("String")
                    .visibility(Visibility.PUBLIC)
                    .build();

            assertEquals("+name : String", field.toPlantUml());
        }

        @Test
        @DisplayName("Private field")
        void privateField() {
            UmlField field = UmlField.builder()
                    .name("count")
                    .type("int")
                    .visibility(Visibility.PRIVATE)
                    .build();

            assertEquals("-count : int", field.toPlantUml());
        }

        @Test
        @DisplayName("Protected field")
        void protectedField() {
            UmlField field = UmlField.builder()
                    .name("data")
                    .type("byte[]")
                    .visibility(Visibility.PROTECTED)
                    .build();

            assertEquals("#data : byte[]", field.toPlantUml());
        }

        @Test
        @DisplayName("Package-private field")
        void packageField() {
            UmlField field = UmlField.builder()
                    .name("helper")
                    .type("Helper")
                    .visibility(Visibility.PACKAGE)
                    .build();

            assertEquals("~helper : Helper", field.toPlantUml());
        }

        @Test
        @DisplayName("Static field")
        void staticField() {
            UmlField field = UmlField.builder()
                    .name("instance")
                    .type("Singleton")
                    .visibility(Visibility.PRIVATE)
                    .modifiers(Modifier.of(Modifier.STATIC))
                    .build();

            assertEquals("-{static} instance : Singleton", field.toPlantUml());
        }

        @Test
        @DisplayName("ReadOnly field")
        void readOnlyField() {
            UmlField field = UmlField.builder()
                    .name("id")
                    .type("Long")
                    .visibility(Visibility.PUBLIC)
                    .readOnly(true)
                    .build();

            assertEquals("+id : Long {readOnly}", field.toPlantUml());
        }

        @Test
        @DisplayName("Static final field shows both modifiers")
        void staticFinalField() {
            UmlField field = UmlField.builder()
                    .name("MAX_SIZE")
                    .type("int")
                    .visibility(Visibility.PUBLIC)
                    .modifiers(Modifier.of(Modifier.STATIC, Modifier.FINAL))
                    .readOnly(true)
                    .build();

            // Static shows, final doesn't have PlantUML keyword, but readOnly does
            String result = field.toPlantUml();
            assertTrue(result.contains("{static}"));
            assertTrue(result.contains("{readOnly}"));
        }

        @Test
        @DisplayName("Generic field type simplifies")
        void genericFieldType() {
            UmlField field = UmlField.builder()
                    .name("items")
                    .type("java.util.List<String>")
                    .visibility(Visibility.PRIVATE)
                    .build();

            assertEquals("-items : List<String>", field.toPlantUml());
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
