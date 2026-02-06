package no.ntnu.eitri.writer;

import org.junit.jupiter.api.Test;
import java.nio.file.Path;
import static org.junit.jupiter.api.Assertions.*;

class WriteExceptionTest {

    @Test
    void testMessageConstructor() {
        WriteException ex = new WriteException("Write error");
        assertEquals("Write error", ex.getMessage());
        assertNull(ex.getOutputPath());
        assertFalse(ex.hasOutputPath());
    }

    @Test
    void testMessageAndCauseConstructor() {
        Throwable cause = new RuntimeException("Root cause");
        WriteException ex = new WriteException("Write error", cause);
        assertEquals("Write error", ex.getMessage());
        assertSame(cause, ex.getCause());
        assertNull(ex.getOutputPath());
        assertFalse(ex.hasOutputPath());
    }

    @Test
    void testOutputPathConstructor() {
        Path path = Path.of("output.txt");
        WriteException ex = new WriteException("Write error", path);
        assertTrue(ex.getMessage().contains("output.txt"));
        assertEquals(path, ex.getOutputPath());
        assertTrue(ex.hasOutputPath());
    }

    @Test
    void testOutputPathAndCauseConstructor() {
        Path path = Path.of("output.txt");
        Throwable cause = new RuntimeException("Root cause");
        WriteException ex = new WriteException("Write error", path, cause);
        assertTrue(ex.getMessage().contains("output.txt"));
        assertEquals(path, ex.getOutputPath());
        assertSame(cause, ex.getCause());
        assertTrue(ex.hasOutputPath());
    }
}
