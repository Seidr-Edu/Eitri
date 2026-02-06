package no.ntnu.eitri.parser;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ParseExceptionTest {

    @Test
    void testMessageConstructor() {
        ParseException ex = new ParseException("Parse error");
        assertEquals("Parse error", ex.getMessage());
        assertNull(ex.getSourcePath());
        assertEquals(-1, ex.getLine());
        assertEquals(-1, ex.getColumn());
        assertFalse(ex.hasLocation());
    }

    @Test
    void testMessageAndCauseConstructor() {
        Throwable cause = new RuntimeException("Root cause");
        ParseException ex = new ParseException("Parse error", cause);
        assertEquals("Parse error", ex.getMessage());
        assertSame(cause, ex.getCause());
        assertNull(ex.getSourcePath());
        assertEquals(-1, ex.getLine());
        assertEquals(-1, ex.getColumn());
        assertFalse(ex.hasLocation());
    }

    @Test
    void testLocationConstructor() {
        ParseException ex = new ParseException("Parse error", "file.java", 10, 5);
        assertTrue(ex.getMessage().contains("file.java"));
        assertEquals("file.java", ex.getSourcePath());
        assertEquals(10, ex.getLine());
        assertEquals(5, ex.getColumn());
        assertTrue(ex.hasLocation());
    }

    @Test
    void testLocationAndCauseConstructor() {
        Throwable cause = new RuntimeException("Root cause");
        ParseException ex = new ParseException("Parse error", "file.java", 10, 5, cause);
        assertTrue(ex.getMessage().contains("file.java"));
        assertEquals("file.java", ex.getSourcePath());
        assertEquals(10, ex.getLine());
        assertEquals(5, ex.getColumn());
        assertSame(cause, ex.getCause());
        assertTrue(ex.hasLocation());
    }
}
