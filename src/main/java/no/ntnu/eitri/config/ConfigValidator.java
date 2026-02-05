package no.ntnu.eitri.config;

import no.ntnu.eitri.parser.ParserRegistry;
import no.ntnu.eitri.writer.WriterRegistry;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Validates configuration values and returns human-readable errors.
 */
public final class ConfigValidator {

    private static final String FIELD_SOURCE_PATHS = "sourcePaths";
    private static final String FIELD_OUTPUT_PATH = "outputPath";
    private static final String FIELD_PARSER_EXTENSION = "parserExtension";
    private static final String FIELD_WRITER_EXTENSION = "writerExtension";

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
        validateParserExtension(config, result);
        validateWriterExtension(config, result);

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

    private static void validateParserExtension(EitriConfig config, ValidationResult result) {
        if (config.getParserExtension() == null) return;

        ParserRegistry registry = ParserRegistry.defaultRegistry();
        if (!registry.supports(config.getParserExtension())) {
            result.add(ValidationError.error(
                    "PARSER_EXTENSION_UNSUPPORTED",
                    "Unsupported parser extension: " + config.getParserExtension()
                            + ". Supported: " + registry.getSupportedExtensions(),
                    FIELD_PARSER_EXTENSION
            ));
        }
    }

    private static void validateWriterExtension(EitriConfig config, ValidationResult result) {
        if (config.getWriterExtension() == null) return;
        
        WriterRegistry registry = WriterRegistry.defaultRegistry();
        if (!registry.supports(config.getWriterExtension())) {
            result.add(ValidationError.error(
                    "WRITER_EXTENSION_UNSUPPORTED",
                    "Unsupported writer extension: " + config.getWriterExtension()
                            + ". Supported: " + registry.getSupportedExtensions(),
                    FIELD_WRITER_EXTENSION
            ));
        }
    }
}
