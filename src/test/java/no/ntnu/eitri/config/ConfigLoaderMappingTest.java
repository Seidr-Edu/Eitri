package no.ntnu.eitri.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ConfigLoaderMappingTest {

    @TempDir
    Path tempDir;

    @Test
    void mapsInputOutputAndLayoutSections() throws Exception {
        Path configFile = tempDir.resolve("config.yaml");
        Path src = tempDir.resolve("src");
        Path out = tempDir.resolve("diagram.puml");
        java.nio.file.Files.createDirectories(src);

        String yaml = """
                input:
                  sources:
                    - %s
                  parserExtension: .java
                output:
                  file: %s
                  name: demo
                  writerExtension: .puml
                layout:
                  direction: lr
                  groupInheritance: 3
                  classAttributeIconSize: 4
                display:
                  showNotes: true
                """.formatted(src.toString(), out.toString());
        java.nio.file.Files.writeString(configFile, yaml);

        EitriConfig config = ConfigLoader.loadFromYaml(configFile);

        assertEquals(List.of(src), config.getSourcePaths());
        assertEquals(out, config.getOutputPath());
        assertEquals("demo", config.getDiagramName());
        assertEquals(LayoutDirection.LEFT_TO_RIGHT, config.getDirection());
        assertEquals(3, config.getGroupInheritance());
        assertEquals(4, config.getClassAttributeIconSize());
        assertTrue(config.isShowNotes());
    }
}
