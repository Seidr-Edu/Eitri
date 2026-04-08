package no.ntnu.eitri.app;

import no.ntnu.eitri.cli.CliOptions;
import no.ntnu.eitri.config.ConfigException;
import no.ntnu.eitri.config.ConfigService;
import no.ntnu.eitri.config.PlantUmlConfig;
import no.ntnu.eitri.degradation.ModelDegrader;
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

        Path outputPath = result.outputPath();
        Path diagramV2Path = null;
        Path diagramV3Path = null;
        List<ModelDegrader.DiagramDegradationResult> variants = List.of();

        if (config.generateDegradedDiagrams()) {
            ModelDegrader degrader = new ModelDegrader();
            PlantUmlWriter writer = new PlantUmlWriter();
            variants = degrader.degradeAll(result.model(), config);
            diagramV2Path = siblingPath(outputPath, "diagram_v2.puml");
            diagramV3Path = siblingPath(outputPath, "diagram_v3.puml");
            for (ModelDegrader.DiagramDegradationResult variant : variants) {
                writer.write(variant.model(), config, siblingPath(outputPath, variant.variant() + ".puml"));
            }
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
        artifacts.put("diagram_v2_path", diagramV2Path != null ? diagramV2Path.toString() : null);
        artifacts.put("diagram_v3_path", diagramV3Path != null ? diagramV3Path.toString() : null);
        artifacts.put("model_snapshot_path", ModelSnapshotWriter.defaultPath(result.outputPath()).toString());
        report.put("artifacts", artifacts);
        report.put("degradation", variants.isEmpty() ? null : degradationDocument(result.outputPath(), variants));
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
            entry.put("eligible_kind_counts", variant.eligibleKindCounts());
            entry.put("applied_count", variant.appliedCount());
            entry.put("applied_kind_counts", appliedKindCounts(variant.applied()));
            entry.put("effective_percentage", effectivePercentage(variant.appliedCount(), variant.eligibleCandidateCount()));

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
                | diagram_v2_effective_percentage | %s |
                | diagram_v3_effective_percentage | %s |
                | diagram_v2_eligible_kind_counts | %s |
                | diagram_v3_eligible_kind_counts | %s |
                | diagram_v2_applied_kind_counts | %s |
                | diagram_v3_applied_kind_counts | %s |
                | generated_at | %s |
                """.formatted(
                report.get("status"),
                artifacts.get("diagram_path"),
                nullToEmpty(artifacts.get("diagram_v2_path")),
                nullToEmpty(artifacts.get("diagram_v3_path")),
                report.get("type_count"),
                report.get("relation_count"),
                appliedCountForVariant(degradation, "diagram_v2"),
                appliedCountForVariant(degradation, "diagram_v3"),
                effectivePercentageForVariant(degradation, "diagram_v2"),
                effectivePercentageForVariant(degradation, "diagram_v3"),
                eligibleKindCountsForVariant(degradation, "diagram_v2"),
                eligibleKindCountsForVariant(degradation, "diagram_v3"),
                appliedKindCountsForVariant(degradation, "diagram_v2"),
                appliedKindCountsForVariant(degradation, "diagram_v3"),
                report.get("generated_at"));
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
                Object count = variantMap.get("applied_count");
                return count != null ? count.toString() : "";
            }
        }
        return "";
    }

    private String appliedKindCountsForVariant(Map<String, Object> degradation, String variantId) {
        return valueForVariant(degradation, variantId, "applied_kind_counts");
    }

    private String eligibleKindCountsForVariant(Map<String, Object> degradation, String variantId) {
        return valueForVariant(degradation, variantId, "eligible_kind_counts");
    }

    private String effectivePercentageForVariant(Map<String, Object> degradation, String variantId) {
        return valueForVariant(degradation, variantId, "effective_percentage");
    }

    private String valueForVariant(Map<String, Object> degradation, String variantId, String fieldName) {
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
                Object value = variantMap.get(fieldName);
                return value != null ? value.toString() : "";
            }
        }
        return "";
    }

    private Map<String, Integer> appliedKindCounts(List<ModelDegrader.AppliedDegradation> applied) {
        Map<String, Integer> counts = new LinkedHashMap<>();
        for (ModelDegrader.AppliedDegradation degradation : applied) {
            counts.merge(degradation.kind(), 1, Integer::sum);
        }
        return counts;
    }

    private double effectivePercentage(int appliedCount, int eligibleCandidateCount) {
        if (eligibleCandidateCount <= 0) {
            return 0.0d;
        }
        return (appliedCount * 100.0d) / eligibleCandidateCount;
    }

    private Path siblingPath(Path outputPath, String fileName) {
        Path parent = outputPath.getParent();
        return parent == null ? Path.of(fileName) : parent.resolve(fileName);
    }

    private String nullToEmpty(Object value) {
        return value == null ? "" : value.toString();
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
