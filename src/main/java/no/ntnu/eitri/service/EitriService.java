package no.ntnu.eitri.service;

import no.ntnu.eitri.app.EitriRunner;
import no.ntnu.eitri.app.RunResult;
import no.ntnu.eitri.cli.CliOptions;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.logging.FileHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

/**
 * Container service wrapper for Eitri.
 */
public final class EitriService {

    private static final Logger LOGGER = Logger.getLogger(EitriService.class.getName());
    private static final String REPORT_SCHEMA_VERSION = "eitri_service_report.v1";
    private static final DateTimeFormatter RUN_ID_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'")
            .withZone(ZoneOffset.UTC);

    private final Path inputDir;
    private final Path runDir;
    private final Path manifestPath;
    private final Clock clock;

    public EitriService(Path inputDir, Path runDir, Path manifestPath) {
        this(inputDir, runDir, manifestPath, Clock.systemUTC());
    }

    EitriService(Path inputDir, Path runDir, Path manifestPath, Clock clock) {
        this.inputDir = Objects.requireNonNull(inputDir, "inputDir");
        this.runDir = Objects.requireNonNull(runDir, "runDir");
        this.manifestPath = Objects.requireNonNull(manifestPath, "manifestPath");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    public int run() {
        Instant startedAt = clock.instant();
        String defaultRunId = generateRunId(startedAt);

        try {
            prepareRunDirectories();
        } catch (IOException e) {
            System.err.println("error: unable to prepare service output directory: " + e.getMessage());
            return 1;
        }

        Path logsDir = logsDir();
        DetachableLogHandler logHandler = null;
        try {
            logHandler = attachLogFile(logsDir.resolve("service.log"));
        } catch (IOException e) {
            System.err.println("warning: unable to attach service log file: " + e.getMessage());
        }

        try {
            EitriServiceManifest manifest = EitriServiceManifest.empty(defaultRunId);
            try {
                manifest = EitriServiceManifestLoader.load(manifestPath);
                String runId = manifest.runId() != null ? manifest.runId() : defaultRunId;
                Path configPath = materializeWriterConfigIfPresent(manifest, logsDir);
                List<Path> sourcePaths = resolveSourcePaths(manifest.sourceRelpaths());

                CliOptions cliOptions = new CliOptions(
                        sourcePaths,
                        diagramPath(),
                        configPath,
                        manifest.parserExtension(),
                        manifest.writerExtension(),
                        manifest.verbose(),
                        false);

                LOGGER.log(Level.INFO, "Starting Eitri service run {0}", runId);
                RunResult result = new EitriRunner().run(cliOptions);
                writeReports(
                        runId,
                        result.exitCode() == 0 ? "passed" : "error",
                        result.failureKind() != null ? result.failureKind().reasonCode() : null,
                        result.errorMessage(),
                        manifest,
                        result.typeCount(),
                        result.relationCount(),
                        startedAt,
                        clock.instant());
                return 0;
            } catch (EitriServiceManifestException e) {
                LOGGER.log(Level.SEVERE, "Manifest error: {0}", e.getMessage());
                writeReports(
                        defaultRunId,
                        "error",
                        e.reasonCode(),
                        e.getMessage(),
                        manifest,
                        0,
                        0,
                        startedAt,
                        clock.instant());
                return 0;
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Unexpected service error", e);
                writeReports(
                        manifest.runId() != null ? manifest.runId() : defaultRunId,
                        "error",
                        "service-error",
                        e.getMessage(),
                        manifest,
                        0,
                        0,
                        startedAt,
                        clock.instant());
                return 0;
            }
        } catch (IOException e) {
            System.err.println("error: unable to emit service report: " + e.getMessage());
            return 1;
        } finally {
            if (logHandler != null) {
                logHandler.close();
            }
        }
    }

    private void prepareRunDirectories() throws IOException {
        Files.createDirectories(runDir);
        if (!Files.isWritable(runDir)) {
            throw new IOException(runDir + " is not writable");
        }
        Files.createDirectories(outputsDir());
        Files.createDirectories(modelDir());
        Files.createDirectories(logsDir());
        if (!Files.isWritable(outputsDir())) {
            throw new IOException(outputsDir() + " is not writable");
        }
    }

    private DetachableLogHandler attachLogFile(Path logPath) throws IOException {
        FileHandler handler = new FileHandler(logPath.toString(), true);
        handler.setEncoding("UTF-8");
        handler.setFormatter(new SimpleFormatter());
        Logger rootLogger = Logger.getLogger("");
        rootLogger.addHandler(handler);
        return new DetachableLogHandler(rootLogger, handler);
    }

    private Path materializeWriterConfigIfPresent(EitriServiceManifest manifest, Path logsDir) throws IOException {
        if (!manifest.hasWriterConfig()) {
            return null;
        }

        Path configPath = logsDir.resolve(".eitri.config.yaml");
        Map<String, Object> root = new LinkedHashMap<>();
        root.put("writers", manifest.writers());
        try (Writer writer = Files.newBufferedWriter(configPath)) {
            new Yaml().dump(root, writer);
        }
        return configPath;
    }

    private List<Path> resolveSourcePaths(List<String> sourceRelpaths) throws EitriServiceManifestException {
        List<Path> resolved = new ArrayList<>(sourceRelpaths.size());
        for (String relpath : sourceRelpaths) {
            resolved.add(resolveSourcePath(relpath));
        }
        return resolved;
    }

    private Path resolveSourcePath(String relpath) throws EitriServiceManifestException {
        String raw = relpath.trim();
        if (raw.isEmpty()) {
            throw new EitriServiceManifestException("invalid-manifest", "source_relpaths must not contain blanks.");
        }
        if (raw.startsWith("/") || raw.contains(":")) {
            throw new EitriServiceManifestException(
                    "invalid-manifest",
                    "source_relpaths must contain only safe relative paths.");
        }

        Path resolved = ".".equals(raw)
                ? inputDir.normalize()
                : inputDir.resolve(Path.of(raw)).normalize();

        if (!resolved.startsWith(inputDir.normalize())) {
            throw new EitriServiceManifestException(
                    "invalid-manifest",
                    "source_relpaths must not escape /input/repo.");
        }
        return resolved;
    }

    private void writeReports(
            String runId,
            String status,
            String reason,
            String statusDetail,
            EitriServiceManifest manifest,
            int typeCount,
            int relationCount,
            Instant startedAt,
            Instant finishedAt) throws IOException {

        Map<String, Object> report = new LinkedHashMap<>();
        report.put("service_schema_version", REPORT_SCHEMA_VERSION);
        report.put("run_id", runId);
        report.put("status", status);
        report.put("reason", reason);
        report.put("status_detail", statusDetail);
        report.put("started_at", startedAt.toString());
        report.put("finished_at", finishedAt.toString());
        report.put("type_count", typeCount);
        report.put("relation_count", relationCount);

        Map<String, Object> inputs = new LinkedHashMap<>();
        inputs.put("source_root", inputDir.toString());
        inputs.put("source_relpaths", manifest.sourceRelpaths());
        inputs.put("parser_extension", manifest.parserExtension());
        inputs.put("writer_extension", manifest.writerExtension());
        inputs.put("verbose", manifest.verbose());
        report.put("inputs", inputs);

        Map<String, Object> artifacts = new LinkedHashMap<>();
        artifacts.put("diagram_path", diagramPath().toString());
        artifacts.put("logs_dir", logsDir().toString());
        report.put("artifacts", artifacts);

        JsonWriter.write(reportPath(), report);
        Files.writeString(summaryPath(), renderSummary(report));
    }

    private String renderSummary(Map<String, Object> report) {
        @SuppressWarnings("unchecked")
        Map<String, Object> inputs = (Map<String, Object>) report.get("inputs");
        @SuppressWarnings("unchecked")
        Map<String, Object> artifacts = (Map<String, Object>) report.get("artifacts");

        return """
                # Eitri Service Run Report

                | Field | Value |
                |-------|-------|
                | run_id | %s |
                | status | %s |
                | reason | %s |
                | status_detail | %s |
                | source_relpaths | %s |
                | diagram_path | %s |
                | type_count | %s |
                | relation_count | %s |
                | started_at | %s |
                | finished_at | %s |
                """.formatted(
                report.get("run_id"),
                report.get("status"),
                nullToEmpty(report.get("reason")),
                nullToEmpty(report.get("status_detail")),
                inputs.get("source_relpaths"),
                artifacts.get("diagram_path"),
                report.get("type_count"),
                report.get("relation_count"),
                report.get("started_at"),
                report.get("finished_at"));
    }

    private String generateRunId(Instant instant) {
        return RUN_ID_FORMATTER.format(instant);
    }

    private static String nullToEmpty(Object value) {
        return value == null ? "" : value.toString();
    }

    private Path outputsDir() {
        return runDir.resolve("outputs");
    }

    private Path modelDir() {
        return runDir.resolve("artifacts").resolve("model");
    }

    private Path logsDir() {
        return modelDir().resolve("logs");
    }

    private Path diagramPath() {
        return modelDir().resolve("diagram.puml");
    }

    private Path reportPath() {
        return outputsDir().resolve("run_report.json");
    }

    private Path summaryPath() {
        return outputsDir().resolve("summary.md");
    }

    static final class DetachableLogHandler extends Handler implements AutoCloseable {
        private final Logger rootLogger;
        private final Handler delegate;

        private DetachableLogHandler(Logger rootLogger, Handler delegate) {
            this.rootLogger = rootLogger;
            this.delegate = delegate;
        }

        @Override
        public void publish(java.util.logging.LogRecord record) {
            delegate.publish(record);
        }

        @Override
        public void flush() {
            delegate.flush();
        }

        @Override
        public void close() throws SecurityException {
            rootLogger.removeHandler(delegate);
            delegate.flush();
            delegate.close();
        }
    }
}
