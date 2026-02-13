package no.ntnu.eitri.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConfigLoaderTest {

    @TempDir
    Path tempDir;

    @Test
    @DisplayName("Parses valid writers.plantuml configuration")
    void parsesValidWriterConfig() throws Exception {
        Path file = writeYaml("""
                writers:
                  plantuml:
                    diagramName: demo
                    direction: lr
                    groupInheritance: 3
                    classAttributeIconSize: 4
                    showNested: false
                    showLabels: false
                """);

        PlantUmlConfig plantUml = ConfigLoader.loadPlantUmlConfig(file);

        assertEquals("demo", plantUml.diagramName());
        assertEquals(LayoutDirection.LEFT_TO_RIGHT, plantUml.direction());
        assertEquals(3, plantUml.groupInheritance());
        assertEquals(4, plantUml.classAttributeIconSize());
        assertFalse(plantUml.showNested());
        assertFalse(plantUml.showLabels());
    }

    @Test
    @DisplayName("Rejects unknown top-level keys")
    void rejectsUnknownTopLevelKey() throws Exception {
        Path file = writeYaml("""
                output:
                  file: diagram.puml
                """);

        ConfigException ex = assertThrows(ConfigException.class, () -> ConfigLoader.loadPlantUmlConfig(file));
        assertTrue(ex.getMessage().contains("Unknown config key: root.output"));
    }

    @Test
    @DisplayName("Rejects unknown writer ids")
    void rejectsUnknownWriterId() throws Exception {
        Path file = writeYaml("""
                writers:
                  markdown:
                    showHeaders: true
                """);

        ConfigException ex = assertThrows(ConfigException.class, () -> ConfigLoader.loadPlantUmlConfig(file));
        assertTrue(ex.getMessage().contains("Unknown config key: writers.markdown"));
    }

    @Test
    @DisplayName("Rejects unknown plantuml keys")
    void rejectsUnknownPlantUmlKey() throws Exception {
        Path file = writeYaml("""
                writers:
                  plantuml:
                    unknownFlag: true
                """);

        ConfigException ex = assertThrows(ConfigException.class, () -> ConfigLoader.loadPlantUmlConfig(file));
        assertTrue(ex.getMessage().contains("Unknown config key: writers.plantuml.unknownFlag"));
    }

    @Test
    @DisplayName("Rejects type mismatch")
    void rejectsTypeMismatch() throws Exception {
        Path file = writeYaml("""
                writers:
                  plantuml:
                    showLabels: "yes"
                """);

        ConfigException ex = assertThrows(ConfigException.class, () -> ConfigLoader.loadPlantUmlConfig(file));
        assertTrue(ex.getMessage().contains("expected boolean"));
    }

      @Test
      @DisplayName("Rejects malformed YAML with config error")
      void rejectsMalformedYaml() throws Exception {
        Path file = writeYaml("""
            writers:
              plantuml:
              showLabels true
            """);

        ConfigException ex = assertThrows(ConfigException.class, () -> ConfigLoader.loadPlantUmlConfig(file));
        assertTrue(ex.getMessage().contains("Failed to parse config file"));
      }

    private Path writeYaml(String content) throws IOException {
        Path file = tempDir.resolve("config.yaml");
        Files.writeString(file, content);
        return file;
    }
}
