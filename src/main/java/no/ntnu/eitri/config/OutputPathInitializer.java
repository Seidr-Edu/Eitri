package no.ntnu.eitri.config;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Prepares the output path for writing by ensuring the parent directory exists.
 */
public final class OutputPathInitializer {

    private OutputPathInitializer() {
        // Utility class
    }

    public static void initialize(Path outputPath) throws ConfigException {
        if (outputPath == null) {
            throw new ConfigException("Output path is required.");
        }
        Path parent = outputPath.getParent();
        if (parent == null) {
            return;
        }
        if (Files.exists(parent)) {
            if (!Files.isWritable(parent)) {
                throw new ConfigException("Output directory is not writable: " + parent);
            }
            return;
        }
        try {
            Files.createDirectories(parent);
        } catch (Exception e) {
            throw new ConfigException("Cannot create output directory: " + parent, e);
        }
    }
}
