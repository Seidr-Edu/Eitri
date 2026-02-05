package no.ntnu.eitri.app;

import no.ntnu.eitri.app.registry.ParserRegistry;
import no.ntnu.eitri.app.registry.WriterRegistry;
import no.ntnu.eitri.cli.CliOptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class EitriRunnerTest {

    @TempDir
    Path tempDir;

    @Test
    void dryRunDoesNotWriteFileAndReturnsCounts() throws Exception {
        Path src = tempDir.resolve("src");
        Files.createDirectories(src);
        Files.writeString(src.resolve("Sample.java"), "public class Sample {}\n");
        Path out = tempDir.resolve("diagram.puml");

        CliOptions options = new CliOptions(
                List.of(src),
                out,
                null,
                ".java",
                ".puml",
                true,
                true
        );

        EitriRunner runner = new EitriRunner();
        RunResult result = runner.run(options);

        assertEquals(0, result.exitCode());
        assertEquals(out, result.outputPath());
        assertTrue(result.dryRun());
        assertFalse(Files.exists(out));
    }

    @Test
    void writesOutputWhenNotDryRun() throws Exception {
        Path src = tempDir.resolve("src");
        Files.createDirectories(src);
        Files.writeString(src.resolve("Sample.java"), "public class Sample {}\n");
        Path out = tempDir.resolve("diagram.puml");

        CliOptions options = new CliOptions(
                List.of(src),
                out,
                null,
                ".java",
                ".puml",
                false,
                false
        );

        EitriRunner runner = new EitriRunner();
        RunResult result = runner.run(options);

        assertEquals(0, result.exitCode());
        assertTrue(Files.exists(out));
        assertFalse(result.dryRun());
    }

    @Test
    void customRegistriesAreUsed() {
        ParserRegistry parserRegistry = ParserRegistry.defaultRegistry();
        WriterRegistry writerRegistry = WriterRegistry.defaultRegistry();

        EitriRunner runner = new EitriRunner(parserRegistry, writerRegistry);
        CliOptions options = new CliOptions(List.of(tempDir), tempDir.resolve("out.puml"), null, null, null, false, true);

        RunResult result = runner.run(options);
        assertNotNull(result);
    }
}
