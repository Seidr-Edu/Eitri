package no.ntnu.eitri.config;

import no.ntnu.eitri.cli.CliOptions;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Central service for resolving and validating configuration.
 */
public final class ConfigService {

    /**
     * Resolves configuration using defaults, optional config files, and CLI overrides.
     *
     * @param cliOptions CLI options
     * @return configuration resolution result
     * @throws ConfigException if resolution or validation fails
     */
    public ConfigResolution resolve(CliOptions cliOptions) throws ConfigException {
        Path configFileUsed = resolveConfigFileUsed(cliOptions.configPath());

        EitriConfig config = ConfigLoader.load(cliOptions.configPath());
        applyCliOverrides(config, cliOptions);

        List<String> errors = ConfigValidator.validate(config);
        if (!errors.isEmpty()) {
            throw new ConfigException(String.join("\n", errors));
        }

        return new ConfigResolution(config, configFileUsed);
    }

    private Path resolveConfigFileUsed(Path explicitConfigPath) {
        if (explicitConfigPath != null) {
            return explicitConfigPath;
        }

        Path workingDirConfig = Path.of(System.getProperty("user.dir"), ConfigLoader.DEFAULT_CONFIG_FILENAME);
        if (Files.exists(workingDirConfig)) {
            return workingDirConfig;
        }

        return null;
    }

    private void applyCliOverrides(EitriConfig config, CliOptions cliOptions) {
        for (Path src : cliOptions.sourcePaths()) {
            config.addSourcePath(src);
        }
        config.setOutputPath(cliOptions.outputPath());

        config.setVerbose(cliOptions.verbose());
        config.setDryRun(cliOptions.dryRun());
    }
}
