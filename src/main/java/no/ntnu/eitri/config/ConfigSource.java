package no.ntnu.eitri.config;

import java.util.Optional;

/**
 * Source of configuration values.
 */
public interface ConfigSource {
    /**
     * Loads configuration values from this source.
     *
     * @return optional configuration values
     * @throws ConfigException if loading fails
     */
    Optional<EitriConfig> load() throws ConfigException;
}
