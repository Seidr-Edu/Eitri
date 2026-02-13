package no.ntnu.eitri.config;

import no.ntnu.eitri.util.ExtensionNormalizer;

import java.nio.file.Path;
import java.util.List;

/**
 * Core runtime configuration sourced from CLI.
 */
public record RunConfig(
        List<Path> sourcePaths,
        Path outputPath,
        String parserExtension,
        String writerExtension,
        boolean verbose,
        boolean dryRun) {

    public RunConfig {
        sourcePaths = sourcePaths != null ? List.copyOf(sourcePaths) : List.of();
        parserExtension = ExtensionNormalizer.normalizeExtension(parserExtension);
        writerExtension = ExtensionNormalizer.normalizeExtension(writerExtension);
    }

    public static RunConfig fromCli(no.ntnu.eitri.cli.CliOptions options) {
        return new RunConfig(
                options.sourcePaths(),
                options.outputPath(),
                options.parserExtension(),
                options.writerExtension(),
                options.verbose(),
                options.dryRun());
    }
}
