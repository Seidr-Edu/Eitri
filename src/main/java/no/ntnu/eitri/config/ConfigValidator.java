package no.ntnu.eitri.config;

import no.ntnu.eitri.app.registry.ParserRegistry;
import no.ntnu.eitri.app.registry.WriterRegistry;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Validates runtime configuration values and returns human-readable errors.
 */
public final class ConfigValidator {

    private static final String FIELD_SOURCE_PATHS = "sourcePaths";
    private static final String FIELD_OUTPUT_PATH = "outputPath";
    private static final String FIELD_PARSER_EXTENSION = "parserExtension";
    private static final String FIELD_WRITER_EXTENSION = "writerExtension";

    private ConfigValidator() {
    }

    public static ValidationResult validate(RunConfig config) {
        ValidationResult result = new ValidationResult();

        validateSourcePaths(config, result);
        validateOutputPath(config, result);
        validateParserExtension(config, result);
        validateWriterExtension(config, result);

        return result;
    }

    private static void validateSourcePaths(RunConfig config, ValidationResult result) {
        if (config.sourcePaths().isEmpty()) {
            result.add(ValidationError.error(
                    "SOURCE_PATHS_REQUIRED",
                    "At least one source path (--src) is required.",
                    FIELD_SOURCE_PATHS
            ));
            return;
        }

        for (Path src : config.sourcePaths()) {
            if (!Files.exists(src)) {
                result.add(ValidationError.error(
                        "SOURCE_PATH_NOT_FOUND",
                        "Source path does not exist: " + src,
                        FIELD_SOURCE_PATHS
                ));
            } else if (!Files.isDirectory(src) && !Files.isRegularFile(src)) {
                result.add(ValidationError.error(
                        "SOURCE_PATH_INVALID",
                        "Source path is not a file or directory: " + src,
                        FIELD_SOURCE_PATHS
                ));
            }
        }
    }

    private static void validateOutputPath(RunConfig config, ValidationResult result) {
        if (config.outputPath() == null) {
            result.add(ValidationError.error(
                    "OUTPUT_PATH_REQUIRED",
                    "Output path (--out) is required.",
                    FIELD_OUTPUT_PATH
            ));
            return;
        }

        Path parent = config.outputPath().getParent();
        if (parent != null && Files.exists(parent) && !Files.isWritable(parent)) {
            result.add(ValidationError.error(
                    "OUTPUT_DIR_NOT_WRITABLE",
                    "Output directory is not writable: " + parent,
                    FIELD_OUTPUT_PATH
            ));
        }
    }

    private static void validateParserExtension(RunConfig config, ValidationResult result) {
        if (config.parserExtension() == null) {
            return;
        }

        ParserRegistry registry = ParserRegistry.defaultRegistry();
        if (!registry.supports(config.parserExtension())) {
            result.add(ValidationError.error(
                    "PARSER_EXTENSION_UNSUPPORTED",
                    "Unsupported parser extension: " + config.parserExtension()
                            + ". Supported: " + registry.getSupportedExtensions(),
                    FIELD_PARSER_EXTENSION
            ));
        }
    }

    private static void validateWriterExtension(RunConfig config, ValidationResult result) {
        if (config.writerExtension() == null) {
            return;
        }

        WriterRegistry registry = WriterRegistry.defaultRegistry();
        if (!registry.supports(config.writerExtension())) {
            result.add(ValidationError.error(
                    "WRITER_EXTENSION_UNSUPPORTED",
                    "Unsupported writer extension: " + config.writerExtension()
                            + ". Supported: " + registry.getSupportedExtensions(),
                    FIELD_WRITER_EXTENSION
            ));
        }
    }
}
