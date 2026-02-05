package no.ntnu.eitri.config;

import no.ntnu.eitri.cli.CliOptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ConfigServiceTest {

    @TempDir
    Path tempDir;

    @Test
    void throwsWhenExplicitConfigMissing() {
        Path missing = tempDir.resolve("missing.yaml");
        CliOptions options = new CliOptions(
                List.of(tempDir),
                tempDir.resolve("out.puml"),
                missing,
                null,
                null,
                false,
                false
        );

        ConfigService service = new ConfigService();
        ConfigException ex = assertThrows(ConfigException.class, () -> service.resolve(options));
        assertTrue(ex.getMessage().contains("Config file not found"));
    }

    @Test
    void usesExplicitConfigPathWhenProvided() throws Exception {
        Path configFile = tempDir.resolve("config.yaml");
        Files.writeString(configFile, "# empty config\n");

        CliOptions options = new CliOptions(
                List.of(tempDir),
                tempDir.resolve("out.puml"),
                configFile,
                ".java",
                ".puml",
                false,
                false
        );

        ConfigService service = new ConfigService();
        ConfigResolution resolution = service.resolve(options);

        assertEquals(configFile, resolution.configFileUsed());
        assertEquals(tempDir.resolve("out.puml"), resolution.config().getOutputPath());
    }

    @Test
    void usesWorkingDirConfigWhenPresent() throws Exception {
        String previous = System.getProperty("user.dir");
        Path workDir = tempDir.resolve("work");
        Files.createDirectories(workDir);
        Path defaultConfig = workDir.resolve(ConfigLoader.DEFAULT_CONFIG_FILENAME);
        Files.writeString(defaultConfig, "# empty config\n");

        try {
            System.setProperty("user.dir", workDir.toString());

            CliOptions options = new CliOptions(
                    List.of(workDir),
                    workDir.resolve("out.puml"),
                    null,
                    ".java",
                    ".puml",
                    false,
                    false
            );

            ConfigService service = new ConfigService();
            ConfigResolution resolution = service.resolve(options);

            assertEquals(defaultConfig, resolution.configFileUsed());
        } finally {
            System.setProperty("user.dir", previous);
        }
    }
}
