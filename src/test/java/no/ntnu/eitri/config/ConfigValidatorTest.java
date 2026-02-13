package no.ntnu.eitri.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConfigValidatorTest {

    @TempDir
    Path tempDir;

    @Test
    void reportsMissingRequiredFields() {
        RunConfig config = new RunConfig(List.of(), null, null, null, false, false);

        ValidationResult result = ConfigValidator.validate(config);

        assertFalse(result.isValid());
        List<ValidationError> errors = result.getErrors();
        assertTrue(errors.stream().anyMatch(e -> "SOURCE_PATHS_REQUIRED".equals(e.code())));
        assertTrue(errors.stream().anyMatch(e -> "OUTPUT_PATH_REQUIRED".equals(e.code())));
    }

    @Test
    void reportsUnsupportedExtensions() throws Exception {
        Path src = tempDir.resolve("src");
        Path out = tempDir.resolve("diagram.puml");
        java.nio.file.Files.createDirectories(src);

        RunConfig config = new RunConfig(List.of(src), out, ".foo", ".bar", false, false);

        ValidationResult result = ConfigValidator.validate(config);

        assertFalse(result.isValid());
        assertTrue(result.getErrors().stream().anyMatch(e -> "PARSER_EXTENSION_UNSUPPORTED".equals(e.code())));
        assertTrue(result.getErrors().stream().anyMatch(e -> "WRITER_EXTENSION_UNSUPPORTED".equals(e.code())));
    }
}
