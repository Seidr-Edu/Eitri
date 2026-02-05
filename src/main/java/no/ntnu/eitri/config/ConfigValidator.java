package no.ntnu.eitri.config;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Validates configuration values and returns human-readable errors.
 */
public final class ConfigValidator {

    private ConfigValidator() {
        // Utility class
    }

    /**
     * Validates the configuration and returns a list of error messages.
     *
     * @param config the configuration to validate
     * @return list of error messages (empty if valid)
     */
    public static List<String> validate(EitriConfig config) {
        List<String> errors = new ArrayList<>();

        validateSourcePaths(config, errors);
        validateOutputPath(config, errors);

        return errors;
    }

    private static void validateSourcePaths(EitriConfig config, List<String> errors) {
        if (config.getSourcePaths().isEmpty()) {
            errors.add("At least one source path (--src) is required.");
            return;
        }

        for (Path src : config.getSourcePaths()) {
            if (!Files.exists(src)) {
                errors.add("Source path does not exist: " + src);
            } else if (!Files.isDirectory(src)) {
                errors.add("Source path is not a directory: " + src);
            }
        }
    }

    private static void validateOutputPath(EitriConfig config, List<String> errors) {
        if (config.getOutputPath() == null) {
            errors.add("Output path (--out) is required.");
            return;
        }

        Path parent = config.getOutputPath().getParent();
        if (parent != null && !Files.exists(parent)) {
            try {
                Files.createDirectories(parent);
            } catch (Exception _) {
                errors.add("Cannot create output directory: " + parent);
            }
        }
        if (parent != null && Files.exists(parent) && !Files.isWritable(parent)) {
            errors.add("Output directory is not writable: " + parent);
        }
    }
}
