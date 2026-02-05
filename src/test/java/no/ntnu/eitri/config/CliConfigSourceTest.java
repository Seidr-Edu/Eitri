package no.ntnu.eitri.config;

import no.ntnu.eitri.cli.CliOptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CliConfigSourceTest {

    @TempDir
    Path tempDir;

    @Test
    void mapsCliOptionsToConfig() {
        Path src = tempDir.resolve("src");
        Path out = tempDir.resolve("out.puml");

        CliOptions options = new CliOptions(
                List.of(src),
                out,
                null,
                ".java",
                ".puml",
                true,
                true
        );

        CliConfigSource source = new CliConfigSource(options);
        EitriConfig config = source.load().orElseThrow();

        assertEquals(List.of(src), config.getSourcePaths());
        assertEquals(out, config.getOutputPath());
        assertEquals(".java", config.getParserExtension());
        assertEquals(".puml", config.getWriterExtension());
        assertTrue(config.isVerbose());
        assertTrue(config.isDryRun());
    }
}
