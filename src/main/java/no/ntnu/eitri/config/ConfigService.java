package no.ntnu.eitri.config;

import no.ntnu.eitri.cli.CliOptions;

import java.nio.file.Path;
import java.util.ArrayList;
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

        List<ConfigSource> sources = new ArrayList<>();
        Path workingDirConfig = Path.of(System.getProperty("user.dir"), ConfigLoader.DEFAULT_CONFIG_FILENAME);
        if (configFileUsed != null && configFileUsed.equals(workingDirConfig)) {
            sources.add(new YamlConfigSource(workingDirConfig));
        }

        if (cliOptions.configPath() != null) {
            if (!java.nio.file.Files.exists(cliOptions.configPath())) {
                throw new ConfigException("Config file not found: " + cliOptions.configPath());
            }
            sources.add(new YamlConfigSource(cliOptions.configPath()));
        }

        sources.add(new CliConfigSource(cliOptions));

        ConfigMerger merger = new ConfigMerger();
        EitriConfig config = merger.merge(sources);

        ValidationResult validation = ConfigValidator.validate(config);
        if (!validation.isValid()) {
            throw new ConfigException(validation.formatMessages());
        }

        return new ConfigResolution(config, configFileUsed);
    }

    private Path resolveConfigFileUsed(Path explicitConfigPath) {
        if (explicitConfigPath != null) {
            return explicitConfigPath;
        }

        Path workingDirConfig = Path.of(System.getProperty("user.dir"), ConfigLoader.DEFAULT_CONFIG_FILENAME);
        if (java.nio.file.Files.exists(workingDirConfig)) {
            return workingDirConfig;
        }

        return null;
    }
}
