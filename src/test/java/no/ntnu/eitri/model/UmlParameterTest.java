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
}
