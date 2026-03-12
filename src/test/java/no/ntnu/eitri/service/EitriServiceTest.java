package no.ntnu.eitri.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EitriServiceTest {

    @TempDir
    Path tempDir;

    @Test
    void runsWithMultipleSourceRelpathsAndMaterializedWriterConfig() throws Exception {
        Path inputDir = tempDir.resolve("input-repo");
        Path runDir = tempDir.resolve("run");
        Path manifestPath = runDir.resolve("config").resolve("manifest.yaml");
        Files.createDirectories(manifestPath.getParent());

        writeJavaSource(
                inputDir.resolve("module-a/src/main/java/demo/a/SampleA.java"),
                """
                        package demo.a;
                        public class SampleA {
                            private int secret;
                            public String value;
                        }
                        """);
        writeJavaSource(
                inputDir.resolve("module-b/src/main/java/demo/b/SampleB.java"),
                """
                        package demo.b;
                        public class SampleB {
                        }
                        """);

        Files.writeString(manifestPath, """
                version: 1
                run_id: service-run-1
                source_relpaths:
                  - module-a/src/main/java
                  - module-b/src/main/java
                parser_extension: .java
                writer_extension: .puml
                verbose: true
                writers:
                  plantuml:
                    diagramName: service-demo
                    hidePrivate: true
                """);

        EitriService service = new EitriService(
                inputDir,
                runDir,
                manifestPath,
                Clock.fixed(Instant.parse("2026-03-12T08:00:00Z"), ZoneOffset.UTC));

        int exitCode = service.run();

        assertEquals(0, exitCode);
        assertTrue(Files.exists(runDir.resolve("outputs/run_report.json")));
        assertTrue(Files.exists(runDir.resolve("outputs/summary.md")));
        assertTrue(Files.exists(runDir.resolve("artifacts/model/diagram.puml")));
        assertTrue(Files.isDirectory(runDir.resolve("artifacts/model/logs")));
        assertTrue(Files.exists(runDir.resolve("artifacts/model/logs/service.log")));
        assertTrue(Files.exists(runDir.resolve("artifacts/model/logs/.eitri.config.yaml")));

        Map<String, Object> report = readYamlLikeJson(runDir.resolve("outputs/run_report.json"));
        assertEquals("service-run-1", report.get("run_id"));
        assertEquals("passed", report.get("status"));
        assertNull(report.get("reason"));
        assertEquals(2, ((Number) report.get("type_count")).intValue());
        assertNotNull(report.get("started_at"));
        assertNotNull(report.get("finished_at"));

        @SuppressWarnings("unchecked")
        Map<String, Object> inputs = (Map<String, Object>) report.get("inputs");
        assertEquals(List.of("module-a/src/main/java", "module-b/src/main/java"), inputs.get("source_relpaths"));

        @SuppressWarnings("unchecked")
        Map<String, Object> artifacts = (Map<String, Object>) report.get("artifacts");
        assertEquals(runDir.resolve("artifacts/model/diagram.puml").toString(), artifacts.get("diagram_path"));

        String diagram = Files.readString(runDir.resolve("artifacts/model/diagram.puml"));
        assertTrue(diagram.contains("@startuml service-demo"));
        assertFalse(diagram.contains("-secret : int"));
    }

    @Test
    void rejectsUnknownTopLevelManifestKeysWithReport() throws Exception {
        Path inputDir = tempDir.resolve("input-repo");
        Path runDir = tempDir.resolve("run-unknown");
        Path manifestPath = runDir.resolve("config").resolve("manifest.yaml");
        Files.createDirectories(manifestPath.getParent());
        Files.createDirectories(inputDir);

        Files.writeString(manifestPath, """
                version: 1
                source_relpaths:
                  - src/main/java
                unexpected: true
                """);

        int exitCode = new EitriService(inputDir, runDir, manifestPath).run();

        assertEquals(0, exitCode);
        Map<String, Object> report = readYamlLikeJson(runDir.resolve("outputs/run_report.json"));
        assertEquals("error", report.get("status"));
        assertEquals("unknown-manifest-key", report.get("reason"));
        assertTrue(Files.exists(runDir.resolve("outputs/summary.md")));
    }

    @Test
    void emitsReportOnRunnerConfigFailure() throws Exception {
        Path inputDir = tempDir.resolve("input-repo");
        Path runDir = tempDir.resolve("run-invalid-writer");
        Path manifestPath = runDir.resolve("config").resolve("manifest.yaml");
        Files.createDirectories(manifestPath.getParent());

        writeJavaSource(
                inputDir.resolve("src/main/java/demo/Sample.java"),
                """
                        package demo;
                        public class Sample {}
                        """);

        Files.writeString(manifestPath, """
                version: 1
                run_id: invalid-writer-run
                source_relpaths:
                  - src/main/java
                writer_extension: .invalid
                """);

        int exitCode = new EitriService(inputDir, runDir, manifestPath).run();

        assertEquals(0, exitCode);
        Map<String, Object> report = readYamlLikeJson(runDir.resolve("outputs/run_report.json"));
        assertEquals("error", report.get("status"));
        assertEquals("config-error", report.get("reason"));
        assertTrue(String.valueOf(report.get("status_detail")).contains("Unsupported writer extension"));
    }

    private void writeJavaSource(Path path, String content) throws IOException {
        Files.createDirectories(path.getParent());
        Files.writeString(path, content);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> readYamlLikeJson(Path path) throws IOException {
        try (var in = Files.newInputStream(path)) {
            return new Yaml().load(in);
        }
    }
}
