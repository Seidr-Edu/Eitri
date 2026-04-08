package no.ntnu.eitri.service;

import no.ntnu.eitri.app.EitriRunner;
import no.ntnu.eitri.app.RepositoryStats;
import no.ntnu.eitri.app.RunResult;
import no.ntnu.eitri.cli.CliOptions;
import no.ntnu.eitri.config.ConfigException;
import no.ntnu.eitri.config.ConfigService;
import no.ntnu.eitri.config.PlantUmlConfig;
import no.ntnu.eitri.degradation.ModelDegrader;
import no.ntnu.eitri.model.UmlModel;
import no.ntnu.eitri.writer.WriteException;
import no.ntnu.eitri.writer.plantuml.PlantUmlWriter;
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
import java.util.logging.LogRecord;
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
            LOGGER.log(Level.SEVERE, "Unable to prepare service output directory.", e);
            return 1;
        }

        Path logsDir = logsDir();
        DetachableLogHandler logHandler = attachLogFileSafely(logsDir.resolve("service.log"));

        try {
            return executeServiceRun(startedAt, defaultRunId, logsDir);
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Unable to emit service report.", e);
            return 1;
        } finally {
            if (logHandler != null) {
                logHandler.close();
            }
        }
    }

    private int executeServiceRun(Instant startedAt, String defaultRunId, Path logsDir) throws IOException {
        EitriServiceManifest manifest = EitriServiceManifest.empty(defaultRunId);
        try {
            manifest = EitriServiceManifestLoader.load(manifestPath);
            return executeLoadedManifest(manifest, defaultRunId, startedAt, logsDir);
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
                    null,
                    null,
                    startedAt,
                    clock.instant());
            return 0;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Unexpected service error", e);
            writeReports(
                    resolveRunId(manifest, defaultRunId),
                    "error",
                    "service-error",
                    e.getMessage(),
                    manifest,
                    0,
                    0,
                    null,
                    null,
                    startedAt,
                    clock.instant());
            return 0;
        }
    }

    private int executeLoadedManifest(
            EitriServiceManifest manifest,
            String defaultRunId,
            Instant startedAt,
            Path logsDir) throws IOException, EitriServiceManifestException, ConfigException {
        String runId = resolveRunId(manifest, defaultRunId);
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
        DegradationArtifacts degradationArtifacts = null;
        if (result.exitCode() == 0 && result.model() != null && shouldGenerateDegradedArtifacts(cliOptions)) {
            try {
                degradationArtifacts = generateDegradedArtifacts(result.model(), cliOptions);
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Failed to generate degraded diagram variants", e);
                writeReports(
                        runId,
                        "error",
                        "degradation-error",
                        e.getMessage(),
                        manifest,
                        result.typeCount(),
                        result.relationCount(),
                        result.repositoryStats(),
                        degradationArtifacts,
                        startedAt,
                        clock.instant());
                return 0;
            }
        }
        writeReports(
                runId,
                result.exitCode() == 0 ? "passed" : "error",
                result.failureKind() != null ? result.failureKind().reasonCode() : null,
                result.errorMessage(),
                manifest,
                result.typeCount(),
                result.relationCount(),
                result.repositoryStats(),
                degradationArtifacts,
                startedAt,
                clock.instant());
        return 0;
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

    private DetachableLogHandler attachLogFileSafely(Path logPath) {
        try {
            return attachLogFile(logPath);
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Unable to attach service log file.", e);
            return null;
        }
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
            RepositoryStats repositoryStats,
            DegradationArtifacts degradationArtifacts,
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
        if (repositoryStats != null) {
            Map<String, Object> statsDocument = repositoryStatsDocument(repositoryStats);
            report.put("repository_stats", statsDocument);
            JsonWriter.write(repositoryStatsPath(), statsDocument);
        }

        Map<String, Object> inputs = new LinkedHashMap<>();
        inputs.put("source_root", inputDir.toString());
        inputs.put("source_relpaths", manifest.sourceRelpaths());
        inputs.put("parser_extension", manifest.parserExtension());
        inputs.put("writer_extension", manifest.writerExtension());
        inputs.put("verbose", manifest.verbose());
        report.put("inputs", inputs);

        Map<String, Object> artifacts = new LinkedHashMap<>();
        artifacts.put("diagram_path", diagramPath().toString());
        artifacts.put("diagram_v2_path",
                degradationArtifacts != null ? degradationArtifacts.diagramV2Path().toString() : null);
        artifacts.put("diagram_v3_path",
                degradationArtifacts != null ? degradationArtifacts.diagramV3Path().toString() : null);
        artifacts.put("model_snapshot_path", modelSnapshotPath().toString());
        artifacts.put("logs_dir", logsDir().toString());
        artifacts.put("repository_stats_path", repositoryStats != null ? repositoryStatsPath().toString() : null);
        report.put("artifacts", artifacts);
        report.put("degradation", degradationArtifacts != null ? degradationDocument(degradationArtifacts) : null);

        JsonWriter.write(reportPath(), report);
        Files.writeString(summaryPath(), renderSummary(report));
    }

    private String renderSummary(Map<String, Object> report) {
        @SuppressWarnings("unchecked")
        Map<String, Object> inputs = (Map<String, Object>) report.get("inputs");
        @SuppressWarnings("unchecked")
        Map<String, Object> artifacts = (Map<String, Object>) report.get("artifacts");
        @SuppressWarnings("unchecked")
        Map<String, Object> repositoryStats = (Map<String, Object>) report.get("repository_stats");
        @SuppressWarnings("unchecked")
        Map<String, Object> degradation = (Map<String, Object>) report.get("degradation");

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
                | diagram_v2_path | %s |
                | diagram_v3_path | %s |
                | type_count | %s |
                | relation_count | %s |
                | diagram_v2_applied_count | %s |
                | diagram_v3_applied_count | %s |
                | source_file_count | %s |
                | package_count | %s |
                | started_at | %s |
                | finished_at | %s |
                """.formatted(
                report.get("run_id"),
                report.get("status"),
                nullToEmpty(report.get("reason")),
                nullToEmpty(report.get("status_detail")),
                inputs.get("source_relpaths"),
                artifacts.get("diagram_path"),
                nullToEmpty(artifacts.get("diagram_v2_path")),
                nullToEmpty(artifacts.get("diagram_v3_path")),
                report.get("type_count"),
                report.get("relation_count"),
                appliedCountForVariant(degradation, "diagram_v2"),
                appliedCountForVariant(degradation, "diagram_v3"),
                repositoryStats != null ? repositoryStats.get("source_file_count") : "",
                repositoryStats != null ? repositoryStats.get("package_count") : "",
                report.get("started_at"),
                report.get("finished_at"));
    }

    private Map<String, Object> repositoryStatsDocument(RepositoryStats repositoryStats) {
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("source_path_count", repositoryStats.sourcePathCount());
        stats.put("source_file_count", repositoryStats.sourceFileCount());
        stats.put("type_count", repositoryStats.typeCount());
        stats.put("top_level_type_count", repositoryStats.topLevelTypeCount());
        stats.put("nested_type_count", repositoryStats.nestedTypeCount());
        stats.put("package_count", repositoryStats.packageCount());
        stats.put("packages", repositoryStats.packages());
        stats.put("package_type_counts", repositoryStats.packageTypeCounts());
        stats.put("type_kind_counts", repositoryStats.typeKindCounts());
        return stats;
    }

    private Map<String, Object> degradationDocument(DegradationArtifacts degradationArtifacts) {
        Map<String, Object> document = new LinkedHashMap<>();
        List<Map<String, Object>> variants = new ArrayList<>();
        for (ModelDegrader.DiagramDegradationResult variant : degradationArtifacts.variants()) {
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("variant", variant.variant());
            entry.put("diagram_path", pathForVariant(degradationArtifacts, variant.variant()).toString());
            entry.put("percentage", variant.percentage());
            entry.put("minimum", variant.minimum());
            entry.put("eligible_candidate_count", variant.eligibleCandidateCount());
            entry.put("applied_count", variant.appliedCount());

            List<Map<String, Object>> applied = new ArrayList<>();
            for (ModelDegrader.AppliedDegradation degradation : variant.applied()) {
                Map<String, Object> appliedEntry = new LinkedHashMap<>();
                appliedEntry.put("kind", degradation.kind());
                appliedEntry.put("owner_fqn", degradation.ownerFqn());
                appliedEntry.put("target", degradation.target());
                appliedEntry.put("detail", degradation.detail());
                applied.add(appliedEntry);
            }
            entry.put("applied", applied);
            variants.add(entry);
        }
        document.put("variants", variants);
        return document;
    }

    private Path generateVariantPath(String variantId) {
        return modelDir().resolve(variantId + ".puml");
    }

    private DegradationArtifacts generateDegradedArtifacts(UmlModel model, CliOptions cliOptions)
            throws WriteException, IOException, ConfigException {
        PlantUmlConfig plantUmlConfig = new ConfigService().resolve(cliOptions).plantUmlConfig();
        ModelDegrader degrader = new ModelDegrader();
        List<ModelDegrader.DiagramDegradationResult> variants = degrader.degradeAll(model, plantUmlConfig);
        PlantUmlWriter writer = new PlantUmlWriter();
        Path diagramV2 = generateVariantPath("diagram_v2");
        Path diagramV3 = generateVariantPath("diagram_v3");

        for (ModelDegrader.DiagramDegradationResult variant : variants) {
            writer.write(variant.model(), plantUmlConfig, generateVariantPath(variant.variant()));
        }
        return new DegradationArtifacts(diagramV2, diagramV3, variants);
    }

    private boolean shouldGenerateDegradedArtifacts(CliOptions cliOptions) throws ConfigException {
        return new ConfigService().resolve(cliOptions).plantUmlConfig().generateDegradedDiagrams();
    }

    private String appliedCountForVariant(Map<String, Object> degradation, String variantId) {
        if (degradation == null) {
            return "";
        }
        Object rawVariants = degradation.get("variants");
        if (!(rawVariants instanceof List<?> variants)) {
            return "";
        }
        for (Object rawVariant : variants) {
            if (!(rawVariant instanceof Map<?, ?> variantMap)) {
                continue;
            }
            if (variantId.equals(variantMap.get("variant"))) {
                Object appliedCount = variantMap.get("applied_count");
                return appliedCount != null ? appliedCount.toString() : "";
            }
        }
        return "";
    }

    private Path pathForVariant(DegradationArtifacts degradationArtifacts, String variantId) {
        return switch (variantId) {
            case "diagram_v2" -> degradationArtifacts.diagramV2Path();
            case "diagram_v3" -> degradationArtifacts.diagramV3Path();
            default -> generateVariantPath(variantId);
        };
    }

    private String generateRunId(Instant instant) {
        return RUN_ID_FORMATTER.format(instant);
    }

    private static String resolveRunId(EitriServiceManifest manifest, String defaultRunId) {
        return manifest.runId() != null ? manifest.runId() : defaultRunId;
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

    private Path repositoryStatsPath() {
        return modelDir().resolve("repository_stats.json");
    }

    private Path modelSnapshotPath() {
        return modelDir().resolve("model_snapshot.json");
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
        public void publish(LogRecord logRecord) {
            delegate.publish(logRecord);
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

    private record DegradationArtifacts(
            Path diagramV2Path,
            Path diagramV3Path,
            List<ModelDegrader.DiagramDegradationResult> variants) {
    }
}
