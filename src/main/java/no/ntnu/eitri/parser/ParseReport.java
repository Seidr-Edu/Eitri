package no.ntnu.eitri.parser;

import no.ntnu.eitri.parser.resolution.TypeResolutionStats;

import java.util.List;

/**
 * Consolidated parser diagnostics and resolution metrics for a parse run.
 */
public record ParseReport(
        List<String> warnings,
        TypeResolutionStats typeResolutionStats
) {
    public ParseReport {
        warnings = warnings != null ? List.copyOf(warnings) : List.of();
    }

    public int warningCount() {
        return warnings.size();
    }
}
