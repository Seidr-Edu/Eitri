package no.ntnu.eitri.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class YamlConfigSourceTest {

    @TempDir
    Path tempDir;

    @Test
    void returnsEmptyWhenFileMissing() throws Exception {
        Path missing = tempDir.resolve("missing.yaml");
        YamlConfigSource source = new YamlConfigSource(missing);

        assertTrue(source.load().isEmpty());
    }
}
