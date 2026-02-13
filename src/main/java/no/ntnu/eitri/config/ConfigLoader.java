package no.ntnu.eitri.config;

import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

/**
 * Strict YAML loader for writer-specific configuration.
 */
public final class ConfigLoader {

    public static final String DEFAULT_CONFIG_FILENAME = ".eitri.config.yaml";

    private ConfigLoader() {
    }

    /**
     * Loads {@code writers.plantuml} and binds it into {@link PlantUmlConfig}.
     * Missing sections return defaults.
     */
    public static PlantUmlConfig loadPlantUmlConfig(Path configPath) throws ConfigException {
        Map<String, Object> root = parseYaml(configPath);
        validateTopLevelKeys(root);

        Object writersNode = root.get("writers");
        if (writersNode == null) {
            return PlantUmlConfig.defaults();
        }

        Map<String, Object> writers = requireMap(writersNode, "writers");
        validateWriterIds(writers);

        Object plantUmlNode = writers.get("plantuml");
        if (plantUmlNode == null) {
            return PlantUmlConfig.defaults();
        }

        Map<String, Object> plantUml = requireMap(plantUmlNode, "writers.plantuml");
        return RecordBinder.bindFlatRecord(
                plantUml,
                PlantUmlConfig.class,
                PlantUmlConfig.defaults(),
                "writers.plantuml");
    }

    private static Map<String, Object> parseYaml(Path configPath) throws ConfigException {
        try (InputStream in = Files.newInputStream(configPath)) {
            Yaml yaml = new Yaml();
            Object loaded = yaml.load(in);
            if (loaded == null) {
                return Map.of();
            }
            if (!(loaded instanceof Map<?, ?> map)) {
                throw new ConfigException("Config root must be a mapping/object");
            }
            return castStringObjectMap(map, "root");
        } catch (IOException e) {
            throw new ConfigException("Failed to read config file: " + configPath, e);
        }
    }

    private static void validateTopLevelKeys(Map<String, Object> root) throws ConfigException {
        for (String key : root.keySet()) {
            if (!"writers".equals(key)) {
                throw new ConfigException("Unknown config key: root." + key);
            }
        }
    }

    private static void validateWriterIds(Map<String, Object> writers) throws ConfigException {
        for (String writerId : writers.keySet()) {
            if (!"plantuml".equals(writerId)) {
                throw new ConfigException("Unknown config key: writers." + writerId);
            }
        }
    }

    private static Map<String, Object> requireMap(Object value, String path) throws ConfigException {
        if (!(value instanceof Map<?, ?> map)) {
            throw new ConfigException("Expected mapping/object at " + path);
        }
        return castStringObjectMap(map, path);
    }

    private static Map<String, Object> castStringObjectMap(Map<?, ?> map, String path) throws ConfigException {
        for (Object key : map.keySet()) {
            if (!(key instanceof String)) {
                throw new ConfigException("Expected string keys at " + path);
            }
        }
        @SuppressWarnings("unchecked")
        Map<String, Object> cast = (Map<String, Object>) map;
        return cast;
    }
}
