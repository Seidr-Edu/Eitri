package no.ntnu.eitri.config;

import no.ntnu.eitri.cli.CliOptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ConfigMergerPrecedenceTest {

    @TempDir
    Path tempDir;

    @Test
    @SuppressWarnings("null")
    void cliOverridesYamlAndDefaults() throws Exception {
        Path yamlPath = tempDir.resolve("config.yaml");
        Path yamlOut = tempDir.resolve("from-yaml.puml");
        Path yamlSrc = tempDir.resolve("yaml-src");
        java.nio.file.Files.createDirectories(yamlSrc);

        String yaml = """
                input:
                  sources:
                    - %s
                  parserExtension: .java
                output:
                  file: %s
                  writerExtension: .puml
                runtime:
                  verbose: false
                """.formatted(yamlSrc.toString(), yamlOut.toString());
        java.nio.file.Files.writeString(yamlPath, yaml);

        Path cliOut = tempDir.resolve("from-cli.puml");
        Path cliSrc = tempDir.resolve("cli-src");
        java.nio.file.Files.createDirectories(cliSrc);

        CliOptions cliOptions = new CliOptions(
                List.of(cliSrc),
                cliOut,
                null,
                ".java",
                ".puml",
                true,
                false
        );

        ConfigMerger merger = new ConfigMerger();
        EitriConfig config = merger.merge(List.of(
                new YamlConfigSource(yamlPath),
                new CliConfigSource(cliOptions)
        ));

        assertEquals(cliOut, config.getOutputPath());
        assertEquals(List.of(yamlSrc, cliSrc), config.getSourcePaths());
        assertEquals(".java", config.getParserExtension());
        assertEquals(".puml", config.getWriterExtension());
        assertTrue(config.isVerbose());
    }

    @Test
    void defaultsAppliedWhenNoSources() throws Exception {
        ConfigMerger merger = new ConfigMerger();
        EitriConfig config = merger.merge(List.of());

        assertEquals("diagram", config.getDiagramName());
        assertEquals(LayoutDirection.TOP_TO_BOTTOM, config.getDirection());
        assertTrue(config.isHideEmptyMembers());
    }
}
