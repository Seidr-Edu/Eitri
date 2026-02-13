package no.ntnu.eitri.config;

import no.ntnu.eitri.cli.CliOptions;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

/**
 * Central service for resolving and validating configuration.
 */
public final class ConfigService {

    public ConfigResolution resolve(CliOptions cliOptions) throws ConfigException {
        Objects.requireNonNull(cliOptions, "cliOptions");

        RunConfig runConfig = RunConfig.fromCli(cliOptions);
        Path configFileUsed = resolveConfigFileUsed(cliOptions.configPath());
        PlantUmlConfig plantUmlConfig = configFileUsed == null
                ? PlantUmlConfig.defaults()
                : ConfigLoader.loadPlantUmlConfig(configFileUsed);

        ValidationResult validation = ConfigValidator.validate(runConfig);
        if (!validation.isValid()) {
            throw new ConfigException(validation.formatMessages());
        }

        return new ConfigResolution(runConfig, plantUmlConfig, configFileUsed);
    }

    private Path resolveConfigFileUsed(Path explicitConfigPath) throws ConfigException {
        if (explicitConfigPath != null) {
            if (!Files.exists(explicitConfigPath)) {
                throw new ConfigException("Config file not found: " + explicitConfigPath);
            }
            return explicitConfigPath;
        }

        Path workingDirConfig = Path.of(System.getProperty("user.dir"), ConfigLoader.DEFAULT_CONFIG_FILENAME);
        if (Files.exists(workingDirConfig)) {
            return workingDirConfig;
        }

        return null;
    }
}
