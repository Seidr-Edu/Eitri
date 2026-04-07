package no.ntnu.eitri.app;

import no.ntnu.eitri.model.UmlModel;

import java.nio.file.Path;

/**
 * Result of running Eitri.
 */
public record RunResult(
        int exitCode,
        RunFailureKind failureKind,
        String errorMessage,
        int typeCount,
        int relationCount,
        RepositoryStats repositoryStats,
        Path outputPath,
        boolean dryRun,
        UmlModel model
) {}
