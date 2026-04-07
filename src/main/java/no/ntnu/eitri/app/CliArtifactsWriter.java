package no.ntnu.eitri.app;

import no.ntnu.eitri.cli.CliOptions;
import no.ntnu.eitri.config.ConfigException;
import no.ntnu.eitri.config.ConfigService;
import no.ntnu.eitri.config.PlantUmlConfig;
import no.ntnu.eitri.degradation.ModelDegrader;
import no.ntnu.eitri.model.UmlModel;
import no.ntnu.eitri.writer.WriteException;
import no.ntnu.eitri.writer.plantuml.PlantUmlWriter;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Writes additive CLI-side artifacts for manual and local testing.
 */
public final class CliArtifactsWriter {

    private static final String REPORT_SCHEMA_VERSION = "eitri_cli_report.v1";

    public void write(CliOptions cliOptions, RunResult result) throws IOException, ConfigException, WriteException {
        if (result.exitCode() != 0 || result.dryRun() || result.outputPath() == null || result.model() == null) {
            return;
        }

        PlantUmlConfig config = new ConfigService().resolve(cliOptions).plantUmlConfig();
        ModelDegrader degrader = new ModelDegrader();
        List<ModelDegrader.DiagramDegradationResult> variants = degrader.degradeAll(result.model(), config);
        PlantUmlWriter writer = new PlantUmlWriter();

        Path outputPath = result.outputPath();
        Path diagramV2Path = siblingPath(outputPath, "diagram_v2.puml");
        Path diagramV3Path = siblingPath(outputPath, "diagram_v3.puml");
        for (ModelDegrader.DiagramDegradationResult variant : variants) {
            writer.write(variant.model(), config, siblingPath(outputPath, variant.variant() + ".puml"));
        }

        Path reportPath = siblingPath(outputPath, "run_report.json");
        Path summaryPath = siblingPath(outputPath, "summary.md");
        Map<String, Object> report = reportDocument(result, diagramV2Path, diagramV3Path, variants);
        Files.writeString(reportPath, toJson(report) + System.lineSeparator());
        Files.writeString(summaryPath, renderSummary(report));
    }

    private Map<String, Object> reportDocument(
            RunResult result,
            Path diagramV2Path,
            Path diagramV3Path,
            List<ModelDegrader.DiagramDegradationResult> variants) {
        Map<String, Object> report = new LinkedHashMap<>();
        report.put("report_schema_version", REPORT_SCHEMA_VERSION);
        report.put("status", "passed");
        report.put("generated_at", Instant.now().toString());
        report.put("type_count", result.typeCount());
        report.put("relation_count", result.relationCount());
        report.put("dry_run", result.dryRun());
        report.put("repository_stats", repositoryStatsDocument(result.repositoryStats()));

        Map<String, Object> artifacts = new LinkedHashMap<>();
        artifacts.put("diagram_path", result.outputPath().toString());
        artifacts.put("diagram_v2_path", diagramV2Path.toString());
        artifacts.put("diagram_v3_path", diagramV3Path.toString());
        artifacts.put("model_snapshot_path", ModelSnapshotWriter.defaultPath(result.outputPath()).toString());
        report.put("artifacts", artifacts);
        report.put("degradation", degradationDocument(result.outputPath(), variants));
        return report;
    }

    private Map<String, Object> repositoryStatsDocument(RepositoryStats repositoryStats) {
        if (repositoryStats == null) {
            return null;
        }
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

    private Map<String, Object> degradationDocument(
            Path outputPath,
            List<ModelDegrader.DiagramDegradationResult> variants) {
        Map<String, Object> document = new LinkedHashMap<>();
        List<Map<String, Object>> variantEntries = new ArrayList<>();
        for (ModelDegrader.DiagramDegradationResult variant : variants) {
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("variant", variant.variant());
            entry.put("diagram_path", siblingPath(outputPath, variant.variant() + ".puml").toString());
            entry.put("percentage", variant.percentage());
            entry.put("minimum", variant.minimum());
            entry.put("eligible_candidate_count", variant.eligibleCandidateCount());
            entry.put("applied_count", variant.appliedCount());

            List<Map<String, Object>> appliedEntries = new ArrayList<>();
            for (ModelDegrader.AppliedDegradation applied : variant.applied()) {
                Map<String, Object> appliedEntry = new LinkedHashMap<>();
                appliedEntry.put("kind", applied.kind());
                appliedEntry.put("owner_fqn", applied.ownerFqn());
                appliedEntry.put("target", applied.target());
                appliedEntry.put("detail", applied.detail());
                appliedEntries.add(appliedEntry);
            }
            entry.put("applied", appliedEntries);
            variantEntries.add(entry);
        }
        document.put("variants", variantEntries);
        return document;
    }

    private String renderSummary(Map<String, Object> report) {
        @SuppressWarnings("unchecked")
        Map<String, Object> artifacts = (Map<String, Object>) report.get("artifacts");
        @SuppressWarnings("unchecked")
        Map<String, Object> degradation = (Map<String, Object>) report.get("degradation");

        return """
                # Eitri CLI Run Report

                | Field | Value |
                |-------|-------|
                | status | %s |
                | diagram_path | %s |
                | diagram_v2_path | %s |
                | diagram_v3_path | %s |
                | type_count | %s |
                | relation_count | %s |
                | diagram_v2_applied_count | %s |
                | diagram_v3_applied_count | %s |
                | generated_at | %s |
                """.formatted(
                report.get("status"),
                artifacts.get("diagram_path"),
                artifacts.get("diagram_v2_path"),
                artifacts.get("diagram_v3_path"),
                report.get("type_count"),
                report.get("relation_count"),
                appliedCountForVariant(degradation, "diagram_v2"),
                appliedCountForVariant(degradation, "diagram_v3"),
                report.get("generated_at"));
    }

    @SuppressWarnings("unchecked")
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
                Object count = variantMap.get("applied_count");
                return count != null ? count.toString() : "";
            }
        }
        return "";
    }

    private Path siblingPath(Path outputPath, String fileName) {
        Path parent = outputPath.getParent();
        return parent == null ? Path.of(fileName) : parent.resolve(fileName);
    }

    @SuppressWarnings("unchecked")
    private String toJson(Object value) {
        if (value == null) {
            return "null";
        }
        if (value instanceof String stringValue) {
            return "\"" + escape(stringValue) + "\"";
        }
        if (value instanceof Number || value instanceof Boolean) {
            return String.valueOf(value);
        }
        if (value instanceof Map<?, ?> map) {
            StringBuilder builder = new StringBuilder("{");
            boolean first = true;
            for (Map.Entry<Object, Object> entry : ((Map<Object, Object>) map).entrySet()) {
                if (!first) {
                    builder.append(',');
                }
                first = false;
                builder.append("\"").append(escape(String.valueOf(entry.getKey()))).append("\":");
                builder.append(toJson(entry.getValue()));
            }
            builder.append('}');
            return builder.toString();
        }
        if (value instanceof Iterable<?> iterable) {
            StringBuilder builder = new StringBuilder("[");
            boolean first = true;
            for (Object item : iterable) {
                if (!first) {
                    builder.append(',');
                }
                first = false;
                builder.append(toJson(item));
            }
            builder.append(']');
            return builder.toString();
        }
        throw new IllegalArgumentException("Unsupported JSON value: " + value.getClass().getName());
    }

    private String escape(String value) {
        return value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}
