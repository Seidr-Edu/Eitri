package no.ntnu.eitri.cli;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ManifestVersionProviderTest {

    @Test
    void testGetVersionReturnsStringArray() {
        ManifestVersionProvider provider = new ManifestVersionProvider();
        String[] version = provider.getVersion();
        assertNotNull(version);
        assertEquals(1, version.length);
        assertNotNull(version[0]);
        assertFalse(version[0].isEmpty());
    }
}
