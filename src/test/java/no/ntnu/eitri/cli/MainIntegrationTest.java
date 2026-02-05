package no.ntnu.eitri.cli;

import no.ntnu.eitri.Main;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import picocli.CommandLine;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class MainIntegrationTest {

    @TempDir
    Path tempDir;

    @Test
    void returnsZeroOnSuccessfulDryRun() throws Exception {
        Path src = tempDir.resolve("src");
        Files.createDirectories(src);
        Files.writeString(src.resolve("Sample.java"), "public class Sample {}\n");

        Path out = tempDir.resolve("diagram.puml");

        int exitCode = new CommandLine(new Main()).execute(
                "--src", src.toString(),
                "--out", out.toString(),
                "--dry-run"
        );

        assertEquals(0, exitCode);
    }

    @Test
    void returnsNonZeroOnInvalidSourcePath() {
        Path missing = tempDir.resolve("missing");
        Path out = tempDir.resolve("diagram.puml");

        int exitCode = new CommandLine(new Main()).execute(
                "--src", missing.toString(),
                "--out", out.toString()
        );

        assertNotEquals(0, exitCode);
    }
}
