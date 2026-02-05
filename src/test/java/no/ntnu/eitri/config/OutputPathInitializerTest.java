package no.ntnu.eitri.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.Assumptions;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class OutputPathInitializerTest {

    @TempDir
    Path tempDir;

    @Test
    void createsMissingParentDirectory() throws Exception {
        Path target = tempDir.resolve("nested/output.puml");
        Path parent = target.getParent();
        assertNotNull(parent);
        assertFalse(Files.exists(parent));

        OutputPathInitializer.initialize(target);

        assertTrue(Files.exists(parent));
        assertTrue(Files.isDirectory(parent));
    }

    @Test
    void failsWhenParentNotWritable() throws Exception {
        Assumptions.assumeTrue(tempDir.getFileSystem().supportedFileAttributeViews().contains("posix"));

        Path parent = tempDir.resolve("readonly");
        Files.createDirectories(parent);
        Files.setPosixFilePermissions(parent, Set.of(
                PosixFilePermission.OWNER_READ,
                PosixFilePermission.OWNER_EXECUTE
        ));
        Assumptions.assumeFalse(Files.isWritable(parent));

        Path target = parent.resolve("output.puml");
        ConfigException ex = assertThrows(ConfigException.class, () -> OutputPathInitializer.initialize(target));
        assertTrue(ex.getMessage().contains("not writable"));
    }
}
