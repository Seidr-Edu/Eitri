package no.ntnu.eitri.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for Visibility enum and its PlantUML rendering.
 */
class VisibilityTest {

    @Test
    @DisplayName("PUBLIC renders as +")
    void publicRendersAsPlus() {
        assertEquals("+", Visibility.PUBLIC.toPlantUml());
    }

    @Test
    @DisplayName("PRIVATE renders as -")
    void privateRendersAsMinus() {
        assertEquals("-", Visibility.PRIVATE.toPlantUml());
    }

    @Test
    @DisplayName("PROTECTED renders as #")
    void protectedRendersAsHash() {
        assertEquals("#", Visibility.PROTECTED.toPlantUml());
    }

    @Test
    @DisplayName("PACKAGE renders as ~")
    void packageRendersAsTilde() {
        assertEquals("~", Visibility.PACKAGE.toPlantUml());
    }

    @Test
    @DisplayName("All visibility symbols are single characters")
    void allSymbolsAreSingleChar() {
        for (Visibility v : Visibility.values()) {
            assertEquals(1, v.toPlantUml().length(),
                    "Visibility " + v + " should have single-char symbol");
        }
    }
}
