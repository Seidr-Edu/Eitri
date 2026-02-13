package no.ntnu.eitri.config;

import no.ntnu.eitri.cli.CliOptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConfigServiceTest {

    @TempDir
    Path tempDir;

    @Test
    void throwsWhenExplicitConfigMissing() {
        Path src = tempDir.resolve("src");
        Path out = tempDir.resolve("out.puml");
        CliOptions options = new CliOptions(
                List.of(src),
                out,
                tempDir.resolve("missing.yaml"),
                ".java",
                ".puml",
                false,
                false
        );

        ConfigException ex = assertThrows(ConfigException.class, () -> new ConfigService().resolve(options));
        assertTrue(ex.getMessage().contains("Config file not found"));
    }

    @Test
    void appliesWriterSettingsFromExplicitConfig() throws Exception {
        Path src = tempDir.resolve("src");
        Files.createDirectories(src);
        Path out = tempDir.resolve("out.puml");
        Path configFile = tempDir.resolve("config.yaml");
        Files.writeString(configFile, """
                writers:
                  plantuml:
                    diagramName: demo
                    showNested: false
                    showLabels: false
                """);

        CliOptions options = new CliOptions(
                List.of(src),
                out,
                configFile,
                ".java",
                ".puml",
                false,
                false
        );

        ConfigResolution resolution = new ConfigService().resolve(options);

        assertEquals(configFile, resolution.configFileUsed());
        assertEquals(out, resolution.runConfig().outputPath());
        assertEquals("demo", resolution.plantUmlConfig().diagramName());
        assertFalse(resolution.plantUmlConfig().showNested());
        assertFalse(resolution.plantUmlConfig().showLabels());
    }

    @Test
    void noConfigUsesDefaults() throws Exception {
        String previous = System.getProperty("user.dir");
        Path src = tempDir.resolve("src");
        Files.createDirectories(src);
        Path out = tempDir.resolve("out.puml");
        Path cleanDir = tempDir.resolve("clean");
        Files.createDirectories(cleanDir);
        try {
            System.setProperty("user.dir", cleanDir.toString());
            CliOptions options = new CliOptions(
                    List.of(src),
                    out,
                    null,
                    ".java",
                    ".puml",
                    false,
                    false
            );

            ConfigResolution resolution = new ConfigService().resolve(options);

            assertNull(resolution.configFileUsed());
            assertEquals("diagram", resolution.plantUmlConfig().diagramName());
            assertTrue(resolution.plantUmlConfig().showNested());
        } finally {
            System.setProperty("user.dir", previous);
        }
    }

    @Test
    void usesWorkingDirConfigWhenPresent() throws Exception {
        String previous = System.getProperty("user.dir");
        Path workDir = tempDir.resolve("work");
        Files.createDirectories(workDir);
        Path src = workDir.resolve("src");
        Files.createDirectories(src);
        Path out = workDir.resolve("out.puml");
        Path defaultConfig = workDir.resolve(ConfigLoader.DEFAULT_CONFIG_FILENAME);
        Files.writeString(defaultConfig, """
                writers:
                  plantuml:
                    diagramName: from-working-dir
                """);

        try {
            System.setProperty("user.dir", workDir.toString());
            CliOptions options = new CliOptions(
                    List.of(src),
                    out,
                    null,
                    ".java",
                    ".puml",
                    false,
                    false
            );

            ConfigResolution resolution = new ConfigService().resolve(options);
            assertEquals(defaultConfig, resolution.configFileUsed());
            assertEquals("from-working-dir", resolution.plantUmlConfig().diagramName());
        } finally {
            System.setProperty("user.dir", previous);
        }
    }
}
