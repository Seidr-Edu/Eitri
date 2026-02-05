package no.ntnu.eitri.cli;

import java.nio.file.Path;
import java.util.List;

/**
 * Immutable container for CLI options passed to the application.
 */
public record CliOptions(List<Path> sourcePaths, Path outputPath, Path configPath, boolean verbose, boolean dryRun) {
    public CliOptions {
        sourcePaths = sourcePaths != null ? List.copyOf(sourcePaths) : List.of();
    }
}
