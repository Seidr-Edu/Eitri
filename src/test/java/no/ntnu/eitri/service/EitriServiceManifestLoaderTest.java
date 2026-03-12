package no.ntnu.eitri.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
                run_id: "  run-123  "
                source_relpaths:
                  - "  src/main/java  "
                  - " shared/src/main/java "
                parser_extension: " .java "
                writer_extension: " .puml "
                verbose: true
                writers:
                  plantuml:
                    diagramName: service-demo
                    hidePrivate: true
                """);

        EitriServiceManifest loaded = EitriServiceManifestLoader.load(manifest);

        assertEquals("run-123", loaded.runId());
        assertEquals(List.of("src/main/java", "shared/src/main/java"), loaded.sourceRelpaths());
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

    @Test
    void loadsStringVersionAndNormalizesNestedWriterLists() throws Exception {
        Path manifest = tempDir.resolve("manifest.yaml");
        Files.writeString(manifest, """
                version: "1"
                source_relpaths:
                  - .
                writers:
                  plantuml:
                    packages:
                      - demo.a
                      - demo.b
                """);

        EitriServiceManifest loaded = EitriServiceManifestLoader.load(manifest);

        assertEquals(List.of("."), loaded.sourceRelpaths());
        assertFalse(loaded.verbose());
        assertTrue(loaded.hasWriterConfig());
        @SuppressWarnings("unchecked")
        Map<String, Object> plantuml = (Map<String, Object>) loaded.writers().get("plantuml");
        assertEquals(List.of("demo.a", "demo.b"), plantuml.get("packages"));
    }

    @Test
    void rejectsMissingManifestFile() {
        Path manifest = tempDir.resolve("missing.yaml");

        EitriServiceManifestException error = assertThrows(
                EitriServiceManifestException.class,
                () -> EitriServiceManifestLoader.load(manifest));

        assertEquals("missing-manifest", error.reasonCode());
        assertTrue(error.getMessage().contains("Manifest file not found"));
    }

    @Test
    void rejectsNonMappingRoot() throws Exception {
        EitriServiceManifestException error = assertManifestError("""
                - src/main/java
                - shared/src/main/java
                """);

        assertEquals("invalid-manifest", error.reasonCode());
        assertTrue(error.getMessage().contains("mapping/object"));
    }

    @Test
    void rejectsMissingVersion() throws Exception {
        EitriServiceManifestException error = assertManifestError("""
                source_relpaths:
                  - src/main/java
                """);

        assertEquals("invalid-manifest", error.reasonCode());
        assertTrue(error.getMessage().contains("'version'"));
    }

    @Test
    void rejectsUnsupportedVersion() throws Exception {
        EitriServiceManifestException error = assertManifestError("""
                version: 2
                source_relpaths:
                  - src/main/java
                """);

        assertEquals("unsupported-manifest-version", error.reasonCode());
        assertTrue(error.getMessage().contains("Unsupported manifest version"));
    }

    @Test
    void rejectsNonIntegralNumericVersions() throws Exception {
        EitriServiceManifestException decimalError = assertManifestError("""
                version: 1.1
                source_relpaths:
                  - src/main/java
                """);
        assertEquals("unsupported-manifest-version", decimalError.reasonCode());

        EitriServiceManifestException floatError = assertManifestError("""
                version: 1.0
                source_relpaths:
                  - src/main/java
                """);
        assertEquals("unsupported-manifest-version", floatError.reasonCode());
    }

    @Test
    void rejectsBlankOptionalStringFields() throws Exception {
        EitriServiceManifestException error = assertManifestError("""
                version: 1
                run_id: "   "
                source_relpaths:
                  - src/main/java
                """);

        assertEquals("invalid-manifest", error.reasonCode());
        assertTrue(error.getMessage().contains("'run_id'"));
    }

    @Test
    void rejectsInvalidOptionalFieldTypes() throws Exception {
        EitriServiceManifestException parserError = assertManifestError("""
                version: 1
                source_relpaths:
                  - src/main/java
                parser_extension: 123
                """);
        assertEquals("invalid-manifest", parserError.reasonCode());
        assertTrue(parserError.getMessage().contains("'parser_extension'"));

        EitriServiceManifestException verboseError = assertManifestError("""
                version: 1
                source_relpaths:
                  - src/main/java
                verbose: 1
                """);
        assertEquals("invalid-manifest", verboseError.reasonCode());
        assertTrue(verboseError.getMessage().contains("'verbose'"));

        EitriServiceManifestException writersError = assertManifestError("""
                version: 1
                source_relpaths:
                  - src/main/java
                writers:
                  - plantuml
                """);
        assertEquals("invalid-manifest", writersError.reasonCode());
        assertTrue(writersError.getMessage().contains("'writers'"));
    }

    @Test
    void rejectsInvalidSourceRelpathsShapes() throws Exception {
        EitriServiceManifestException missingError = assertManifestError("""
                version: 1
                """);
        assertEquals("invalid-manifest", missingError.reasonCode());
        assertTrue(missingError.getMessage().contains("'source_relpaths' is required"));

        EitriServiceManifestException emptyError = assertManifestError("""
                version: 1
                source_relpaths: []
                """);
        assertEquals("invalid-manifest", emptyError.reasonCode());
        assertTrue(emptyError.getMessage().contains("must not be empty"));

        EitriServiceManifestException blankItemError = assertManifestError("""
                version: 1
                source_relpaths:
                  - "   "
                """);
        assertEquals("invalid-manifest", blankItemError.reasonCode());
        assertTrue(blankItemError.getMessage().contains("non-empty strings"));
    }

    @Test
    void rejectsNonStringKeysInNestedMappings() throws Exception {
        EitriServiceManifestException error = assertManifestError("""
                version: 1
                source_relpaths:
                  - src/main/java
                writers:
                  plantuml:
                    1: true
                """);

        assertEquals("invalid-manifest", error.reasonCode());
        assertTrue(error.getMessage().contains("writers.plantuml"));
    }

    @Test
    void rejectsMalformedYaml() throws Exception {
        EitriServiceManifestException error = assertManifestError("version: [");

        assertEquals("invalid-manifest", error.reasonCode());
        assertTrue(error.getMessage().contains("Failed to parse manifest YAML"));
    }

    @Test
    void rejectsDuplicateKeys() throws Exception {
        EitriServiceManifestException error = assertManifestError("""
                version: 1
                version: 2
                source_relpaths:
                  - src/main/java
                """);

        assertEquals("invalid-manifest", error.reasonCode());
        assertTrue(error.getMessage().contains("Failed to parse manifest YAML"));
    }

    @Test
    void rejectsUnsafeGlobalTags() throws Exception {
        EitriServiceManifestException error = assertManifestError("""
                version: 1
                run_id: !!java.net.URL "https://example.com"
                source_relpaths:
                  - src/main/java
                """);

        assertEquals("invalid-manifest", error.reasonCode());
        assertTrue(error.getMessage().contains("Failed to parse manifest YAML"));
    }

    private EitriServiceManifestException assertManifestError(String content) throws Exception {
        Path manifest = tempDir.resolve("manifest.yaml");
        Files.writeString(manifest, content);
        return assertThrows(EitriServiceManifestException.class, () -> EitriServiceManifestLoader.load(manifest));
    }
}
