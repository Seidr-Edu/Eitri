package no.ntnu.eitri.config;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Validates configuration values and returns human-readable errors.
 */
public final class ConfigValidator {

    private static final String FIELD_SOURCE_PATHS = "sourcePaths";
    private static final String FIELD_OUTPUT_PATH = "outputPath";

    private ConfigValidator() {
        // Utility class
    }

    /**
     * Validates the configuration and returns a list of error messages.
     *
     * @param config the configuration to validate
     * @return list of error messages (empty if valid)
     */
    public static ValidationResult validate(EitriConfig config) {
        ValidationResult result = new ValidationResult();

        validateSourcePaths(config, result);
        validateOutputPath(config, result);

        return result;
    }

    private static void validateSourcePaths(EitriConfig config, ValidationResult result) {
        if (config.getSourcePaths().isEmpty()) {
            result.add(ValidationError.error(
                    "SOURCE_PATHS_REQUIRED",
                    "At least one source path (--src) is required.",
                    FIELD_SOURCE_PATHS
            ));
            return;
        }

        for (Path src : config.getSourcePaths()) {
            if (!Files.exists(src)) {
                result.add(ValidationError.error(
                        "SOURCE_PATH_NOT_FOUND",
                        "Source path does not exist: " + src,
                        FIELD_SOURCE_PATHS
                ));
            } else if (!Files.isDirectory(src)) {
                result.add(ValidationError.error(
                        "SOURCE_PATH_NOT_DIRECTORY",
                        "Source path is not a directory: " + src,
                        FIELD_SOURCE_PATHS
                ));
            }
        }
    }

    private static void validateOutputPath(EitriConfig config, ValidationResult result) {
        if (config.getOutputPath() == null) {
            result.add(ValidationError.error(
                    "OUTPUT_PATH_REQUIRED",
                    "Output path (--out) is required.",
                    FIELD_OUTPUT_PATH
            ));
            return;
        }

        Path parent = config.getOutputPath().getParent();
        if (parent != null && !Files.exists(parent)) {
            try {
                Files.createDirectories(parent);
            } catch (Exception _) {
                result.add(ValidationError.error(
                        "OUTPUT_DIR_CREATE_FAILED",
                        "Cannot create output directory: " + parent,
                        FIELD_OUTPUT_PATH
                ));
            }
        }
        if (parent != null && Files.exists(parent) && !Files.isWritable(parent)) {
            result.add(ValidationError.error(
                    "OUTPUT_DIR_NOT_WRITABLE",
                    "Output directory is not writable: " + parent,
                    FIELD_OUTPUT_PATH
            ));
        }
    }
}
