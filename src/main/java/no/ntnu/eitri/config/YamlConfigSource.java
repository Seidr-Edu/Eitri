package no.ntnu.eitri.config;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

/**
 * Loads configuration from a YAML file.
 */
public final class YamlConfigSource implements ConfigSource {

    private final Path path;

    public YamlConfigSource(Path path) {
        this.path = path;
    }

    @Override
    public Optional<EitriConfig> load() throws ConfigException {
        if (path == null || !Files.exists(path)) {
            return Optional.empty();
        }
        return Optional.of(ConfigLoader.loadFromYaml(path));
    }
}
