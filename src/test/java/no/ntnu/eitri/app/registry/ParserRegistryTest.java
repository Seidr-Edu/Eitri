package no.ntnu.eitri.app.registry;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Contract tests for ParserRegistry behavior.
 */
class ParserRegistryTest {

    @Test
    void supportsNormalizedExtensions() {
        ParserRegistry registry = ParserRegistry.defaultRegistry();

        assertTrue(registry.supports(".java"));
        assertTrue(registry.supports("java"));
        assertTrue(registry.supports(" JAVA "));
        assertFalse(registry.supports(".kt"));
    }

    @Test
    void defaultExtensionIsSupported() {
        ParserRegistry registry = ParserRegistry.defaultRegistry();

        String defaultExtension = registry.getDefaultExtension();
        assertNotNull(defaultExtension);
        assertTrue(registry.supports(defaultExtension));
    }

    @Test
    void getByExtensionReturnsParserWhenSupported() {
        ParserRegistry registry = ParserRegistry.defaultRegistry();

        assertTrue(registry.getByExtension(".java").isPresent());
        assertTrue(registry.getByExtension("java").isPresent());
        assertTrue(registry.getByExtension(".JAVA").isPresent());
        assertTrue(registry.getByExtension(".kt").isEmpty());
    }
}
