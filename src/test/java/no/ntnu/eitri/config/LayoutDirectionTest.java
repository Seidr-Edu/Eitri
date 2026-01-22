package no.ntnu.eitri.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for LayoutDirection enum.
 */
class LayoutDirectionTest {

    @Test
    @DisplayName("TOP_TO_BOTTOM renders correct PlantUML directive")
    void topToBottomDirective() {
        assertEquals("top to bottom direction", LayoutDirection.TOP_TO_BOTTOM.toPlantUml());
    }

    @Test
    @DisplayName("LEFT_TO_RIGHT renders correct PlantUML directive")
    void leftToRightDirective() {
        assertEquals("left to right direction", LayoutDirection.LEFT_TO_RIGHT.toPlantUml());
    }

    @Test
    @DisplayName("fromString parses 'lr' as LEFT_TO_RIGHT")
    void parseLr() {
        assertEquals(LayoutDirection.LEFT_TO_RIGHT, LayoutDirection.fromString("lr"));
    }

    @Test
    @DisplayName("fromString parses 'left-to-right' as LEFT_TO_RIGHT")
    void parseLeftToRight() {
        assertEquals(LayoutDirection.LEFT_TO_RIGHT, LayoutDirection.fromString("left-to-right"));
    }

    @Test
    @DisplayName("fromString parses 'tb' as TOP_TO_BOTTOM")
    void parseTb() {
        assertEquals(LayoutDirection.TOP_TO_BOTTOM, LayoutDirection.fromString("tb"));
    }

    @Test
    @DisplayName("fromString parses 'top-to-bottom' as TOP_TO_BOTTOM")
    void parseTopToBottom() {
        assertEquals(LayoutDirection.TOP_TO_BOTTOM, LayoutDirection.fromString("top-to-bottom"));
    }

    @Test
    @DisplayName("fromString defaults to TOP_TO_BOTTOM for null")
    void parseNull() {
        assertEquals(LayoutDirection.TOP_TO_BOTTOM, LayoutDirection.fromString(null));
    }

    @Test
    @DisplayName("fromString defaults to TOP_TO_BOTTOM for unknown value")
    void parseUnknown() {
        assertEquals(LayoutDirection.TOP_TO_BOTTOM, LayoutDirection.fromString("unknown"));
    }

    @Test
    @DisplayName("fromString is case-insensitive")
    void parseCaseInsensitive() {
        assertEquals(LayoutDirection.LEFT_TO_RIGHT, LayoutDirection.fromString("LR"));
        assertEquals(LayoutDirection.LEFT_TO_RIGHT, LayoutDirection.fromString("Left-To-Right"));
    }
}
