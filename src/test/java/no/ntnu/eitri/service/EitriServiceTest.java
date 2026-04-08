package no.ntnu.eitri.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
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
                run_id: "  service-run-1  "
                source_relpaths:
                  - "  module-a/src/main/java  "
                  - " module-b/src/main/java "
                parser_extension: " .java "
                writer_extension: " .puml "
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
        assertTrue(Files.exists(runDir.resolve("artifacts/model/model_snapshot.json")));
        assertTrue(Files.exists(runDir.resolve("artifacts/model/repository_stats.json")));
        assertTrue(Files.isDirectory(runDir.resolve("artifacts/model/logs")));
        assertTrue(Files.exists(runDir.resolve("artifacts/model/logs/service.log")));
        assertTrue(Files.exists(runDir.resolve("artifacts/model/logs/.eitri.config.yaml")));

        Map<String, Object> report = readYamlLikeJson(runDir.resolve("outputs/run_report.json"));
        assertEquals("service-run-1", report.get("run_id"));
        assertEquals("passed", report.get("status"));
        assertNull(report.get("reason"));
        assertEquals(2, ((Number) report.get("type_count")).intValue());
        @SuppressWarnings("unchecked")
        Map<String, Object> repositoryStats = (Map<String, Object>) report.get("repository_stats");
        assertNotNull(repositoryStats);
        assertEquals(2, ((Number) repositoryStats.get("source_file_count")).intValue());
        assertEquals(2, ((Number) repositoryStats.get("package_count")).intValue());
        assertEquals(List.of("demo.a", "demo.b"), repositoryStats.get("packages"));
        assertNotNull(report.get("started_at"));
        assertNotNull(report.get("finished_at"));

        @SuppressWarnings("unchecked")
        Map<String, Object> inputs = (Map<String, Object>) report.get("inputs");
        assertEquals(List.of("module-a/src/main/java", "module-b/src/main/java"), inputs.get("source_relpaths"));
        assertEquals(".java", inputs.get("parser_extension"));
        assertEquals(".puml", inputs.get("writer_extension"));

        @SuppressWarnings("unchecked")
        Map<String, Object> artifacts = (Map<String, Object>) report.get("artifacts");
        assertEquals(runDir.resolve("artifacts/model/diagram.puml").toString(), artifacts.get("diagram_path"));
        assertEquals(runDir.resolve("artifacts/model/diagram_v2.puml").toString(), artifacts.get("diagram_v2_path"));
        assertEquals(runDir.resolve("artifacts/model/diagram_v3.puml").toString(), artifacts.get("diagram_v3_path"));
        assertEquals(
                runDir.resolve("artifacts/model/model_snapshot.json").toString(),
                artifacts.get("model_snapshot_path"));
        assertEquals(
                runDir.resolve("artifacts/model/repository_stats.json").toString(),
                artifacts.get("repository_stats_path"));

        String diagram = Files.readString(runDir.resolve("artifacts/model/diagram.puml"));
        assertTrue(diagram.contains("@startuml service-demo"));
        assertFalse(diagram.contains("-secret : int"));

        Map<String, Object> snapshot = readYamlLikeJson(runDir.resolve("artifacts/model/model_snapshot.json"));
        assertEquals("uml_model_snapshot.v1", snapshot.get("schema_version"));
        assertEquals(List.of("demo.a", "demo.b"), snapshot.get("packages"));
    }

    @Test
    void emitsDegradedVariantArtifactsAndReportDetails() throws Exception {
        Path inputDir = tempDir.resolve("input-degraded");
        Path runDir = tempDir.resolve("run-degraded");
        Path manifestPath = runDir.resolve("config").resolve("manifest.yaml");
        Files.createDirectories(manifestPath.getParent());

        writeJavaSource(
                inputDir.resolve("src/main/java/demo/SampleBase.java"),
                """
                        package demo;
                        public class SampleBase {}
                        """);
        writeJavaSource(
                inputDir.resolve("src/main/java/demo/SampleB.java"),
                """
                        package demo;
                        public class SampleB {
                            public String label;
                            public void help() {}
                        }
                        """);
        writeJavaSource(
                inputDir.resolve("src/main/java/demo/SampleA.java"),
                """
                        package demo;
                        public class SampleA extends SampleBase {
                            public String value;
                            public SampleB collaborator;
                            public int count;
                            public void ping() {}
                            public void reset() {}
                        }
                        """);

        Files.writeString(manifestPath, """
                version: 1
                run_id: degraded-run
                source_relpaths:
                  - src/main/java
                """);

        int exitCode = new EitriService(inputDir, runDir, manifestPath).run();

        assertEquals(0, exitCode);
        Path diagramPath = runDir.resolve("artifacts/model/diagram.puml");
        Path diagramV2Path = runDir.resolve("artifacts/model/diagram_v2.puml");
        Path diagramV3Path = runDir.resolve("artifacts/model/diagram_v3.puml");
        assertTrue(Files.exists(diagramPath));
        assertTrue(Files.exists(diagramV2Path));
        assertTrue(Files.exists(diagramV3Path));

        String canonicalDiagram = Files.readString(diagramPath);
        String diagramV2 = Files.readString(diagramV2Path);
        String diagramV3 = Files.readString(diagramV3Path);
        assertNotEquals(canonicalDiagram, diagramV2);
        assertNotEquals(canonicalDiagram, diagramV3);

        Map<String, Object> report = readYamlLikeJson(runDir.resolve("outputs/run_report.json"));
        @SuppressWarnings("unchecked")
        Map<String, Object> artifacts = (Map<String, Object>) report.get("artifacts");
        assertEquals(diagramV2Path.toString(), artifacts.get("diagram_v2_path"));
        assertEquals(diagramV3Path.toString(), artifacts.get("diagram_v3_path"));

        @SuppressWarnings("unchecked")
        Map<String, Object> degradation = (Map<String, Object>) report.get("degradation");
        assertNotNull(degradation);
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> variants = (List<Map<String, Object>>) degradation.get("variants");
        assertEquals(2, variants.size());
        for (Map<String, Object> variant : variants) {
            assertTrue(((Number) variant.get("eligible_candidate_count"))
                    .intValue() >= ((Number) variant.get("applied_count")).intValue());
            assertTrue(((Number) variant.get("applied_count")).intValue() > 0);
        }

        String summary = Files.readString(runDir.resolve("outputs/summary.md"));
        assertTrue(summary.contains("diagram_v2_path"));
        assertTrue(summary.contains("diagram_v3_path"));
        assertTrue(summary.contains("diagram_v2_applied_count"));
        assertTrue(summary.contains("diagram_v3_applied_count"));
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

    @Test
    void usesGeneratedRunIdForCurrentDirectorySourceRelpath() throws Exception {
        Path inputDir = tempDir.resolve("input-root");
        Path runDir = tempDir.resolve("run-dot");
        Path manifestPath = runDir.resolve("config").resolve("manifest.yaml");
        Files.createDirectories(manifestPath.getParent());

        writeJavaSource(
                inputDir.resolve("Sample.java"),
                """
                        public class Sample {}
                        """);

        Files.writeString(manifestPath, """
                version: 1
                source_relpaths:
                  - .
                """);

        EitriService service = new EitriService(
                inputDir,
                runDir,
                manifestPath,
                Clock.fixed(Instant.parse("2026-03-12T08:00:00Z"), ZoneOffset.UTC));

        int exitCode = service.run();

        assertEquals(0, exitCode);
        Map<String, Object> report = readYamlLikeJson(runDir.resolve("outputs/run_report.json"));
        assertEquals("20260312T080000Z", report.get("run_id"));
        assertEquals("passed", report.get("status"));
    }

    @Test
    void emitsReportWhenManifestIsMissing() throws Exception {
        Path inputDir = tempDir.resolve("input-missing-manifest");
        Path runDir = tempDir.resolve("run-missing-manifest");
        Path manifestPath = runDir.resolve("config").resolve("manifest.yaml");
        Files.createDirectories(inputDir);
        Files.createDirectories(manifestPath.getParent());

        int exitCode = new EitriService(inputDir, runDir, manifestPath).run();

        assertEquals(0, exitCode);
        Map<String, Object> report = readYamlLikeJson(runDir.resolve("outputs/run_report.json"));
        assertEquals("error", report.get("status"));
        assertEquals("missing-manifest", report.get("reason"));
    }

    @Test
    void rejectsSourceRelpathThatEscapesInputRoot() throws Exception {
        Path inputDir = tempDir.resolve("input-escape");
        Path runDir = tempDir.resolve("run-escape");
        Path manifestPath = runDir.resolve("config").resolve("manifest.yaml");
        Files.createDirectories(inputDir);
        Files.createDirectories(manifestPath.getParent());

        Files.writeString(manifestPath, """
                version: 1
                source_relpaths:
                  - ../outside
                """);

        int exitCode = new EitriService(inputDir, runDir, manifestPath).run();

        assertEquals(0, exitCode);
        Map<String, Object> report = readYamlLikeJson(runDir.resolve("outputs/run_report.json"));
        assertEquals("error", report.get("status"));
        assertEquals("invalid-manifest", report.get("reason"));
        assertTrue(String.valueOf(report.get("status_detail")).contains("must not escape"));
    }

    @Test
    void rejectsSourceRelpathContainingColon() throws Exception {
        Path inputDir = tempDir.resolve("input-colon");
        Path runDir = tempDir.resolve("run-colon");
        Path manifestPath = runDir.resolve("config").resolve("manifest.yaml");
        Files.createDirectories(inputDir);
        Files.createDirectories(manifestPath.getParent());

        Files.writeString(manifestPath, """
                version: 1
                source_relpaths:
                  - module:src
                """);

        int exitCode = new EitriService(inputDir, runDir, manifestPath).run();

        assertEquals(0, exitCode);
        Map<String, Object> report = readYamlLikeJson(runDir.resolve("outputs/run_report.json"));
        assertEquals("error", report.get("status"));
        assertEquals("invalid-manifest", report.get("reason"));
        assertTrue(String.valueOf(report.get("status_detail")).contains("safe relative paths"));
    }

    @Test
    void returnsOneWhenRunDirectoriesCannotBePrepared() throws Exception {
        Path inputDir = tempDir.resolve("input-prepare-failure");
        Path runDir = tempDir.resolve("run-prepare-failure");
        Path manifestPath = runDir.resolve("config").resolve("manifest.yaml");
        Files.createDirectories(runDir);
        Files.writeString(runDir.resolve("outputs"), "not-a-directory");

        int exitCode = new EitriService(inputDir, runDir, manifestPath).run();

        assertEquals(1, exitCode);
    }

    @Test
    void attachLogFileSafelyReturnsNullWhenPathCannotBeCreated() throws Exception {
        EitriService service = new EitriService(
                tempDir.resolve("input"),
                tempDir.resolve("run"),
                tempDir.resolve("manifest.yaml"),
                Clock.systemUTC());

        Object result = invokePrivate(service, "attachLogFileSafely", new Class<?>[] { Path.class },
                tempDir.resolve("missing-parent").resolve("service.log"));

        assertNull(result);
    }

    @Test
    void resolveSourcePathRejectsBlankInput() throws Exception {
        Path inputDir = tempDir.resolve("input-blank");
        Files.createDirectories(inputDir);
        EitriService service = new EitriService(
                inputDir,
                tempDir.resolve("run-blank"),
                tempDir.resolve("manifest.yaml"),
                Clock.systemUTC());

        InvocationTargetException error = assertThrows(
                InvocationTargetException.class,
                () -> invokePrivate(service, "resolveSourcePath", new Class<?>[] { String.class }, "   "));

        EitriServiceManifestException cause = (EitriServiceManifestException) error.getCause();
        assertEquals("invalid-manifest", cause.reasonCode());
        assertTrue(cause.getMessage().contains("must not contain blanks"));
    }

    @Test
    void detachableLogHandlerDelegatesPublishFlushAndClose() throws Exception {
        Logger logger = Logger.getLogger("no.ntnu.eitri.test." + UUID.randomUUID());
        logger.setUseParentHandlers(false);
        RecordingHandler delegate = new RecordingHandler();
        logger.addHandler(delegate);

        Constructor<EitriService.DetachableLogHandler> constructor = EitriService.DetachableLogHandler.class
                .getDeclaredConstructor(Logger.class, Handler.class);
        constructor.setAccessible(true);
        EitriService.DetachableLogHandler detachable = constructor.newInstance(logger, delegate);

        LogRecord logRecord = new LogRecord(Level.INFO, "service log");
        detachable.publish(logRecord);
        detachable.flush();
        detachable.close();

        assertEquals(logRecord, delegate.lastPublished);
        assertTrue(delegate.flushed);
        assertTrue(delegate.closed);
        assertEquals(0, logger.getHandlers().length);
    }

    private void writeJavaSource(Path path, String content) throws IOException {
        Files.createDirectories(path.getParent());
        Files.writeString(path, content);
    }

    private Object invokePrivate(Object target, String methodName, Class<?>[] parameterTypes, Object... args)
            throws Exception {
        Method method = target.getClass().getDeclaredMethod(methodName, parameterTypes);
        method.setAccessible(true);
        return method.invoke(target, args);
    }

    private Map<String, Object> readYamlLikeJson(Path path) throws IOException {
        try (var in = Files.newInputStream(path)) {
            return new Yaml().load(in);
        }
    }

    private static final class RecordingHandler extends Handler {
        private LogRecord lastPublished;
        private boolean flushed;
        private boolean closed;

        @Override
        public void publish(LogRecord record) {
            lastPublished = record;
        }

        @Override
        public void flush() {
            flushed = true;
        }

        @Override
        public void close() {
            closed = true;
        }
    }
}
