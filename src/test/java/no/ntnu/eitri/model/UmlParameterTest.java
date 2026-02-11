package no.ntnu.eitri.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class UmlParameterTest {

    @Test
    @DisplayName("Derives simple name from fully qualified type")
    void derivesSimpleName() {
        UmlParameter parameter = new UmlParameter("items", "java.util.List<String>");

        assertEquals("List<String>", parameter.typeSimpleName());
    }

    @Test
    @DisplayName("Uses provided simple name when specified")
    void usesProvidedSimpleName() {
        UmlParameter parameter = new UmlParameter("items", "java.util.List<String>", "List");

        assertEquals("List", parameter.typeSimpleName());
    }

    @Test
    @DisplayName("Null name throws exception")
    void nullNameThrows() {
        Exception exception = assertThrows(NullPointerException.class,
                () -> new UmlParameter(null, "String"));
        assertEquals("Parameter name cannot be null", exception.getMessage());
    }

    @Test
    @DisplayName("Null type throws exception")
    void nullTypeThrows() {
        Exception exception = assertThrows(NullPointerException.class,
                () -> new UmlParameter("name", null));
        assertEquals("Parameter type cannot be null", exception.getMessage());
    }

    @Test
    @DisplayName("Blank simple name falls back to derived simple name")
    void blankSimpleNameFallsBack() {
        UmlParameter parameter = new UmlParameter("items", "java.util.List<java.lang.Integer>", "   ");
        assertEquals("List<Integer>", parameter.typeSimpleName());
    }

    @Test
    @DisplayName("Nested generics are simplified recursively")
    void nestedGenericsSimplified() {
        UmlParameter parameter = new UmlParameter(
                "mapping",
                "java.util.Map<java.lang.String, java.util.List<com.acme.Item>>"
        );
        assertEquals("Map<String, List<Item>>", parameter.typeSimpleName());
    }
}
