package no.ntnu.eitri.config;

import no.ntnu.eitri.cli.CliOptions;

import java.util.Optional;

/**
 * Converts CLI options into a partial configuration source.
 */
public final class CliConfigSource implements ConfigSource {

    private final CliOptions cliOptions;

    public CliConfigSource(CliOptions cliOptions) {
        this.cliOptions = cliOptions;
    }

    @Override
    public Optional<EitriConfig> load() {
        EitriConfig.Builder builder = EitriConfig.builder();

        for (var src : cliOptions.sourcePaths()) {
            builder.addSourcePath(src);
        }
        if (cliOptions.outputPath() != null) {
            builder.outputPath(cliOptions.outputPath());
        }

        if (cliOptions.parserExtension() != null) {
            builder.parserExtension(cliOptions.parserExtension());
        }
        if (cliOptions.writerExtension() != null) {
            builder.writerExtension(cliOptions.writerExtension());
        }

        builder.verbose(cliOptions.verbose());
        builder.dryRun(cliOptions.dryRun());

        return Optional.of(builder.build());
    }
}
