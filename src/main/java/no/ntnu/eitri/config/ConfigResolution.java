package no.ntnu.eitri.config;

import java.nio.file.Path;

/**
 * Result of configuration resolution.
 */
public record ConfigResolution(RunConfig runConfig, PlantUmlConfig plantUmlConfig, Path configFileUsed) {

    /**
     * Resolves writer configuration by the writer's declared config type.
     */
    public <C extends WriterConfig> C writerConfig(Class<C> configType) throws ConfigException {
        if (configType == PlantUmlConfig.class) {
            return configType.cast(plantUmlConfig);
        }
        throw new ConfigException("No configuration available for writer config type: " + configType.getName());
    }
}
