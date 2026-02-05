package no.ntnu.eitri.util;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class PathExtensionTest {

    @Test
    void fromPathHandlesNullsAndMissingExtension() {
        assertNull(PathExtension.fromPath(null));
        assertNull(PathExtension.fromFileName(""));
        assertNull(PathExtension.fromFileName("file"));
        assertNull(PathExtension.fromFileName("file."));
    }

    @Test
    void fromFileNameNormalizesExtension() {
        assertEquals(".txt", PathExtension.fromFileName("file.TXT"));
        assertEquals(".env", PathExtension.fromFileName(".env"));
        assertEquals(".puml", PathExtension.fromPath(Path.of("diagram.puml")));
    }
}
