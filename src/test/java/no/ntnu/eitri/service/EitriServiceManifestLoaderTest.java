package no.ntnu.eitri.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EitriServiceManifestLoaderTest {

    @TempDir
    Path tempDir;

    @Test
    void loadsValidManifestWithWriterConfig() throws Exception {
        Path manifest = tempDir.resolve("manifest.yaml");
        Files.writeString(manifest, """
                version: 1
                run_id: run-123
                source_relpaths:
                  - src/main/java
                  - shared/src/main/java
                parser_extension: .java
                writer_extension: .puml
                verbose: true
                writers:
                  plantuml:
                    diagramName: service-demo
                    hidePrivate: true
                """);

        EitriServiceManifest loaded = EitriServiceManifestLoader.load(manifest);

        assertEquals("run-123", loaded.runId());
        assertEquals(2, loaded.sourceRelpaths().size());
        assertEquals(".java", loaded.parserExtension());
        assertEquals(".puml", loaded.writerExtension());
        assertTrue(loaded.verbose());
        assertTrue(loaded.hasWriterConfig());
    }

    @Test
    void rejectsUnknownTopLevelKeys() throws Exception {
        Path manifest = tempDir.resolve("manifest.yaml");
        Files.writeString(manifest, """
                version: 1
                source_relpaths:
                  - src/main/java
                unexpected: true
                """);

        EitriServiceManifestException error = assertThrows(
                EitriServiceManifestException.class,
                () -> EitriServiceManifestLoader.load(manifest));

        assertEquals("unknown-manifest-key", error.reasonCode());
        assertTrue(error.getMessage().contains("unexpected"));
    }
}
