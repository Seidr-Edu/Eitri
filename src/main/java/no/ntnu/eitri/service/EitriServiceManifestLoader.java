package no.ntnu.eitri.service;

import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;
import org.yaml.snakeyaml.error.YAMLException;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * Loads and validates the public service manifest.
 */
final class EitriServiceManifestLoader {

    private static final Set<String> ALLOWED_TOP_LEVEL_KEYS = Set.of(
            "version",
            "run_id",
            "source_relpaths",
            "parser_extension",
            "writer_extension",
            "verbose",
            "writers");

    private EitriServiceManifestLoader() {
    }

    static EitriServiceManifest load(Path manifestPath) throws EitriServiceManifestException {
        Map<String, Object> root = parseYaml(manifestPath);
        rejectUnknownTopLevelKeys(root);

        validateVersion(root.get("version"));

        String runId = readOptionalString(root, "run_id");
        List<String> sourceRelpaths = readRequiredStringList(root, "source_relpaths");
        String parserExtension = readOptionalString(root, "parser_extension");
        String writerExtension = readOptionalString(root, "writer_extension");
        boolean verbose = readOptionalBoolean(root, "verbose", false);
        Map<String, Object> writers = readOptionalMap(root, "writers");

        return new EitriServiceManifest(runId, sourceRelpaths, parserExtension, writerExtension, verbose, writers);
    }

    private static Map<String, Object> parseYaml(Path manifestPath) throws EitriServiceManifestException {
        try (InputStream in = Files.newInputStream(manifestPath)) {
            Object loaded = createManifestYaml().load(in);
            if (loaded == null) {
                return Map.of();
            }
            if (!(loaded instanceof Map<?, ?> map)) {
                throw new EitriServiceManifestException(
                        "invalid-manifest",
                        "Manifest root must be a mapping/object.");
            }
            return normalizeStringObjectMap(map, "manifest");
        } catch (java.nio.file.NoSuchFileException e) {
            throw new EitriServiceManifestException("missing-manifest", "Manifest file not found: " + manifestPath, e);
        } catch (YAMLException e) {
            throw new EitriServiceManifestException("invalid-manifest", "Failed to parse manifest YAML.", e);
        } catch (IOException e) {
            throw new EitriServiceManifestException("invalid-manifest", "Failed to read manifest: " + manifestPath, e);
        }
    }

    private static void rejectUnknownTopLevelKeys(Map<String, Object> root) throws EitriServiceManifestException {
        Set<String> unknown = new TreeSet<>(root.keySet());
        unknown.removeAll(ALLOWED_TOP_LEVEL_KEYS);
        if (!unknown.isEmpty()) {
            throw new EitriServiceManifestException(
                    "unknown-manifest-key",
                    "Unknown manifest key(s): " + String.join(", ", unknown));
        }
    }

    private static void validateVersion(Object rawVersion) throws EitriServiceManifestException {
        if (rawVersion == null) {
            throw new EitriServiceManifestException("invalid-manifest", "Manifest field 'version' is required.");
        }

        if (isSupportedVersionNumber(rawVersion)) {
            return;
        }
        if (rawVersion instanceof String stringValue && "1".equals(stringValue.trim())) {
            return;
        }

        throw new EitriServiceManifestException(
                "unsupported-manifest-version",
                "Unsupported manifest version: " + rawVersion);
    }

    private static String readOptionalString(Map<String, Object> root, String key) throws EitriServiceManifestException {
        Object rawValue = root.get(key);
        if (rawValue == null) {
            return null;
        }
        if (!(rawValue instanceof String stringValue)) {
            throw new EitriServiceManifestException(
                    "invalid-manifest",
                    "Manifest field '" + key + "' must be a string.");
        }
        String trimmedValue = stringValue.trim();
        if (trimmedValue.isBlank()) {
            throw new EitriServiceManifestException(
                    "invalid-manifest",
                    "Manifest field '" + key + "' must not be blank.");
        }
        return trimmedValue;
    }

