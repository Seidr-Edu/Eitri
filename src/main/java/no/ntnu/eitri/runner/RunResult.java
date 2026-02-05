package no.ntnu.eitri.runner;

import java.nio.file.Path;

/**
 * Result of running Eitri.
 */
public record RunResult(
        int exitCode,
        String errorMessage,
        int typeCount,
        int relationCount,
        Path outputPath,
        boolean dryRun
) {}
