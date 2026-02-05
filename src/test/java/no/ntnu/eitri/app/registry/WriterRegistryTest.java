package no.ntnu.eitri.app.registry;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Contract tests for WriterRegistry behavior.
 */
class WriterRegistryTest {

    @Test
    void supportsNormalizedExtensions() {
        WriterRegistry registry = WriterRegistry.defaultRegistry();

        assertTrue(registry.supports(".puml"));
        assertTrue(registry.supports("puml"));
        assertTrue(registry.supports(" PUMl "));
        assertFalse(registry.supports(".md"));
    }

    @Test
    void defaultExtensionIsSupported() {
        WriterRegistry registry = WriterRegistry.defaultRegistry();

        String defaultExtension = registry.getDefaultExtension();
        assertNotNull(defaultExtension);
        assertTrue(registry.supports(defaultExtension));
    }

    @Test
    void getByExtensionReturnsWriterWhenSupported() {
        WriterRegistry registry = WriterRegistry.defaultRegistry();

        assertTrue(registry.getByExtension(".puml").isPresent());
        assertTrue(registry.getByExtension("puml").isPresent());
        assertTrue(registry.getByExtension(".PUMl").isPresent());
        assertTrue(registry.getByExtension(".md").isEmpty());
    }
}
