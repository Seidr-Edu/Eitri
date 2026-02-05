package no.ntnu.eitri.app;

import no.ntnu.eitri.cli.CliOptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class EitriRunnerErrorTest {

    @TempDir
    Path tempDir;

    @Test
    void returnsNonZeroOnInvalidSourcePath() {
        Path missing = tempDir.resolve("missing");
        Path out = tempDir.resolve("diagram.puml");

        CliOptions options = new CliOptions(
                List.of(missing),
                out,
                null,
                ".java",
                ".puml",
                false,
                false
        );

        EitriRunner runner = new EitriRunner();
        RunResult result = runner.run(options);

        assertNotEquals(0, result.exitCode());
        assertNotNull(result.errorMessage());
    }
}
