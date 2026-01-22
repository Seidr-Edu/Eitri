package no.ntnu.eitri.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for ConfigLoader.
 */
class ConfigLoaderTest {

    @TempDir
    Path tempDir;

    @Nested
    @DisplayName("YAML parsing")
    class YamlParsing {

        @Test
        @DisplayName("Parses layout direction from YAML")
        void parseLayoutDirection() throws IOException, ConfigException {
            String yaml = """
                layout:
                  direction: left-to-right
                  groupInheritance: 3
                """;
            Path configFile = writeYaml(yaml);

            EitriConfig config = ConfigLoader.loadFromYaml(configFile);

            assertEquals(LayoutDirection.LEFT_TO_RIGHT, config.getDirection());
            assertEquals(3, config.getGroupInheritance());
        }

        @Test
        @DisplayName("Parses visibility options from YAML")
        void parseVisibility() throws IOException, ConfigException {
            String yaml = """
                visibility:
                  hidePrivate: true
                  hideProtected: true
                  hidePackage: false
                """;
            Path configFile = writeYaml(yaml);

            EitriConfig config = ConfigLoader.loadFromYaml(configFile);

            assertTrue(config.isHidePrivate());
            assertTrue(config.isHideProtected());
            assertFalse(config.isHidePackage());
        }

        @Test
        @DisplayName("Parses members options from YAML")
        void parseMembers() throws IOException, ConfigException {
            String yaml = """
                members:
                  hideFields: true
                  hideMethods: false
                  hideEmptyMembers: true
                """;
            Path configFile = writeYaml(yaml);

            EitriConfig config = ConfigLoader.loadFromYaml(configFile);

            assertTrue(config.isHideFields());
            assertFalse(config.isHideMethods());
            assertTrue(config.isHideEmptyMembers());
        }

        @Test
        @DisplayName("Parses display options from YAML")
        void parseDisplay() throws IOException, ConfigException {
            String yaml = """
                display:
                  hideCircle: true
                  showStereotypes: false
                  showGenerics: false
                  showNotes: true
                """;
            Path configFile = writeYaml(yaml);

            EitriConfig config = ConfigLoader.loadFromYaml(configFile);

            assertTrue(config.isHideCircle());
            assertFalse(config.isShowStereotypes());
            assertFalse(config.isShowGenerics());
            assertTrue(config.isShowNotes());
        }

        @Test
        @DisplayName("Parses relations options from YAML")
        void parseRelations() throws IOException, ConfigException {
            String yaml = """
                relations:
                  showInheritance: true
                  showImplements: true
                  showComposition: false
                  showAggregation: false
                  showAssociation: true
                  showDependency: false
                """;
            Path configFile = writeYaml(yaml);

            EitriConfig config = ConfigLoader.loadFromYaml(configFile);

            assertTrue(config.isShowInheritance());
            assertTrue(config.isShowImplements());
            assertFalse(config.isShowComposition());
            assertFalse(config.isShowAggregation());
            assertTrue(config.isShowAssociation());
            assertFalse(config.isShowDependency());
        }

        @Test
        @DisplayName("Parses input sources from YAML")
        void parseInputSources() throws IOException, ConfigException {
            String yaml = """
                input:
                  sources:
                    - src/main/java
                    - src/test/java
                """;
            Path configFile = writeYaml(yaml);

            EitriConfig config = ConfigLoader.loadFromYaml(configFile);

            assertEquals(2, config.getSourcePaths().size());
            assertEquals(Path.of("src/main/java"), config.getSourcePaths().get(0));
            assertEquals(Path.of("src/test/java"), config.getSourcePaths().get(1));
        }

        @Test
        @DisplayName("Parses output options from YAML")
        void parseOutput() throws IOException, ConfigException {
            String yaml = """
                output:
                  file: my-diagram.puml
                  name: my-project
                """;
            Path configFile = writeYaml(yaml);

            EitriConfig config = ConfigLoader.loadFromYaml(configFile);

            assertEquals(Path.of("my-diagram.puml"), config.getOutputPath());
            assertEquals("my-project", config.getDiagramName());
        }

        @Test
        @DisplayName("Empty YAML returns default config")
        void emptyYaml() throws IOException, ConfigException {
            Path configFile = writeYaml("");

            EitriConfig config = ConfigLoader.loadFromYaml(configFile);

            assertNotNull(config);
            assertEquals(LayoutDirection.TOP_TO_BOTTOM, config.getDirection());
        }

        @Test
        @DisplayName("Throws ConfigException for non-existent file")
        void nonExistentFile() {
            Path nonExistent = tempDir.resolve("does-not-exist.yaml");

            assertThrows(ConfigException.class, () ->
                    ConfigLoader.loadFromYaml(nonExistent));
        }
    }

    @Nested
    @DisplayName("Config loading and merging")
    class ConfigLoadingAndMerging {

        @Test
        @DisplayName("load returns default config when no config files exist")
        void loadDefaultConfig() throws ConfigException {
            // No .eitri.config.yaml in temp dir, no explicit config
            EitriConfig config = ConfigLoader.load(null);

            assertNotNull(config);
            assertEquals(LayoutDirection.TOP_TO_BOTTOM, config.getDirection());
        }

        @Test
        @DisplayName("load applies explicit config file")
        void loadExplicitConfig() throws IOException, ConfigException {
            String yaml = """
                layout:
                  direction: left-to-right
                visibility:
                  hidePrivate: true
                """;
            Path configFile = writeYaml(yaml);

            EitriConfig config = ConfigLoader.load(configFile);

            assertEquals(LayoutDirection.LEFT_TO_RIGHT, config.getDirection());
            assertTrue(config.isHidePrivate());
        }

        @Test
        @DisplayName("Throws ConfigException for missing explicit config")
        void missingExplicitConfig() {
            Path nonExistent = tempDir.resolve("missing.yaml");

            ConfigException ex = assertThrows(ConfigException.class, () ->
                    ConfigLoader.load(nonExistent));

            assertTrue(ex.getMessage().contains("not found"));
        }
    }

    @Nested
    @DisplayName("Complete config file parsing")
    class CompleteConfigParsing {

        @Test
        @DisplayName("Parses a complete configuration file")
        void parseCompleteConfig() throws IOException, ConfigException {
            String yaml = """
                input:
                  sources:
                    - src/main/java

                output:
                  file: diagram.puml
                  name: my-diagram

                layout:
                  direction: left-to-right
                  groupInheritance: 2

                visibility:
                  hidePrivate: true

                members:
                  hideEmptyMembers: true

                display:
                  hideCircle: true
                  showStereotypes: true
                  showGenerics: true
                  showNotes: false

                relations:
                  showDependency: false
                """;
            Path configFile = writeYaml(yaml);

            EitriConfig config = ConfigLoader.loadFromYaml(configFile);

            // Verify all sections parsed correctly
            assertEquals(1, config.getSourcePaths().size());
            assertEquals(Path.of("diagram.puml"), config.getOutputPath());
            assertEquals("my-diagram", config.getDiagramName());
            assertEquals(LayoutDirection.LEFT_TO_RIGHT, config.getDirection());
            assertEquals(2, config.getGroupInheritance());
            assertTrue(config.isHidePrivate());
            assertTrue(config.isHideEmptyMembers());
            assertTrue(config.isHideCircle());
            assertTrue(config.isShowStereotypes());
            assertFalse(config.isShowDependency());
        }
    }

    private Path writeYaml(String content) throws IOException {
        Path file = tempDir.resolve("test-config.yaml");
        Files.writeString(file, content);
        return file;
    }
}
