package no.ntnu.eitri.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConfigLoaderMappingTest {

    @TempDir
    Path tempDir;

    @Test
    void mapsPlantUmlWriterSettings() throws Exception {
        Path configFile = tempDir.resolve("config.yaml");
        Files.writeString(configFile, """
                writers:
                  plantuml:
                    diagramName: demo
                    direction: left-to-right
                    groupInheritance: 3
                    classAttributeIconSize: 4
                    showNotes: true
                    hidePrivate: true
                """);

        PlantUmlConfig plantUml = ConfigLoader.loadPlantUmlConfig(configFile);

        assertEquals("demo", plantUml.diagramName());
        assertEquals(LayoutDirection.LEFT_TO_RIGHT, plantUml.direction());
        assertEquals(3, plantUml.groupInheritance());
        assertEquals(4, plantUml.classAttributeIconSize());
        assertTrue(plantUml.showNotes());
        assertTrue(plantUml.hidePrivate());
    }
}