    private static boolean readOptionalBoolean(
            Map<String, Object> root, String key, boolean defaultValue) throws EitriServiceManifestException {
        Object rawValue = root.get(key);
        if (rawValue == null) {
            return defaultValue;
        }
        if (!(rawValue instanceof Boolean boolValue)) {
            throw new EitriServiceManifestException(
                    "invalid-manifest",
                    "Manifest field '" + key + "' must be a boolean.");
        }
        return boolValue;
    }

    private static List<String> readRequiredStringList(Map<String, Object> root, String key)
            throws EitriServiceManifestException {
        Object rawValue = root.get(key);
        if (rawValue == null) {
            throw new EitriServiceManifestException(
                    "invalid-manifest",
                    "Manifest field '" + key + "' is required.");
        }
        if (!(rawValue instanceof List<?> items)) {
            throw new EitriServiceManifestException(
                    "invalid-manifest",
                    "Manifest field '" + key + "' must be an array of strings.");
        }
        if (items.isEmpty()) {
            throw new EitriServiceManifestException(
                    "invalid-manifest",
                    "Manifest field '" + key + "' must not be empty.");
        }

        List<String> values = new ArrayList<>(items.size());
        for (Object item : items) {
            if (!(item instanceof String stringValue)) {
                throw new EitriServiceManifestException(
                        "invalid-manifest",
                        "Manifest field '" + key + "' must contain only non-empty strings.");
            }
            String trimmedValue = stringValue.trim();
            if (trimmedValue.isBlank()) {
                throw new EitriServiceManifestException(
                        "invalid-manifest",
                        "Manifest field '" + key + "' must contain only non-empty strings.");
            }
            values.add(trimmedValue);
        }
        return values;
    }

    private static Map<String, Object> readOptionalMap(Map<String, Object> root, String key)
            throws EitriServiceManifestException {
        Object rawValue = root.get(key);
        if (rawValue == null) {
            return Map.of();
        }
        if (!(rawValue instanceof Map<?, ?> mapValue)) {
            throw new EitriServiceManifestException(
                    "invalid-manifest",
                    "Manifest field '" + key + "' must be a mapping/object.");
        }
        return normalizeStringObjectMap(mapValue, key);
    }

    private static Map<String, Object> normalizeStringObjectMap(Map<?, ?> rawMap, String path)
            throws EitriServiceManifestException {
        Map<String, Object> normalized = new LinkedHashMap<>();
        for (Map.Entry<?, ?> entry : rawMap.entrySet()) {
            if (!(entry.getKey() instanceof String stringKey)) {
                throw new EitriServiceManifestException(
                        "invalid-manifest",
                        "Expected string keys at " + path + ".");
            }
            normalized.put(stringKey, normalizeYamlValue(entry.getValue(), path + "." + stringKey));
        }
        return normalized;
    }

    private static Object normalizeYamlValue(Object value, String path) throws EitriServiceManifestException {
        if (value instanceof Map<?, ?> mapValue) {
            return normalizeStringObjectMap(mapValue, path);
        }
        if (value instanceof List<?> listValue) {
            List<Object> normalized = new ArrayList<>(listValue.size());
            for (int i = 0; i < listValue.size(); i++) {
                normalized.add(normalizeYamlValue(listValue.get(i), path + "[" + i + "]"));
            }
            return normalized;
        }
        return value;
    }

    private static Yaml createManifestYaml() {
        LoaderOptions loaderOptions = new LoaderOptions();
        loaderOptions.setAllowDuplicateKeys(false);
        loaderOptions.setWarnOnDuplicateKeys(false);
        return new Yaml(new SafeConstructor(loaderOptions));
    }

    private static boolean isSupportedVersionNumber(Object rawVersion) {
        return switch (rawVersion) {
            case Byte number -> number == 1;
            case Short number -> number == 1;
            case Integer number -> number == 1;
            case Long number -> number == 1L;
            case BigInteger number -> BigInteger.ONE.equals(number);
            default -> false;
        };
    }
}
